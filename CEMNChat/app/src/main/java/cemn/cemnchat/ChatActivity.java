package cemn.cemnchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import co.devcenter.androiduilibrary.ChatView;
import co.devcenter.androiduilibrary.ChatViewEventListener;
import co.devcenter.androiduilibrary.SendButton;

import static cemn.cemnchat.Crypto.AsymmetricDecodePrivate;
import static cemn.cemnchat.Crypto.AsymmetricEncrypt;
import static cemn.cemnchat.Crypto.CreateSignature;
import static cemn.cemnchat.Crypto.VerifySignature;
import static cemn.cemnchat.Crypto.bytesToString;
import static cemn.cemnchat.Crypto.stringToBytes;


/**
 * Created by Milad Hajihassan on 9/16/2017.
 */

public class ChatActivity extends AppCompatActivity {

    private static final String TAG ="ChatActivity";

    private String mRequestedId;
    private String mRequestedUsername;
    private String mServiceName;
    private String mRequestedPublicKey;
    private String mRequesterPrivateKey;
    private ChatView mChatView;
    private SendButton mSendButton;
    private BroadcastReceiver mBroadcastReceiver;
    private ImageButton mCloseConnectionButton;
    private ImageButton mSettingsButton;
    private Context mContext;
    private SharedPreferences prefs=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Setup ActionBar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_chat);

        mContext = this;

        Intent intent = getIntent();
        prefs = getSharedPreferences("cemn_prefs", 0);
        mRequestedUsername = intent.getStringExtra("EXTRA_REQUESTED_USERNAME");
        mServiceName = prefs.getString("cemn_service_name",null);
        mRequestedId = mRequestedUsername + "@" + mServiceName;
        mRequestedPublicKey = intent.getStringExtra("EXTRA_REQUESTED_PUBLIC_KEY");
        mRequesterPrivateKey = prefs.getString("cemn_requester_private_key",null);

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

        mChatView =(ChatView) findViewById(R.id.cemn_chat_view);
        mChatView.setEventListener(new ChatViewEventListener() {
            @Override
            public void userIsTyping() {
            }

            @Override
            public void userHasStoppedTyping() {
            }
        });

        mSendButton = mChatView.getSendButton();
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Send the message if the client is connected to server
                if (CemnConnectionService.getState().equals(CemnConnection.ConnectionState.CONNECTED)) {
                   // Log.d(TAG, "Client is connected to the server.");
                    //Send the message to the server
                    Intent iChat = new Intent(CemnConnectionService.SEND_MESSAGE);
                    iChat.putExtra(CemnConnectionService.BUNDLE_MESSAGE_BODY, messageEncrypter(mChatView.getTypedString(),mRequestedPublicKey,mRequesterPrivateKey));
                    iChat.putExtra(CemnConnectionService.BUNDLE_TO, mRequestedId);
                    sendBroadcast(iChat);
                    //Update chat view
                    mChatView.sendMessage();
                } else {
                    Toast.makeText(getApplicationContext(), "User is not connected to server.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    //Function to Decrypt a encrypted message using PublicKey and PrivateKey
    private String messageDecrypter(String sDecrypted,String sPublicKey,String sPrivateKey) {
        PublicKey myPublicKey = null;
        PrivateKey myPrivateKey = null;
        byte[]  myDecodedTextBuf = null;
        String myDecodedTextString = null;
        byte[] myEncryptedSignatureString = null;
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            myPublicKey = kf.generatePublic(new X509EncodedKeySpec(stringToBytes(sPublicKey)));
            myPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(stringToBytes(sPrivateKey)));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "sDecrypted: "+sDecrypted);
        String myTextString = getElementValueFromXML("text", sDecrypted);
        String mySignatureString = getElementValueFromXML("signature", sDecrypted);
        // Decode the Asymmmetric Encrpyted text
        myDecodedTextBuf = AsymmetricDecodePrivate(myPrivateKey, stringToBytes(myTextString));
        //convert deoded byte buffer to a string
        myDecodedTextString = new String(myDecodedTextBuf);
        //Digital signature in bytes
        myEncryptedSignatureString  = stringToBytes(mySignatureString);

        int status = VerifySignature(myPublicKey, myDecodedTextString,  myEncryptedSignatureString);
        if (status == 0)
        {
            //Log.d(TAG, "Signature does not match");
            return null;
        }
        else {
            //Log.d(TAG, "Signature matches. Sender is verified.");
            return myDecodedTextString;
        }

    }

    //Function to Encrypt a message using PublicKey and PrivateKey
    private String messageEncrypter(String sText,String sPublicKey,String sPrivateKey) {
        PublicKey myPublicKey = null;
        PrivateKey myPrivateKey = null;
        byte[]  myEncodedTextBuf = null;
        byte[]  myEncodedSignatureBuf = null;
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            myPublicKey = kf.generatePublic(new X509EncodedKeySpec(stringToBytes(sPublicKey)));
            myPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(stringToBytes(sPrivateKey)));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        myEncodedTextBuf = AsymmetricEncrypt(myPublicKey, sText);
        myEncodedSignatureBuf = CreateSignature(myPrivateKey,sText);
        String messageString = "<subject><text>" + bytesToString(myEncodedTextBuf) + "</text>"
                +"<signature>" + bytesToString(myEncodedSignatureBuf) + "</signature></subject>";
        return messageString;
    }

    //Function that get value of an element in XML string
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

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver(mBroadcastReceiver);
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
                    case CemnConnectionService.NEW_MESSAGE:
                        String from = intent.getStringExtra(CemnConnectionService.BUNDLE_FROM_CID);
                        String body = intent.getStringExtra(CemnConnectionService.BUNDLE_MESSAGE_BODY);

                        if ( from.equals(mRequestedId))
                        {
                            //Decrypt the encrypted message and Display the message
                            mChatView.receiveMessage(mRequestedUsername+":"+messageDecrypter(body,mRequestedPublicKey,mRequesterPrivateKey));

                        }else
                        {
                            //Log.d(TAG,"Got a message from id :"+mRequestedId);
                        }

                        return;
                }

            }
        };

        IntentFilter filter = new IntentFilter(CemnConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver,filter);


    }
}
