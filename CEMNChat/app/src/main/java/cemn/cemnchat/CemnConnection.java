package cemn.cemnchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class CemnConnection implements ConnectionListener {

    private static final String TAG = "CemnConnection";

    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private  final String mServiceName;
    public static String mActivityConnection;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;
    private ChatMessageListener messageListener;


    public static enum ConnectionState
    {
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public static enum LoggedInState
    {
        LOGGED_IN , LOGGED_OUT;
    }


    public CemnConnection(SharedPreferences prefs ,String activityConnection, Context context)
    {
        //Log.d(TAG,"CemnConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        mServiceName=prefs.getString("cemn_service_name",null);
        mUsername = prefs.getString("cemn_requester_user_name",null);
        mPassword = prefs.getString("cemn_requester_pass_word",null);
        mActivityConnection = activityConnection;
    }


    public void connectLogin() throws IOException,XMPPException,SmackException
    {
        //Log.d(TAG, "Connecting to server: " + mServiceName);
        XMPPTCPConnectionConfiguration.XMPPTCPConnectionConfigurationBuilder builder=
                XMPPTCPConnectionConfiguration.builder();
        builder.setServiceName(mServiceName);
        builder.setUsernameAndPassword(mUsername, mPassword);
        builder.setHost("192.168.4.1");
        builder.setPort(5222);
        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        builder.setRosterLoadedAtLogin(true);
        builder.setResource("Cemn");

        //Set up the ui broadcasting message receiver thread
        setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(builder.build());
        mConnection.addConnectionListener(this);

        if(mActivityConnection.equals("RegisterActivity")) {
            mConnection.connect();
            SASLMechanism mechanism = new SASLDigestMD5Mechanism();
            SASLAuthentication.registerSASLMechanism(mechanism);
            SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
            SASLAuthentication.unBlacklistSASLMechanism("DIGEST-MD5");
            AccountManager accountManager = AccountManager.getInstance(mConnection);
            Map<String, String> attributes = new HashMap<>();
            attributes.put("username", mUsername);
            attributes.put("password", mPassword);
            try {
                if (accountManager.supportsAccountCreation()) {
                    accountManager.createAccount(mUsername, mPassword);
                    mConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (mActivityConnection.equals("LoginActivity")){
            mConnection.connect();
            mConnection.login();

            messageListener = new ChatMessageListener() {
                @Override
                public void processMessage(Chat chat, Message message) {
                    //Log.d(TAG, "message.getBody() :" + message.getBody());
                    //Log.d(TAG, "message.getFrom() :" + message.getFrom());

                    String from = message.getFrom();
                    String contactCid = "";
                    if (from.contains("/")) {
                        contactCid = from.split("/")[0];
                        //Log.d(TAG, "id is :" + contactCid);
                    } else {
                        contactCid = from;
                    }

                    //Bundle up intent and send it to the broadcaster
                    Intent iCemnConnectionService = new Intent(CemnConnectionService.NEW_MESSAGE);
                    iCemnConnectionService.setPackage(mApplicationContext.getPackageName());
                    iCemnConnectionService.putExtra(CemnConnectionService.BUNDLE_FROM_CID, contactCid);
                    iCemnConnectionService.putExtra(CemnConnectionService.BUNDLE_MESSAGE_BODY, message.getBody());
                    mApplicationContext.sendBroadcast(iCemnConnectionService);
                    //Log.d(TAG, "Received message from :" + contactCid);

                }
            };

            //Attached message listener to connection
            ChatManager.getInstanceFor(mConnection).addChatListener(new ChatManagerListener() {
                @Override
                public void chatCreated(Chat chat, boolean createdLocally) {
                    chat.addMessageListener(messageListener);

                }
            });

            ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
            reconnectionManager.setEnabledPerDefault(true);
            reconnectionManager.enableAutomaticReconnection();
        }

    }



    private void setupUiThreadBroadCastMessageReceiver()
    {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if( action.equals(CemnConnectionService.SEND_MESSAGE))
                {
                    //Send the message
                    sendMessage(intent.getStringExtra(CemnConnectionService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(CemnConnectionService.BUNDLE_TO));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(CemnConnectionService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);
    }

    private void sendMessage ( String body ,String toCid)
    {
        //Log.d(TAG,"Sending message to :"+ toCid);
        Chat chat = ChatManager.getInstanceFor(mConnection)
                .createChat(toCid,messageListener);
        try
        {
            chat.sendMessage(body);
        }catch (SmackException.NotConnectedException | XMPPException e)
        {
            e.printStackTrace();
        }


    }


    public void disconnect()
    {
        //Log.d(TAG,"Disconnecting from: "+ mServiceName);
        try
        {
            if (mConnection != null)
            {
                mConnection.disconnect();
            }

        }catch (SmackException.NotConnectedException e)
        {
            CemnConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
            e.printStackTrace();

        }
        mConnection = null;
        //Un-registering the message broadcast receiver
        if( uiThreadMessageReceiver != null)
        {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        CemnConnectionService.sConnectionState=ConnectionState.CONNECTED;
        //Log.d(TAG,"Connected Successfully");
    }

    @Override
    public void authenticated(XMPPConnection connection) {
        CemnConnectionService.sConnectionState=ConnectionState.CONNECTED;
        //Log.d(TAG,"Authenticated Successfully");
        showContactListActivityWhenAuthenticated();
    }




    @Override
    public void connectionClosed() {
        CemnConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        //Log.d(TAG,"Connectionclosed() function was called.");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        CemnConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        //Log.d(TAG,"Connection was closed on error: "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        CemnConnectionService.sConnectionState = ConnectionState.CONNECTING;
        //Log.d(TAG,"ReconnectingIn() function was called.");

    }

    @Override
    public void reconnectionSuccessful() {
        CemnConnectionService.sConnectionState = ConnectionState.CONNECTED;
        //Log.d(TAG,"ReconnectionSuccessful() function was called.");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        CemnConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        //Log.d(TAG,"ReconnectionFailed() function was called.");

    }

    private void showContactListActivityWhenAuthenticated()
    {
        Intent iCemnConnectionService = new Intent(CemnConnectionService.UI_AUTHENTICATED);
        iCemnConnectionService.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(iCemnConnectionService);
        //Log.d(TAG,"Sent the broadcast that we are authenticated");
    }
}
