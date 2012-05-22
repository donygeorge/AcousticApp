package edu.ucla.cens.acousticapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class ServiceStarter extends BroadcastReceiver 
{
	//Used to start the service at Boot time
    private static final String TAG = "AcousticApp";

    @Override
    public void onReceive(Context context, Intent intent)
    {
    	Log.i(TAG, "Starting at Boot Time");

    	if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) 
    	{
    		Intent serviceIntent = new Intent(context, AcousticAppService.class);
    		serviceIntent.setAction("boot_alarm");
    		context.startService(serviceIntent);
    	} 
    	else 
    	{
    		Log.e("ConnectionChecker", "Received unexpected intent " + intent.toString());
    	}
    }
}

