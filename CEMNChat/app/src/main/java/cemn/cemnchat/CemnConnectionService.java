package cemn.cemnchat;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;



public class CemnConnectionService  extends Service {
    private static final String TAG ="CemnService";

    public static final String UI_AUTHENTICATED = "cemn.cemnchat.uiauthenticated";
    public static final String SEND_MESSAGE = "cemn.cemnchat.sendmessage";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_TO = "b_to";

    public static final String NEW_MESSAGE = "cemn.cemnchat.newmessage";
    public static final String BUNDLE_FROM_CID = "b_from";

    public static CemnConnection.ConnectionState sConnectionState;
    public static CemnConnection.LoggedInState sLoggedInState;
    private static boolean mActive;
    private static Thread mThread;
    private static Handler mTHandler;
    private static CemnConnection mConnection;
    public static String mActivityConnection;

    public CemnConnectionService() {

    }
    public static CemnConnection.ConnectionState getState()
    {
        if (sConnectionState == null)
        {
            return CemnConnection.ConnectionState.DISCONNECTED;
        }
        return sConnectionState;
    }

    public static CemnConnection.LoggedInState getLoggedInState()
    {
        if (sLoggedInState == null)
        {
            return CemnConnection.LoggedInState.LOGGED_OUT;
        }
        return sLoggedInState;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d(TAG,"onCreate() function was called.");
    }

    private void initConnection()
    {
        //Log.d(TAG,"initConnection() function was called.");
        if( mConnection == null)
        {

            SharedPreferences prefs = getSharedPreferences("cemn_prefs", 0);
            mConnection = new CemnConnection(prefs,mActivityConnection,this);
        }
        try
        {
            mConnection.connectLogin();

        }catch (IOException e)
        {
            //Log.d(TAG,"Something has gone wrong while connecting...try again");
            e.printStackTrace();
            //Stop the service all together.
            stopSelf();
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (XMPPException e) {
            e.printStackTrace();
        }

    }


    public void start()
    {
        //Log.d(TAG,"Start() function was called.");
        if(!mActive)
        {
            mActive = true;
            if( mThread ==null || !mThread.isAlive())
            {
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        Looper.loop();

                    }
                });
                mThread.start();
            }


        }

    }

    public static void stop()
    {
        //Log.d(TAG,"stop() function was called.");
        mActive = false;
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                if( mConnection != null)
                {
                    mConnection.disconnect();
                    mThread=null;
                }
            }
        });

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG,"onStartCommand() function was called.");
        mActivityConnection = intent.getStringExtra("EXTRA_REQUESTED_CONNECTION");
        start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Log.d(TAG,"onDestroy() function was called.");
        super.onDestroy();
        stop();
    }

}
