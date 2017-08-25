package cemn.cemnchat;

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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

//**By Milad Hajihassan

public class ChatActivity extends AppCompatActivity {

    private static final int SERVER_PORT = 8080;
    private static final String SERVER_IP = "";

    Button buttonConnect;
    Button buttonSend;
    Button buttonDisconnect;

    ImageButton imageButtonSettings, imageButtonClose;

    ImageView imageViewStatus;

    EditText editTextSendMessage;
    TextView chatMessage;

    EditText editTextRecipientUserName, editTextSenderUserName;

    ChatClientThread chatClientThread = null;

    String messageLog = "";

    boolean connectionShouldEnd = false;

    LinearLayout connectPanel;
    LinearLayout chatPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        //Setup layout
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_chat);

        connectPanel = (LinearLayout)findViewById(R.id.panel_connect);

        connectPanel.setVisibility(View.VISIBLE);

        chatPanel = (LinearLayout)findViewById(R.id.panel_chat);

        chatPanel.setVisibility(View.INVISIBLE);

        buttonConnect = (Button) findViewById(R.id.connect);
        //buttonDisconnect = (Button) findViewById(R.id.disconnect);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        //buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);

        buttonSend = (Button)findViewById(R.id.send);

        buttonSend.setOnClickListener(buttonSendOnClickListener);

        imageButtonClose = (ImageButton) findViewById(R.id.actionbar_header_close);
        imageButtonClose.setOnClickListener(imageButtonCloseOnClickListener);

        imageButtonSettings = (ImageButton) findViewById(R.id.actionbar_header_settings);
        imageButtonSettings.setOnClickListener(imageButtonSettingsOnClickListener);

        imageViewStatus = (ImageView) findViewById(R.id.actionbar_header_status);


        chatMessage = (TextView) findViewById(R.id.chat_message);

        chatMessage.setVisibility(View.VISIBLE);

        editTextRecipientUserName = (EditText) findViewById(R.id.recipient_username);
        editTextSenderUserName = (EditText) findViewById(R.id.sender_username);

        editTextSendMessage = (EditText)findViewById(R.id.send_message);

    }

    View.OnClickListener imageButtonCloseOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(chatClientThread==null){
                return;
            }
            connectionShouldEnd = true;
            connectPanel.setVisibility(View.VISIBLE);
            chatPanel.setVisibility(View.GONE);
        }

    };

    View.OnClickListener imageButtonSettingsOnClickListener = new View.OnClickListener() {


        @Override
        public void onClick(View view) {

        }
    };

    /*
    View.OnClickListener buttonDisconnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(chatClientThread==null){
                return;
            }
            chatClientThread.disconnect();
            connectPanel.setVisibility(View.VISIBLE);
            chatPanel.setVisibility(View.GONE);
        }

    };

    */

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String recipientUserName = editTextRecipientUserName.getText().toString();
            if (recipientUserName.equals("")) {
                Toast.makeText(ChatActivity.this, "Enter recipient Username",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String senderUserName = editTextSenderUserName.getText().toString();
            if (senderUserName.equals("")) {
                Toast.makeText(ChatActivity.this, "Enter your Username",
                        Toast.LENGTH_LONG).show();
                return;
            }

            messageLog = "";
            chatMessage.setText("Milad: Hello!");
            //chatMessage.setText(messageLog);
            //loginPanel.setVisibility(View.GONE);
            //chatPanel.setVisibility(View.VISIBLE);

            chatClientThread = new ChatClientThread(
                    senderUserName, SERVER_IP, SERVER_PORT);
            chatClientThread.start();
            connectionShouldEnd = false;
            connectPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);
        }

    };

    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (editTextSendMessage.getText().toString().equals("")) {
                return;
            }

            if(chatClientThread==null){
                return;
            }

            chatClientThread.sendMessage(encryptMessage(editTextSendMessage.getText().toString()));
            editTextSendMessage.setText("");
        }

    };

    //Encryption function

    private String encryptMessage(String message){
        String encryptedMessage = message;
        return encryptedMessage;
    }

    private class ChatClientThread extends Thread {

        String username;
        String dstAddress;
        int dstPort;

        String messageToSend = "";
        boolean goOut = false;

        ChatClientThread(String username, String address, int port) {
            this.username = username;
            dstAddress = address;
            dstPort = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                try {
                    socket = new Socket(SERVER_IP, dstPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                BufferedReader stdIn =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                dataOutputStream.write(username.getBytes());
                dataOutputStream.flush();

                if (connectionShouldEnd)
                {
                    String endConnectionString ="[end#]";
                    dataOutputStream.write(endConnectionString.getBytes());
                    dataOutputStream.flush();
                    connectionShouldEnd=false;
                    chatClientThread.disconnect();
                }


                while (!goOut) {
                    String in = stdIn.readLine();
                    if (in.length() > 0) {
                   // if (dataInputStream.available() > 0) {


                        //Log.d("test:", String.valueOf(in));
                        //messageLog += dataInputStream.read();
                        messageLog = in;
                        ChatActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMessage.setText(messageLog);
                            }
                        });
                    }

                    if(!messageToSend.equals("")){
                        dataOutputStream.write(messageToSend.getBytes());
                        dataOutputStream.flush();
                        messageToSend = "";
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                ChatActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ChatActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                ChatActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ChatActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                ChatActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        connectPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendMessage(String msg){
            messageToSend = msg;
        }

        private void disconnect(){
            goOut = true;
        }
    }
}
