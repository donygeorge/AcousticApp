package edu.ucla.cens.acousticapp;

import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

public class AcousticApp_Pref extends Activity 
{

	private static final String TAG = "AcousticAppPreferences";
	public static final int VOICE_THRESH = 300;
	public static final int INT_MIN=CONSTS.INT_MIN;
	public static final int INT_MAX=CONSTS.INT_MAX;
	public static final int INTERVAL = CONSTS.INTERVAL;

	private SharedPreferences mSettings;
	private SharedPreferences.Editor mEditor;
	private Intent intent;

	Spinner frameInterval_spinner;
	Spinner frameSizeMin_spinner;
	Spinner frameSizeMax_spinner;
	Spinner frame_spinner;
	
	Boolean default_energy=CONSTS.ENERGY_B;
	Boolean default_zcr=CONSTS.ZCR_B;
	Boolean default_raw=CONSTS.RAW_B;
	Boolean default_spl=CONSTS.SPL_B;
	Boolean default_adv=CONSTS.ADV_B;

	private CheckBox energy_cb;
	private CheckBox zcr_cb;
	private CheckBox raw_cb;
	private CheckBox spl_cb;
	private CheckBox adv_cb;


	private String frameIntervalArray_spinner[]= {"30 seconds", "1 minute", "2 minutes", "3 minutes", "5 minutes", "10 minutes", "20minutes", "30 minutes", "1 hour", "2 hours"};
	private int frameIntervalValues_array[]= {30,60,120,180,300,600,1200,1800,3600,7200};
	private String frameSizeMinArray_spinner[]=  {"1 second", "2 seconds", "5 seconds", "10 seconds", "15 seconds", "30 seconds", "45 seconds", "50 seconds" ,"55 seconds","60 seconds","5 minutes", "10 minutes","15 minutes","20 minutes", "30 minutes", "1 hour"};
	private int frameSizeMinValues_array[]= {1,2,5,10,15,30,45,50, 55,60,300,600,900,1200,1800,3600};
	private String frameSizeMaxArray_spinner[]=  {"1 second", "2 seconds", "5 seconds", "10 seconds", "15 seconds", "30 seconds", "45 seconds", "50 seconds",  "55 seconds", "60 seconds","5 minutes", "10 minutes","15 minutes","20 minutes", "30 minutes","1 hour"};
	private int frameSizeMaxValues_array[]= {1,2,5,10,15,30,45,50, 55,60,300,600,900,1200,1800,3600};
	private String frameArray_spinner[]= {"32 ms", "64 ms", "128 ms", "256 ms", "512 ms"};
	private int frameValues_array[]= {32,64,128,256,512};


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pref);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mEditor = mSettings.edit();

		energy_cb = (CheckBox)findViewById(R.id.energy_cb);
		zcr_cb = (CheckBox)findViewById(R.id.zcr_cb);
		raw_cb = (CheckBox)findViewById(R.id.raw_cb);
		spl_cb = (CheckBox)findViewById(R.id.spl_cb);
		adv_cb = (CheckBox)findViewById(R.id.adv_cb);


		//frameIntervalArray_spinner=new String[7];
		//frameIntervalArray_spinner={"30 seconds", "1 minute", "2 minutes", "3 minutes", "5 minutes", "10 minutes", "30 minutes", "60 minutes"};
		frameInterval_spinner = (Spinner) findViewById(R.id.frameInterval_spinner);
		ArrayAdapter<CharSequence> frameInterval_adapter = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_dropdown_item, frameIntervalArray_spinner);
		frameInterval_spinner.setAdapter(frameInterval_adapter);
		int temp_frameInterval =  mSettings.getInt("alarm_interval",INTERVAL);
		int res = Arrays.binarySearch(frameIntervalValues_array, temp_frameInterval);
		if(res>=0)
			frameInterval_spinner.setSelection(res);
		else
			frameInterval_spinner.setSelection(5);

		frameSizeMin_spinner = (Spinner) findViewById(R.id.frameSizeMin_spinner);
		ArrayAdapter<CharSequence> frameSizeMin_adapter = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_dropdown_item, frameSizeMinArray_spinner);
		frameSizeMin_spinner.setAdapter(frameSizeMin_adapter);
		int temp_frameSizeMin =  mSettings.getInt("min",INT_MIN);
		res = Arrays.binarySearch(frameSizeMinValues_array, temp_frameSizeMin);
		if(res>=0)
			frameSizeMin_spinner.setSelection(res);
		else
			frameSizeMin_spinner.setSelection(10);

		frameSizeMax_spinner = (Spinner) findViewById(R.id.frameSizeMax_spinner);
		ArrayAdapter<CharSequence> frameSizeMax_adapter = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_dropdown_item, frameSizeMaxArray_spinner);
		frameSizeMax_spinner.setAdapter(frameSizeMax_adapter);
		int temp_frameSizeMax =  mSettings.getInt("max",INT_MAX);
		res = Arrays.binarySearch(frameSizeMaxValues_array, temp_frameSizeMax);
		if(res>=0)
			frameSizeMax_spinner.setSelection(res);
		else
			frameSizeMax_spinner.setSelection(10);

		frame_spinner = (Spinner) findViewById(R.id.frame_spinner);
		ArrayAdapter<CharSequence> frame_adapter = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_dropdown_item, frameArray_spinner);
		frame_spinner.setAdapter(frame_adapter);
		int temp_frame =  mSettings.getInt("frameLength",32);
		res = Arrays.binarySearch(frameSizeMinValues_array, temp_frame);
		if(res>=0)
			frame_spinner.setSelection(res);
		else
			frame_spinner.setSelection(0);

		Boolean temp_energy =  mSettings.getBoolean("energy", default_energy);
		energy_cb.setChecked(temp_energy);
		Boolean temp_zcr =  mSettings.getBoolean("zcr", default_zcr);
		zcr_cb.setChecked(temp_zcr);
		Boolean temp_raw =  mSettings.getBoolean("raw", default_raw);
		raw_cb.setChecked(temp_raw);
		Boolean temp_spl =  mSettings.getBoolean("spl", default_spl);
		spl_cb.setChecked(temp_spl);
		Boolean temp_adv =  mSettings.getBoolean("adv", default_adv);
		adv_cb.setChecked(temp_adv);

		Button start_b = (Button)findViewById(R.id.start_b);
		start_b.setOnClickListener(startListener);
		Button stop_b = (Button)findViewById(R.id.stop_b);
		stop_b.setOnClickListener(stopListener);
		Button reset_b = (Button)findViewById(R.id.reset_b);
		reset_b.setOnClickListener(resetListener);
	}

	@Override
	public void onBackPressed()
	{
		save();
		super.onBackPressed();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.pref_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case R.id.save_item:
			save();
			return true;
		case R.id.cancel_item:
			finish();
			return true;
		}
		return false;
	}

	private OnClickListener startListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			Log.i(TAG,"Start Clicked");
			if(!mSettings.getBoolean("status", false))
			{
				save();
				intent = new Intent(AcousticApp_Pref.this, AcousticAppService.class);

				Bundle b= new Bundle();
				int temp_frameInterval =  mSettings.getInt("alarm_interval",INTERVAL);
				int temp_frameSizeMin =  mSettings.getInt("min",INT_MIN);
				int temp_frameSizeMax =  mSettings.getInt("max",INT_MAX);
				int temp_frame =  mSettings.getInt("frameLength",32);
				
				Boolean temp_energy =  mSettings.getBoolean("energy", default_energy);
				Boolean temp_zcr =  mSettings.getBoolean("zcr", default_zcr);
				Boolean temp_raw =  mSettings.getBoolean("raw", default_raw);
				Boolean temp_spl =  mSettings.getBoolean("spl", default_spl);
				Boolean temp_adv =  mSettings.getBoolean("adv", default_adv);
				
				Log.i(TAG,"VALUES : frame Interval: "+temp_frameInterval + ", FrameMin: "+temp_frameSizeMin+ ", FrameMax: "+temp_frameSizeMax);

				b.putInt("recordmax", temp_frameSizeMax);
				b.putInt("recordmin", temp_frameSizeMin);
				b.putInt("frameInterval", temp_frameInterval);
				b.putInt("frameLength", temp_frame);
				
				b.putBoolean("energy", temp_energy);
				b.putBoolean("zcr", temp_zcr);
				b.putBoolean("raw", temp_raw);
				b.putBoolean("spl", temp_spl);
				b.putBoolean("adv", temp_adv);
				
				intent.putExtras(b);

				startService(intent);
				Toast.makeText(getApplicationContext(), "Service Started", Toast.LENGTH_SHORT).show();
				Log.i(TAG, "Started AcousticAppService");
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Service Already Running", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private OnClickListener stopListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			Log.i(TAG,"Stop Clicked");
			//			if(intent != null)
			//				stopService(intent);

			if(mSettings.getBoolean("status", false))
			{	
				stopService(new Intent(AcousticApp_Pref.this, AcousticAppService.class));
				Toast.makeText(getApplicationContext(), "Service Stopped", Toast.LENGTH_SHORT).show();
				Log.i(TAG,"Stopped Service");
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Service Already Stopped", Toast.LENGTH_SHORT).show();				
			}
		}
	};

	private OnClickListener resetListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			mEditor.putInt("alarm_interval", INTERVAL);
			mEditor.putInt("min", INT_MIN);
			mEditor.putInt("max", INT_MAX);
			mEditor.putInt("frameLength", 32);
			
			mEditor.putBoolean("energy", default_energy);
			mEditor.putBoolean("zcr", default_zcr);
			mEditor.putBoolean("raw", default_raw);
			mEditor.putBoolean("spl", default_spl);
			mEditor.putBoolean("adv", default_adv);
			
			mEditor.putInt("minute", -1);
			mEditor.putInt("hour", -1);
			mEditor.putInt("day", -1);
			mEditor.putInt("week", -1);
			
			mEditor.putString("workdone", "0");
			mEditor.putInt("day_count", 0);
			mEditor.putInt("week_count", 0);
			
			mEditor.putInt("calib", VOICE_THRESH);
			mEditor.putString("voicevalue", "N/A ");
			
			for(int i=0;i<24;i++)
			{
				mEditor.putInt("d"+i, -1);
			}
			for(int i=0;i<7;i++)
			{
				mEditor.putInt("w"+i, -1);
			}
			
			mEditor.commit();
			
			Toast.makeText(getApplicationContext(), "Settings Resetted", Toast.LENGTH_SHORT).show();			
		}
	};

	
	public void save()
	{
		mEditor.putInt("alarm_interval", frameIntervalValues_array[frameInterval_spinner.getSelectedItemPosition()]);
		int temp_min=frameSizeMinValues_array[frameSizeMin_spinner.getSelectedItemPosition()];
		int temp_max=frameSizeMaxValues_array[frameSizeMax_spinner.getSelectedItemPosition()];
		if(temp_min>temp_max)
			temp_min=temp_max;
		mEditor.putInt("min", temp_min);
		mEditor.putInt("max", temp_max);

		mEditor.putInt("frameLength", frameValues_array[frame_spinner.getSelectedItemPosition()]);	
		
		mEditor.putBoolean("energy", energy_cb.isChecked());
		mEditor.putBoolean("zcr", zcr_cb.isChecked());
		mEditor.putBoolean("raw", raw_cb.isChecked());
		mEditor.putBoolean("spl", spl_cb.isChecked());
		mEditor.putBoolean("adv", adv_cb.isChecked());
		
		mEditor.commit();
	}    
}