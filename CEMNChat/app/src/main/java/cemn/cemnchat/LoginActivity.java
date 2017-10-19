package cemn.cemnchat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import static android.Manifest.permission.READ_CONTACTS;
import static cemn.cemnchat.Crypto.HashMD5;

/**
 * Created by Milad Hajihassan on 9/16/2017.
 */

//login screen
public class LoginActivity extends AppCompatActivity {

    private static final String TAG="LoginActivity";
    private static final int REQUEST_READ_CONTACTS = 0;

    // UI
    private EditText mCidView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private ImageButton mCloseConnectionButton;
    private ImageButton mSettingsButton;
    private ImageView mConnectionStatus;
    private Button mRegisterPageButton;
    private SharedPreferences prefs=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Set up ActionBar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_chat);
        View v =getSupportActionBar().getCustomView();
        mCloseConnectionButton = (ImageButton) v.findViewById(R.id.actionbar_header_close);
        mCloseConnectionButton.setVisibility(View.INVISIBLE);

        mSettingsButton = (ImageButton) v.findViewById(R.id.actionbar_header_settings);
        mSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent iSettings = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivity(iSettings);

            }
        });

        mConnectionStatus = (ImageView) v.findViewById(R.id.actionbar_connection_status);

        //Set up Login info
        mCidView = (EditText) findViewById(R.id.user_username);
        mPasswordView = (EditText) findViewById(R.id.user_password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mCidSignInButton = (Button) findViewById(R.id.sign_in_button);
        mCidSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mRegisterPageButton = (Button) findViewById(R.id.register_page);
        mRegisterPageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                showProgress(false);
                Intent iRegister = new Intent(getApplicationContext(),RegisterActivity.class);
                startActivity(iRegister);

            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mContext = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action)
                {
                    case CemnConnectionService.UI_AUTHENTICATED:
                        //Go to ContactActivity if authentication is done
                        showProgress(false);
                        Intent iContact = new Intent(mContext,ContactActivity.class);
                        startActivity(iContact);
                        finish();
                        break;
                }

            }
        };
        IntentFilter filter = new IntentFilter(CemnConnectionService.UI_AUTHENTICATED);
        this.registerReceiver(mBroadcastReceiver, filter);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mCidView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    //Function to login the user
    private void attemptLogin() {

        //Reset errors
        mCidView.setError(null);
        mPasswordView.setError(null);

        // Store credentials for login
        String username = mCidView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        //Check for a valid password
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        //Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mCidView.setError(getString(R.string.error_field_required));
            focusView = mCidView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mCidView.setError(getString(R.string.error_invalid_username));
            focusView = mCidView;
            cancel = true;
        }

        //If there was an error
        if (cancel) {
            focusView.requestFocus();
        } else {
            saveCredentialsAndLogin();
        }
    }

    //Function to save login credentials
    private void saveCredentialsAndLogin()
    {
        //Log.d(TAG,"saveCredentialsAndLogin() called.");
        prefs = getSharedPreferences("cemn_prefs", 0);
        prefs.edit()
                .putString("cemn_requester_user_name", mCidView.getText().toString())
                .putString("cemn_requester_pass_word", HashMD5(mPasswordView.getText().toString()))
                .putString("cemn_service_name", "localhost")
                //Change the ip address to the ip address of xmpp node
                .putString("cemn_host", "192.168.4.1")
                .putBoolean("cemn_signed_in",true)
                .commit();

        //Start the CemnConnection
        Intent iCemnConnection = new Intent(this,CemnConnectionService.class);
        iCemnConnection.putExtra("EXTRA_REQUESTED_CONNECTION",TAG);
        startService(iCemnConnection);

    }

    //Check if Username is valid
    private boolean isUsernameValid(String username) {
        return username.length() > 4;
    }

    //Check if Password is valid
    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    //Show progress UI
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

