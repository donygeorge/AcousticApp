
package edu.ucla.cens.acousticapp;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.probemanager.ProbeBuilder;
import org.ohmage.probemanager.ProbeWriter;

public class OhmageProbeWriter extends ProbeWriter 
{
	private static final String TAG = "AcousticAppProbe";

    private static final String OBSERVER_ID = "org.ohmage.probes.acousticProbe";
    private static final int OBSERVER_VERSION = 1;

    private static final String STREAM_NORMAL = "normal";
    private static final int STREAM_NORMAL_VERSION = 1;

    public OhmageProbeWriter(Context context) {
        super(context);
    }

    public void writeData(JSONObject data) {

        try 
        {
            ProbeBuilder probe = new ProbeBuilder(OBSERVER_ID, OBSERVER_VERSION);
            probe.setStream(STREAM_NORMAL, STREAM_NORMAL_VERSION);

            probe.setData(data.toString());
            probe.write(this);
        } 
        catch (RemoteException e) 
        {
        	Log.e(TAG,"Error in Probe :"+e);
            e.printStackTrace();
        }
    }
    
}
