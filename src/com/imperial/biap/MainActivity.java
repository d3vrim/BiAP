package com.imperial.biap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData; 
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	// Debugging
    private static final String TAG = "Biap";
    private static final boolean D = true;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;
    
    //Notification
    private NotificationManager mNotificationManager;
    private int notificationIDConnection = 100;
    private int notificationIDGlucoseWarning = 101;
    private int notificationIDInsulinWarning = 102;
    private int notificationIDdGWarning = 103;
    private int numMessages = 0;
    
    GraphViewSeries exampleSeries = new GraphViewSeries(new GraphViewData[] {});
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if(D) Log.e(TAG, "+++ ON CREATE +++");
		
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        
        
        GraphView graphView = new BarGraphView(MainActivity.this, "GraphViewDemo");  
//        graphView.addSeries(exampleSeries);  
        graphView.getGraphViewStyle().setGridColor(Color.BLACK);
        graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
        graphView.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
        graphView.setScrollable(true);
        graphView.setViewPort(calendar.getTimeInMillis(), calendar.getTimeInMillis() + 10000);
        
        /*
		 * time as label formatter
		 */
        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Date d = new Date((long) value);
                    return dateFormat.format(d);
                }
                return null; // let graphview generate Y-axis label for us
            }
        });
        
        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);  
        layout.addView(graphView); 
	}
	
	@Override
    public void onStart() {
		super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
//        	if(!(mBluetoothService.getState() == BluetoothService.STATE_CONNECTED)){
//        		Toast.makeText(this, "Bluetooth is enabled, please conenct to a device", Toast.LENGTH_LONG).show();
//        	}
        }
	}
	
	@Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the running threads
//        if (mBluetoothService != null) mBluetoothService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Initialize the BluetoothChatService to perform bluetooth connections
                mBluetoothService = new BluetoothService(this, mHandler);
                // Attempt to connect to the device
                mBluetoothService.connect(device);
                // Attempt to connect to the device
//                mConnectThread = new ConnectThread(device);
//                mConnectThread.start();
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
            	Toast.makeText(this, "Bluetooth is enabled, please conenct to a device", Toast.LENGTH_SHORT).show();
//                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
//                finish();
            }
        }
    }
	
	protected void displayNotification(String title, String text, int notificationID, int alertlevel) {
	      Log.i("Start", "notification");

	      /* Invoking the default notification service */
	      NotificationCompat.Builder  mBuilder = 
	      new NotificationCompat.Builder(this);	

	      mBuilder.setContentTitle(title);
	      mBuilder.setContentText(text);
	      mBuilder.setTicker("BiAP Alert!");
	      mBuilder.setSmallIcon(R.drawable.ic_launcher);
	      
	      if((notificationID == notificationIDConnection) && (alertlevel == 1)){
	    	  //Blue LED notification 
	    	  mBuilder.setLights(0xff0000ff, 1000, 1000);
	    	  mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
	    	  mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000, 1000 });
	    	  numMessages++;
	    	  
	      }else if(notificationID == notificationIDGlucoseWarning){
	    	  if(alertlevel == 1){
	    		  //Red LED notification 
	    		  mBuilder.setLights(0xffff0000, 1000, 1000);
	    		  mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, });
	    	  }
	    	  if(alertlevel == 2){
	    		  //Red LED notification 
	    		  mBuilder.setLights(0xffff0000, 1000, 1000);
		    	  mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
		    	  mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000, 1000 });
	    	  }
	    	  
	      }
	      
	      /* Creates an explicit intent for an Activity in your app */
	      Intent resultIntent = new Intent(this, MainActivity.class);

	      TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
	      stackBuilder.addParentStack(MainActivity.class);

	      /* Adds the Intent that starts the Activity to the top of the stack */
	      stackBuilder.addNextIntent(resultIntent);
	      PendingIntent resultPendingIntent =
	         stackBuilder.getPendingIntent(
	            0,
	            PendingIntent.FLAG_UPDATE_CURRENT
	         );

	      mBuilder.setContentIntent(resultPendingIntent);

	      mNotificationManager =
	      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

	      /* notificationID allows you to update the notification later on. */
	      mNotificationManager.notify(notificationID, mBuilder.build());
	   }
	
	protected void cancelNotification(int notificationID) {
	      Log.i("Cancel", "notification");
	      mNotificationManager.cancel(notificationID);
    }
	
	// The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	if(numMessages > 0){
                		cancelNotification(notificationIDConnection); 
                	}
                	break;
                case BluetoothService.STATE_CONNECTING:
                	Toast.makeText(getApplicationContext(), "Connecting ", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.STATE_NONE:
                    break;
                }
                break;
            case MESSAGE_READ:	
                String[] readBuf = (String[]) msg.obj;
                
                Calendar calendar = Calendar.getInstance();
                
                GraphViewData newData = new GraphViewData(calendar.getTimeInMillis() , Float.parseFloat(readBuf[4]));
                exampleSeries.appendData(newData, true, 12);
//                graphView.setViewPort(calendar.getTimeInMillis() - 10, calendar.getTimeInMillis());
                
                if(Float.parseFloat(readBuf[4]) < 4){
                	displayNotification("Hypoglycemia", "Glucose levels in Hypoglycemic range", notificationIDGlucoseWarning, 2);
                }else if((Float.parseFloat(readBuf[4])) < 6){
                	displayNotification("Glucose Levels Low", "Glucose levels are nearing Hypoglycemic range", notificationIDGlucoseWarning, 1);
                }else if(Float.parseFloat(readBuf[4]) > 9){
                	displayNotification("Glucose Levels High", "Glucose levels are nearing Hyperglycemic range", notificationIDGlucoseWarning, 1);
                }else if(Float.parseFloat(readBuf[4]) > 11.1){
                	displayNotification("Hyperglycemia", "Glucose levels in Hyperglycemic range", notificationIDGlucoseWarning, 2);
                }else{
                	displayNotification("Glucose OK", "Glucose levels are normal", notificationIDGlucoseWarning, 0);
                }
                
                
//                Toast.makeText(getApplicationContext(), readBuf[2], Toast.LENGTH_SHORT).show();
                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                if(msg.getData().getString(TOAST).equalsIgnoreCase("Device connection was lost")){
                	displayNotification("No Bluetooth Connection", "Please connect to a Bluetooth Device", notificationIDConnection, 1);
                }
                break;
            }
        }
    };
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if(id == R.id.option_connect){
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
