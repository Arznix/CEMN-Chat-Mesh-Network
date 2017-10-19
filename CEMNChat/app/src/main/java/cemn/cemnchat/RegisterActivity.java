package cemn.cemnchat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static cemn.cemnchat.Crypto.HashMD5;
import static cemn.cemnchat.Crypto.bytesToString;
import static cemn.cemnchat.Crypto.generateAsymmetricKeys;

/**
 * Created by Milad Hajihassan on 9/16/2017.
 */


public class RegisterActivity extends AppCompatActivity {

    private static final String TAG="RegisterActivity";

    //Change the ip address to the ip address of registration server node
    private static final String REGISTER_URL_STRING = "http://192.168.4.1:8081/api/users";
    //Change the Symmetric key
    private static final String REGISTER_SYMMETRIC_KEY_STRING = "cEmg4RRSPGae58JyWZJWE/zYe8nUHMhKTId6rSU7TZA=";

    private SharedPreferences prefs=null;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mPasswordConfirmationView;
    private View mProgressView;
    private View mRegisterFormView;
    private Button mRegisterButton;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private ImageButton mCloseConnectionButton;
    private ImageButton mSettingsButton;
    private ImageView mConnectionStatus;

    private String mUnEncodedRegisterString;
    private String mEncodedRegisterString;
    private String mUnEncodedResponseString;

    static KeyPair mAsymmetricKeyPair;    //Asymmetric Key Storage for Public and Private Keys RSA 1024 or RSA 2018

    Crypto crypto = new Crypto();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_chat);

        View v =getSupportActionBar().getCustomView();
        mCloseConnectionButton = (ImageButton) v.findViewById(R.id.actionbar_header_close);
        mCloseConnectionButton.setVisibility(View.INVISIBLE);

        mSettingsButton = (ImageButton) v.findViewById(R.id.actionbar_header_settings);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent iSettings = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivity(iSettings);

            }
        });

        mConnectionStatus = (ImageView) v.findViewById(R.id.actionbar_connection_status);

        mUsernameView = (EditText) findViewById(R.id.register_username);
        mPasswordView = (EditText) findViewById(R.id.register_password);
        mPasswordConfirmationView = (EditText) findViewById(R.id.register_password_confirmation);

        mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);
        mContext = this;

    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Intent iLogin = new Intent(mContext,LoginActivity.class);
                startActivity(iLogin);
            }
        };
        IntentFilter filter = new IntentFilter(CemnConnectionService.UI_AUTHENTICATED);
        this.registerReceiver(mBroadcastReceiver, filter);
    }

    //Function to register the new user
    private void attemptRegister() {
        //Log.d(TAG,"attemptRegister() called.");
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mPasswordConfirmationView.setError(null);

        // Store credentials for registration
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordConfirmation= mPasswordConfirmationView.getText().toString();
        boolean cancel = false;
        View focusView = null;

        //Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        //Check for a valid password if the user entered password
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        //Check for a valid confirmation password if the user entered confirmation password
        if (TextUtils.isEmpty(passwordConfirmation) || !isPasswordConfirmationValid(password,passwordConfirmation)) {
            mPasswordConfirmationView.setError(getString(R.string.error_invalid_password_confirmation));
            focusView = mPasswordConfirmationView;
            cancel = true;
        }
        //If there was an error
        if (cancel) {
            focusView.requestFocus();
        } else {
            saveCredentialsAndRegister();

        }
    }

    //Function to save registration credentials
    private void saveCredentialsAndRegister()
    {
        //Log.d(TAG,"saveCredentialsAndRegister() called.");

        //Generate an Asymmetic Key pair
        mAsymmetricKeyPair = generateAsymmetricKeys();

        //Put the keys into temporary value for easy viewing in debugger
        PrivateKey myAsymmetricPrivateKey;
        myAsymmetricPrivateKey = mAsymmetricKeyPair.getPrivate();
        String myAsymmetricPrivateString = bytesToString(myAsymmetricPrivateKey.getEncoded());

        PublicKey myAsymmetricPublicKey;
        myAsymmetricPublicKey = mAsymmetricKeyPair.getPublic();
        String myAsymmetricPublicString = bytesToString(myAsymmetricPublicKey.getEncoded());

        mUnEncodedRegisterString = registerStringCreator(mUsernameView.getText().toString(),
                mPasswordView.getText().toString()
                ,myAsymmetricPublicString);

        prefs = getSharedPreferences("cemn_prefs", 0);
        prefs.edit()
                .putString("cemn_requester_user_name", mUsernameView.getText().toString())
                .putString("cemn_requester_pass_word", HashMD5(mPasswordView.getText().toString()))
                .putString("cemn_requester_private_key", myAsymmetricPrivateString)
                .putString("cemn_service_name", "localhost")
                .apply();

        mEncodedRegisterString =crypto.SymmetricEncrypt(mUnEncodedRegisterString, REGISTER_SYMMETRIC_KEY_STRING);
        registerServerCreateUser registerServerTask = new registerServerCreateUser();
        registerServerTask.execute(REGISTER_URL_STRING,mEncodedRegisterString);

    }


    //Registration server post call to register user register
    public class registerServerCreateUser extends AsyncTask<String, String, Void> {

        public registerServerCreateUser(){
        }
        String encodedResponseString = "";
        private ProgressDialog loadingDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = new ProgressDialog(RegisterActivity.this);
            loadingDialog.setMessage("Loading...");
            loadingDialog.show();
        }


        @Override
        protected Void doInBackground(String... params) {

            String urlString = params[0];

            String data =params[1];

            OutputStream out = null;
            try {

                URL url = new URL(urlString);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Content-Type","text/plain");
                out = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out, "UTF-8"));

                writer.write(data);

                writer.flush();

                writer.close();

                out.close();

                int responseCode=urlConnection.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    while ((line=br.readLine()) != null) {
                        encodedResponseString+=line;
                    }
                }
                else {
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (loadingDialog != null)
            {
                loadingDialog.dismiss();
            }
            mUnEncodedResponseString=crypto.SymmetricDecrypt(encodedResponseString, REGISTER_SYMMETRIC_KEY_STRING);
            String errorCode = getElementValueFromXML("error", mUnEncodedResponseString);
            final AlertDialog.Builder dialog = new AlertDialog.Builder(RegisterActivity.this);
            dialog.setCancelable(false);
            if(Integer.valueOf(errorCode)==0){

                dialog.setTitle("User "+ mUsernameView.getText().toString() + " was successfully created.");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        xmppCreateUser xmppTask = new xmppCreateUser();
                        xmppTask.execute();

                    }
                });
            }
            else
            {
                dialog.setTitle("Error: Try again");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
            }
            final AlertDialog alert = dialog.create();
            alert.show();
        }
    }

    //XMPP call to register user register
    public class xmppCreateUser extends AsyncTask<Void, Void, Void> {

        public xmppCreateUser() {
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Intent iCemnConnection = new Intent(mContext,CemnConnectionService.class);
            iCemnConnection.putExtra("EXTRA_REQUESTED_CONNECTION",TAG);
            startService(iCemnConnection);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Intent iLogin = new Intent(getApplicationContext(),LoginActivity.class);
            startActivity(iLogin);
        }
    }

    //Function to get an element value from XML string
    protected String getElementValueFromXML(String tagName, String xmlString) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document document = null;
        try {
            document = builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element element = document.getDocumentElement();
        NodeList list = element.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            NodeList subList = list.item(0).getChildNodes();

            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }

        return null;
    }

    private String registerStringCreator(String sUsername,String sPassword,String sPublicKey) {
        String registerString = "<data>"
                +"<cmd>new</cmd>"
                +"<user_name>" + sUsername + "</user_name>"
                +"<pass_word>" + HashMD5(sPassword) + "</pass_word>"
                +"<public_key>" + sPublicKey + "</public_key>"
                +"</data>";
        return registerString;
    }

    //Check if Username is valid
    private boolean isUsernameValid(String username) {
        if(username.length() > 4)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    //Check if Password is valid
    private boolean isPasswordValid(String password) {
        if(password.length() > 4)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    //Check if Password confirmation is valid
    private boolean isPasswordConfirmationValid(String password,String passwordConfirmation) {
        if(passwordConfirmation.equals(password))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
