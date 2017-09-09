package android.assignment3;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils.Null;
import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxandroidlibv2.activities.OAuthActivity;
import com.box.boxandroidlibv2.dao.BoxAndroidFolder;
import com.box.boxandroidlibv2.dao.BoxAndroidOAuthData;
import com.box.boxjavalibv2.dao.BoxFileVersion;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileUploadRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.LineGraphView;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
 public class MainActivity extends Activity {
//=======================================================================================================================================
// Variable Deceleration:
//=======================================================================================================================================
    private int NumberOfSamples  = 1000;//Rate of saving the sampled data to the file in milliseconds 
    private final static int AUTH_REQUEST = 1;  
    private Handler handler = new Handler();
    private boolean authentication = false,NetworkAvalability=false,DoneUploading=true,mIsBound,SyncData=true;;
     private float X_CO=0,Y_CO=0,Z_CO=0;
     private int samplecounter = 0,counter=0,stepCounter=0,StepSampleRate=50,StepSampleCounter=0;
     private float SampleFloatX=0,SampleFloatY=0,SampleFloatZ=0,PeakFloatX=0,PeakFloatY=0,PeakFloatZ=0;
     private float AvgFloatX=0,AvgFloatY=0,AvgFloatZ=0;
     private float[] DataRec = {X_CO,Y_CO,Z_CO};
     TextView step, goal_text,battery_volt,battery_temp,est_time,charge_level,position;
     Messenger mService = null;
     final Messenger mMessenger = new Messenger(new IncomingHandler());
     private static float graph2LastXValue=0;
     private static double StepAccelTHR=1.2,PeakAccelThr=2.2;
     private static float thr;
     private static int Enable =1, Disable=0;
     private static int NORMAL=3,MEDIUM=2,FAST=1,SUPERFAST=0,StepInterval=1;
     private static int NORMALRATE=2,MEDIUMRATE=5,FASTRATE=15,SUPERFASTRATE=35;     
     private int FilterCounter=0;
     private float[] FilterXData = {0,0,0,0};
     private float[] FilterYData = {0,0,0,0};
     private float[] FilterZData = {0,0,0,0};     
     private float[] FilteredData = {0,0,0};
     private float DataToPlot1,DataToPlot2,DataToPlot3;
     private int plot =1;
     double voltage;
     GraphViewSeries XdataSereis,YdataSereis,ZdataSereis,Threshold;
     GraphView graphView;
     SharedPreferences getPrefs;
     String ProjectName = "AccelData", extension = ".txt",BoxFile="DataFile";
     File DataFile,FileToBeSent,LastUsedDataFile,LastUsedSentFile;
//=======================================================================================================================================
// Service CallBacks:
//=======================================================================================================================================
class IncomingHandler extends Handler {
	@Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case MyService.MSG_SET_INT_VALUE:{
        	DataRec = (float[]) msg.obj;
        	FilterData(DataRec);
        	CheckIfStep(FilteredData,DataRec);
            graph2LastXValue += 1d;
            if (plot==1){
                DataToPlot1 = FilteredData[0];
                DataToPlot2 = DataRec[0];
                DataToPlot3 = PeakFloatX;
            }else if(plot==2){
                DataToPlot1 = FilteredData[1];
                DataToPlot2 = DataRec[1];
                DataToPlot3 = PeakFloatY;
            }else if(plot == 3){
                DataToPlot1 = FilteredData[2];
                DataToPlot2 = DataRec[2];
                DataToPlot3 = PeakFloatZ;
            }else{
                DataToPlot1 = FilteredData[0];
                DataToPlot2 = DataRec[0];
                DataToPlot3 = PeakFloatX;
            }
            XdataSereis.appendData(new GraphViewData(graph2LastXValue, DataToPlot1), true, 10);
            YdataSereis.appendData(new GraphViewData(graph2LastXValue, DataToPlot2), true, 10);
            ZdataSereis.appendData(new GraphViewData(graph2LastXValue, DataToPlot3), true, 10);
            DataRec[3]=thr;
            UpdateFile(DataRec);
            counter++;
            break;        	
        }
        default:
            super.handleMessage(msg);
        }
    }
}
//=======================================================================================================================================
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
//=======================================================================================================================================
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
//=======================================================================================================================================
    private void restoreMe(Bundle state) {
        if (state!=null) {
        }
    }
//=======================================================================================================================================
    private void CheckIfServiceIsRunning() {
        if (MyService.isRunning()) {
            doBindService();
        }
    }     
//=======================================================================================================================================
    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_SET_INT_VALUE, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }
//=======================================================================================================================================
    void doBindService() {
        bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
//=======================================================================================================================================
    void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }
//=======================================================================================================================================
// Android CallBacks
//=======================================================================================================================================
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        restoreMe(savedInstanceState);
        CheckIfServiceIsRunning();
        counter=0;
        samplecounter =0;
        doBindService();
        if (!isNetworkAvailable()){
            Toast.makeText(this, "No Internet Connection Found! \nPlease check your connectivity.", Toast.LENGTH_LONG).show();
            NetworkAvalability = false;
        }
        else{
            Toast.makeText(this, "Internet Connection Found!", Toast.LENGTH_LONG).show();
            NetworkAvalability = true;
        }
        if (!authentication&&NetworkAvalability){
           StartAuth();        	
        }
    }
//=======================================================================================================================================
   @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
        	Accel_Settings(Disable,"3"); 
            doUnbindService();
            if(DataFile.exists()){
            	DataFile.delete();           
            }
            if(FileToBeSent.exists()){
            	FileToBeSent.delete();
            }
            if(LastUsedDataFile.exists()){
            	if(LastUsedDataFile.delete()){
                    Toast.makeText(MainActivity.this, "All the data files deleted!", Toast.LENGTH_LONG).show();            		
            	}            	
            }            
            boolean success = (new File("Temp.txt")).delete();
            if (success) {
                Toast.makeText(MainActivity.this, "TempFile Deleted", Toast.LENGTH_LONG).show();            		
            }
        } catch (Throwable t) {
        }
    }
 //=======================================================================================================================================
   @Override
   public void onBackPressed() {
       AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
       builder.setMessage("Do you really want to exit?").setCancelable(
               false).setPositiveButton("Quit",
                       new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   MainActivity.this.finish();
                   }
               }).setNegativeButton("Cancel",
                       new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {

                   }
               });
       AlertDialog alert = builder.create();
       alert.show();

   }
//=======================================================================================================================================    
    @Override
    protected void onStart() {
    	super.onStart();
        final TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabSpec spec1 = tabHost.newTabSpec("tab1");
        spec1.setContent(R.id.plot);
        spec1.setIndicator("PEDOMETER", null);
        tabHost.addTab(spec1);	
        TabSpec spec3 = tabHost.newTabSpec("tab3");
        spec3.setContent(R.id.power);
        spec3.setIndicator("DATA GRAPH", null);
        tabHost.addTab(spec3);	
        DataToPlot1 = FilteredData[0];
        DataToPlot2 = DataRec[0];
        DataToPlot3 = PeakFloatX;
        for(int i=0;i<tabHost.getTabWidget().getChildCount();i++)
        {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(Color.WHITE);
        }
        DataFile = createMockFile();
        FileToBeSent = new File(Environment.getExternalStorageDirectory() + File.separator +"Temp"+extension);
        try {
        	FileToBeSent.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	String str=getPrefs.getString("PeakThr", "2.2");
		try{
			PeakAccelThr= Float.parseFloat(str);    			
		}catch (NumberFormatException e){
			PeakAccelThr=2.2;
				}        	  

        init();
    }
//=======================================================================================================================================
    @Override
    protected void onPause() {
    	super.onPause();
    }
//=======================================================================================================================================
    @Override
    protected void onRestart() {
    	super.onRestart();
        setContentView(R.layout.activity_main);
        graphView.getGraphViewStyle().setTextSize(12);
    }
//=======================================================================================================================================
    @Override
    protected void onResume() {
    	super.onResume();
        boolean accel_en=false;
        String SelectedRate,str;
        String projectNameString ="AccelData" ;
    	getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    SyncData = getPrefs.getBoolean("SyncBox", true);
    	accel_en = getPrefs.getBoolean("Accel", false);
    	SelectedRate = getPrefs.getString("AccelRate", "3");
    	if(ChangeToInt(SelectedRate)==NORMAL){
    		StepSampleRate = StepInterval*NORMALRATE;
    	}else if(ChangeToInt(SelectedRate)==MEDIUM){
    		StepSampleRate = StepInterval*MEDIUMRATE;    		
    	}else if(ChangeToInt(SelectedRate)==FAST){
    		StepSampleRate = StepInterval*FASTRATE;    		
    	}else if(ChangeToInt(SelectedRate)==SUPERFAST){
    		StepSampleRate = StepInterval*SUPERFASTRATE;    		
    	}else{
        	StepSampleRate = StepInterval*NORMALRATE;    		
    	}
    	projectNameString = getPrefs.getString("ProjectName", "AccelData");
    	BoxFile = projectNameString;
    	if(accel_en){
    		Accel_Settings(Enable,SelectedRate);
    	}else{
    		Accel_Settings(Disable,SelectedRate);    		
    	}
    	str=getPrefs.getString("NumberOfSamples", "2000");
		try 
		{
			NumberOfSamples= Integer.parseInt(str);  
			if (NumberOfSamples<500){
				NumberOfSamples = 2000;
			}
		} 
		catch (NumberFormatException e)
		{
			NumberOfSamples=2000;
		} 
    }
	 	    public void init() {

	 	    	XdataSereis = new GraphViewSeries("Filtered Data",new GraphViewSeriesStyle(Color.RED, 2),
	        		new GraphViewData[]{new GraphViewData(0,0)});
	        YdataSereis = new GraphViewSeries("Raw Data",
	        		new GraphViewSeriesStyle(Color.GREEN, 2),
	        		new GraphViewData[]{new GraphViewData(0,0)});
	        ZdataSereis = new GraphViewSeries("Peak Detector Output",
	        		new GraphViewSeriesStyle(Color.BLUE, 2),
	        		new GraphViewData[]{new GraphViewData(0,0)});
	        graphView = new LineGraphView(this,"Accelerometer Data");
	        graphView.getGraphViewStyle().setTextSize(12);
	        graphView.setViewPort(0, 10);
	        graphView.addSeries(XdataSereis); // data
	        graphView.addSeries(YdataSereis); // data
	        graphView.addSeries(ZdataSereis); // data
	        graphView.setShowLegend(true);
	        graphView.setLegendAlign(LegendAlign.BOTTOM);
	        graphView.getGraphViewStyle().setVerticalLabelsColor(Color.WHITE);
	        graphView.setScrollable(true);
	        graphView.setScalable(true);
	        graphView.setManualYAxis(true);
	        graphView.setManualYAxisBounds(12, -12);
	        LinearLayout layout = (LinearLayout) findViewById(R.id.graph2);
	        layout.addView(graphView);

		}
//=======================================================================================================================================
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	    	super.onCreateOptionsMenu(menu);
	    	MenuInflater blowup = getMenuInflater();
	    	blowup.inflate(R.menu.main, menu);
	    	return true;
	    }
//=======================================================================================================================================
	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	switch(item.getItemId()){
	    	case R.id.about:
	    		Intent intent = new Intent("android.intent.action.ABOUT");
	    		startActivity(intent);
	    		break;	    		
	    	case R.id.app_settings:
	    		Intent intent2 = new Intent("android.intent.action.PREFS");
	    		startActivity(intent2);    		
	    		break;
	    	case R.id.exit:
	    	       AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    	       builder.setMessage("Do you really want to exit?").setCancelable(
	    	               false).setPositiveButton("Quit",
	    	                       new DialogInterface.OnClickListener() {
	    	                   public void onClick(DialogInterface dialog, int id) {
	    	                	   MainActivity.this.finish();
	    	                   }
	    	               }).setNegativeButton("Cancel",
	    	                       new DialogInterface.OnClickListener() {
	    	                   public void onClick(DialogInterface dialog, int id) {

	    	                   }
	    	               });
	    	       AlertDialog alert = builder.create();
	    	       alert.show();
	    		break;
	    	}
	    	return false;
	    }
//=======================================================================================================================================
// Handles:
//=======================================================================================================================================
	    private Runnable runnable = new Runnable() {
	  	   @Override
	  	   public void run() {
	  	      /* do what you need to do */
	  		   if(authentication)
	  		   {	
	  			   try {
	 				FileUtils.copyFile(DataFile, FileToBeSent);
	 				//FileUtils.copyFile(DataFile, LastUsedDataFile);	 				
	 				DataFile.delete();
	 			} catch (IOException e) {
	 				e.printStackTrace();
	 			}
	  			   DataFile = createMockFile();
	  			   UpLoadFile();		  				   
	  		   }
	  	   }
	  	};

//=======================================================================================================================================
// Box CallBacks
//=======================================================================================================================================
	     @Override
	     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	         super.onActivityResult(requestCode, resultCode, data);
	         if (requestCode == AUTH_REQUEST) {
	         	onAuthenticated(resultCode, data);
	         }
	     }
//=======================================================================================================================================
	 	private void onAuthenticated(int resultCode, Intent data) {
	         if (Activity.RESULT_OK != resultCode) {
	             Toast.makeText(this, "Unsuccessful authentication, Try Again.", Toast.LENGTH_LONG).show();
	             authentication = false;
	         }
	         else {
	             BoxAndroidOAuthData oauth = data.getParcelableExtra(OAuthActivity.BOX_CLIENT_OAUTH);
	             BoxAndroidClient client = new BoxAndroidClient(BoxClient.CLIENT_ID, BoxClient.CLIENT_SECRET, null, null);
	             client.authenticate(oauth);
	             if (client == null) {
	                 Toast.makeText(this, "fail", Toast.LENGTH_LONG).show();
	                 authentication = false;
	             }
	             else {
	             	((BoxClient)getApplication()).setClient(client);
	                 Toast.makeText(this, "Authentication succeed!", Toast.LENGTH_LONG).show();
	                 authentication = true;
	             }
	         }
	     }	 		
//=======================================================================================================================================
	 public void StartAuth(){
	 		Intent intent = OAuthActivity.createOAuthActivityIntent(this, BoxClient.CLIENT_ID, BoxClient.CLIENT_SECRET, false,BoxClient.REDIRECT_URL);
	 		this.startActivityForResult(intent,AUTH_REQUEST); 				
	 }
//=======================================================================================================================================
	 public void UpLoadFile(){
	        AsyncTask<Null, Integer, Null> task = new AsyncTask<Null, Integer, Null>() {
	            @Override
	            protected void onPostExecute(Null result) {
	            	Toast.makeText(MainActivity.this, "done uploading", Toast.LENGTH_LONG).show();
 	 			   	DoneUploading = true;
 	 			   	FileToBeSent.delete();
 	 		        FileToBeSent = new File(Environment.getExternalStorageDirectory() + File.separator +"Temp"+extension);
 	 		        try {
 	 		        	FileToBeSent.createNewFile();
 	 				} catch (IOException e) {
 	 					e.printStackTrace();
 	 				}
	                super.onPostExecute(result);
	            }
	            @Override
	            protected void onPreExecute() {
	            	Toast.makeText(MainActivity.this, "start uploading", Toast.LENGTH_LONG).show();
	                super.onPreExecute();
	            }
	            @Override
	            protected Null doInBackground(Null... params) {
		                BoxAndroidClient client = ((BoxClient) getApplication()).getClient();
		                try {
		                    client.getFilesManager().uploadFile(
		                        BoxFileUploadRequestObject.uploadFileRequestObject("0",BoxFile+counter+extension, FileToBeSent, client.getJSONParser()));
		                }
		                catch (Exception e) {
		                }	            		
	                return null;
	            }
	        };
	 task.execute();
}
//=======================================================================================================================================
// User's Methods and Callback:
//=======================================================================================================================================
	    private void UpdateFile(float[] data){
	    	samplecounter++;
	    	if ((samplecounter>NumberOfSamples)&&DoneUploading)
	    	{
	    		if (authentication&&SyncData){
	    			handler.postDelayed(runnable, 100);
	  			   DoneUploading = false;
	    		}
 			   samplecounter=0;
	    	}
		     try {
		    	 	FileUtils.writeStringToFile(DataFile,stepCounter+"\r\n",true);
				} catch (IOException e) {
					e.printStackTrace();
				}	        		 
	    }
//=======================================================================================================================================
	    private File createMockFile() {
	        try {        	
	            DataFile = new File(Environment.getExternalStorageDirectory() + File.separator + ProjectName+counter+extension);
	            try {
	    			DataFile.createNewFile();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	            return DataFile;
	        }
	        catch (Exception e) {
	            return null;
	        }
	    }

//=======================================================================================================================================
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
//=======================================================================================================================================
    void Accel_Settings(int En,String Rate) {
    	Message msg;
        if (mIsBound) {
            if (mService != null) {
                try {
                	if (En==1){
                        msg = Message.obtain(null, MyService.SETTING_ACCEL_SENSOR, En, ChangeToInt(Rate));
                        msg.replyTo = mMessenger;
                        mService.send(msg);                		
                	}else{
                		msg = Message.obtain(null, MyService.SETTING_ACCEL_SENSOR, En, ChangeToInt(Rate));                    	
                        msg.replyTo = mMessenger;
                        mService.send(msg);                		
                    }
                } catch (RemoteException e) {
                }
            }
        }
    }
//=======================================================================================================================================
private int ChangeToInt(String Rate){
	int rate=3;
	if(Rate.contentEquals("0")){
		rate = 0;
	}else if(Rate.contentEquals("1")){
		rate = 1;                		
	}else if(Rate.contentEquals("2")){
		rate = 2;                		
	}else if(Rate.contentEquals("3")){
		rate = 3;                		
	}else{
		rate = 3;                		
	}
	return rate;
}
//=======================================================================================================================================
public int getBatVoltage()
{
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent b = this.registerReceiver(null, ifilter);
    return b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
}
//=======================================================================================================================================
public int getBatTemp()
{
  IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
  Intent b = this.registerReceiver(null, ifilter);
  return b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
}
//=======================================================================================================================================
public int getBatTech()
{
  IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
  Intent b = this.registerReceiver(null, ifilter);
  return b.getIntExtra(BatteryManager.EXTRA_TECHNOLOGY, -1);
}
//=======================================================================================================================================
private void CheckIfStep(float[] data,float[] RawData){
	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	Intent batteryStatus = this.registerReceiver(null, ifilter);
	float batvoltage=0,batTemp;
	batvoltage = (float) (getBatVoltage()/1000.0);
	batTemp = (float) (getBatTemp()/10.0);
	//int percent= getPercent(batvoltage);
	int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	//int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	//float batteryPct = level / (float)scale;
	getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	String str;
	int goal;
	str = getPrefs.getString("Goal", "400");
	try {
		goal = Integer.parseInt(str);			
	} 
	catch (NumberFormatException e)
	{
			goal=400;
	}
	float[] temp = {0,0,0};
	float[] AVG = {0,0,0};
	float[] PEAK = {0,0,0};
	int direction = 0;
	step = (TextView) findViewById(R.id.stepee);
	goal_text = (TextView) findViewById(R.id.Goal);
	battery_volt = (TextView) findViewById(R.id.BatVoltage);
	battery_temp = (TextView) findViewById(R.id.BatTemp);
	est_time = (TextView) findViewById(R.id.Estimation);
	charge_level = (TextView) findViewById(R.id.BatPercent);
	battery_volt.setText("Battery Voltage: "+batvoltage+" V");
	battery_temp.setText("Battery Temp: "+batTemp+" C");
	charge_level.setText("Charge Level: "+level+"%");	
	if (StepSampleCounter <= StepSampleRate){
		if (PeakFloatX<Math.abs(RawData[0])){
			PeakFloatX = RawData[0];
		}
		if (PeakFloatY<Math.abs(RawData[1])){
			PeakFloatY = RawData[1];
		}
		if (PeakFloatZ<Math.abs(RawData[2])){
			PeakFloatZ = RawData[2];
		}
		SampleFloatX = SampleFloatX + Math.abs(data[0]);
		SampleFloatY = SampleFloatY + Math.abs(data[1]);
		SampleFloatZ = SampleFloatZ + Math.abs(data[2]);
		StepSampleCounter++;
	}else{
		AvgFloatX = SampleFloatX/StepSampleRate;
		AvgFloatY = SampleFloatY/StepSampleRate;
		AvgFloatZ = SampleFloatZ/StepSampleRate;
		temp[0] = (float) Math.sqrt(Math.pow(AvgFloatX,2)+Math.pow(PeakFloatX,2));
		temp[1] = (float) Math.sqrt(Math.pow(AvgFloatY,2)+Math.pow(PeakFloatY,2));
		temp[2] = (float) Math.sqrt(Math.pow(AvgFloatZ,2)+Math.pow(PeakFloatZ,2));
		direction = FindDirection(temp);
		AVG[0] = AvgFloatX;
		AVG[1] = AvgFloatY;
		AVG[2] = AvgFloatZ;
		PEAK[0] = PeakFloatX;
		PEAK[1] = PeakFloatY;
		PEAK[2] = PeakFloatZ;
		if ((AVG[direction]>=StepAccelTHR)&&(PEAK[direction]>=PeakAccelThr)){
			stepCounter++;			
			if (stepCounter>= goal){
				stepCounter =0;
				}
		}
		if (stepCounter>99&&stepCounter<999){
			step.setTextSize(150);
			}else if (stepCounter>999){
				step.setTextSize(100);				
			}else{
				step.setTextSize(200);				
			}
	    step.setText(""+stepCounter);
	    goal_text.setText("Goal: " + goal+" Steps");
		StepSampleCounter = 0;
		AvgFloatX = 0;
		PeakFloatX = 0;
		SampleFloatX = 0;
		AvgFloatY = 0;
		PeakFloatY = 0;
		SampleFloatY = 0;
		AvgFloatZ = 0;
		PeakFloatZ = 0;
		SampleFloatZ = 0;
	}       
}
//=======================================================================================================================================
private void FilterData(float[] input){
	if (FilterCounter>3){
		FilterCounter = 0;
	}
	FilterXData [FilterCounter] = input[0]; //Register X data
	FilterYData [FilterCounter] = input[1]; //Register Y data
	FilterZData [FilterCounter] = input[2]; //Register Z data
	FilteredData[0] = (FilterXData[0] + FilterXData[1] + FilterXData[2] + FilterXData[3])/4;
	FilteredData[1] = (FilterYData[0] + FilterYData[1] + FilterYData[2] + FilterYData[3])/4;
	FilteredData[2] = (FilterZData[0] + FilterZData[1] + FilterZData[2] + FilterZData[3])/4;				
	FilterCounter++;	
}
//=======================================================================================================================================
private int FindDirection(float[] data){
	// 0 ------> X
	// 1 ------> Y
	// 2 ------> Z	
	float maxval=0;
	int direction = 1;
	if (Math.max(data[0], data[1]) == data[0]){
		maxval = data[0];
		direction =0;
	}else if (Math.max(data[0], data[1]) == data[1]){
		maxval = data[1];		
		direction =1;
	}
	if (Math.max(maxval, data[2]) == data[2]){
		direction = 2;
	}	
	return direction;
	
}
//=======================================================================================================================================
// Button Callback:
//=======================================================================================================================================
public void ResetSteps(View V){
	TextView step = (TextView) findViewById(R.id.stepee);
	stepCounter = 0;
	step.setText("0.0");
}
//=======================================================================================================================================
public void PlotXData(View V){
	plot =1;
}
//=======================================================================================================================================
public void PlotYData(View V){
	plot =2;
}
//=======================================================================================================================================
public void PlotZData(View V){
	plot =3;
}
 }
