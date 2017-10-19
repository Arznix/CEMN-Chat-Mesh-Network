package cemn.cemnchat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

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

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static cemn.cemnchat.Crypto.HashMD5;

/**
 * Created by Milad Hajihassan on 9/16/2017.
 */

public class ContactActivity extends AppCompatActivity {

    private Context mContext;
    private Button mButtonConnect;
    private EditText mEditTextRequestedUserName;
    private String mRequestedUserName;
    private String mRequesterUserName;
    private String mRequesterPassWord;
    private String mUnEncodedRetrieveKeyString;
    private String mEncodedRetrieveKeyString;
    private String mUnEncodedResponseString;
    private ImageButton mCloseConnectionButton;
    private ImageButton mSettingsButton;

    private SharedPreferences prefs=null;

    //Change the ip address to the ip address of registration server node
    private static final String REGISTER_URL_STRING = "http://192.168.4.1:8081/api/users";
    //Change the Symmetric key
    private static final String REGISTER_SYMMETRIC_KEY_STRING = "cEmg4RRSPGae58JyWZJWE/zYe8nUHMhKTId6rSU7TZA=";

    Crypto crypto = new Crypto();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_chat);

        mContext = this;

        mButtonConnect = (Button) findViewById(R.id.connect);
        mButtonConnect.setOnClickListener(buttonConnectOnClickListener);

        View v =getSupportActionBar().getCustomView();
        mCloseConnectionButton = (ImageButton) v.findViewById(R.id.actionbar_header_close);
        mCloseConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CemnConnectionService.getState().equals(CemnConnection.ConnectionState.CONNECTED)) {
                    CemnConnectionService.stop();
                    Intent i = new Intent(mContext,LoginActivity.class);
                    startActivity(i);
                }
            }
        });

        mSettingsButton = (ImageButton) v.findViewById(R.id.actionbar_header_settings);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent iSettings = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivity(iSettings);

            }
        });

        mRequestedUserName=null;
        mEditTextRequestedUserName = (EditText) findViewById(R.id.recipient_username);
        prefs = getSharedPreferences("cemn_prefs", 0);
        mRequesterUserName = prefs.getString("cemn_requester_user_name",null);
        mRequesterPassWord = prefs.getString("cemn_requester_pass_word",null);
    }

    //Registration server post call to get user public key and see if user exists
    public class onPostCall extends AsyncTask<String, String, Void> {

        public onPostCall(){
        }
        String encodedResponseString = "";
        private ProgressDialog loadingDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = new ProgressDialog(ContactActivity.this);
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
            final AlertDialog.Builder dialog = new AlertDialog.Builder(ContactActivity.this);
            dialog.setCancelable(false);
            if(Integer.valueOf(errorCode)==0){
                dialog.setTitle("User "+ mRequestedUserName + " was successfully found.");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        String message = getElementValueFromXML("message", mUnEncodedResponseString);
                        String publicKey = message.substring(message.lastIndexOf(":") + 1);
                        //Log.d("publicKey: ",publicKey);
                        Intent intent = new Intent(ContactActivity.this
                                ,ChatActivity.class);
                        intent.putExtra("EXTRA_REQUESTED_USERNAME",mRequestedUserName);
                        intent.putExtra("EXTRA_REQUESTED_PUBLIC_KEY",publicKey);
                        startActivity(intent);

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

    //Function to create an uncrypted XML string to retrieve public Key
    private String retrieveKeyStringCreator(String requesterUsername,String requesterPassword,String requestedUsername) {
        String registerString = "<data>"
                +"<cmd>retrieve_public_key</cmd>"
                +"<requester_user_name>" + requesterUsername + "</requester_user_name>"
                +"<pass_word>" + requesterPassword + "</pass_word>"
                +"<requested_user_name>" + requestedUsername + "</requested_user_name>"
                +"</data>";
        return registerString;
    }

    //Action to start chat with selected username on buttonConnectOnClickListener
    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mRequestedUserName  = mEditTextRequestedUserName.getText().toString();
            if (mRequestedUserName.equals("")) {
                Toast.makeText(ContactActivity.this, "Enter recipient Username",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (CemnConnectionService.getState().equals(CemnConnection.ConnectionState.CONNECTED)) {
                mUnEncodedRetrieveKeyString = retrieveKeyStringCreator(mRequesterUserName,
                        mRequesterPassWord
                        ,mRequestedUserName);
                mEncodedRetrieveKeyString =crypto.SymmetricEncrypt(mUnEncodedRetrieveKeyString, REGISTER_SYMMETRIC_KEY_STRING);
                onPostCall postTask = new onPostCall();
                postTask.execute(REGISTER_URL_STRING,mEncodedRetrieveKeyString);
            }

        }

    };
}
