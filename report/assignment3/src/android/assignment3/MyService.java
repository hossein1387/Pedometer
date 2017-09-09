package android.assignment3;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service implements SensorEventListener{
    //private Timer timer = new Timer();
    private int  incrementby = 1;
    private float X_CO=0,Y_CO=0,Z_CO=0,counter = 0;
    private float[] Data = {X_CO,Y_CO,Z_CO,counter};
    private static boolean isRunning = false;
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    int mValue = 0; // Holds last value set by a client.
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    static final int SETTING_ACCEL_SENSOR = 6;    
    SensorManager mSensorManager;
    Sensor mAccelerometer;
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.


    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_SET_INT_VALUE:
                incrementby = msg.arg1;
                break;
            case SETTING_ACCEL_SENSOR:
            	useAccelerometer(msg.arg1,msg.arg2);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    private void sendMessageToUI(float[] DataToSend) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send data as an Integer
                mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, 0, 0, Data));

            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

	@Override
    public void onCreate() {
        super.onCreate();
        
    }
	//=======================================================================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // run until explicitly stopped.
    }

    public static boolean isRunning()
    {
        return isRunning;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        counter=0;
        mSensorManager.unregisterListener(this);
        isRunning = false;
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		X_CO = event.values[0];
		Y_CO = event.values[1];
		Z_CO = event.values[2];
		Data[0] = X_CO;
		Data[1] = Y_CO;
		Data[2] = Z_CO;
        sendMessageToUI(Data);
	}
	
	
    public void useAccelerometer(int Enable,int delayRate) {
        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        isRunning = true;
            if(Enable == 1) {
                if (mSensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).size() != 0)
                {
                    mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            		switch (delayRate) {
            		case SensorManager.SENSOR_DELAY_NORMAL:
            			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            			break;
            		case SensorManager.SENSOR_DELAY_UI:
            			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            			break;
            		case SensorManager.SENSOR_DELAY_GAME:
            			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            			break;
            		case SensorManager.SENSOR_DELAY_FASTEST:
            			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            			break;
            		default:
            			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            			break;
            		}
                	Toast.makeText(getBaseContext(), "Accelerometer Sensor detected",Toast.LENGTH_LONG).show();
                }
                else{
                	Toast.makeText(getBaseContext(), "No Accelerometer Sensor detected!",Toast.LENGTH_LONG).show();
                }

                }
            else {
            	mSensorManager.unregisterListener(this, mAccelerometer);
            	mSensorManager.unregisterListener(this);
            }
    }

    
}