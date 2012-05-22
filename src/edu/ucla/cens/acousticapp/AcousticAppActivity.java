package edu.ucla.cens.acousticapp;

//import java.io.BufferedInputStream;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class AcousticAppActivity extends Activity 
{
	/** Name of this application */
	public static final String APP_NAME = "AcousticApp";
	private static final String TAG = "AcousticAppActivity";
	public static final int VOICE_THRESH = 300;
	public static final int INT_MIN=CONSTS.INT_MIN;
	public static final int INT_MAX=CONSTS.INT_MAX;
	public static final int INTERVAL = CONSTS.INTERVAL;
	private Intent intent;
	private SharedPreferences mSettings;
	private SharedPreferences.Editor mEditor;
	public Handler handler= new Handler();
	Boolean update_bool=false;
	Boolean calib_b=false;
	Thread updateThread;
	String versionNo;
	SeekBar seekBar;
	TextView calib_tv;
	TextView voice_tv;
	Button calib_button;

	Boolean default_energy=CONSTS.ENERGY_B;
	Boolean default_zcr=CONSTS.ZCR_B;
	Boolean default_raw=CONSTS.RAW_B;
	Boolean default_spl=CONSTS.SPL_B;
	Boolean default_adv=CONSTS.ADV_B;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);   
		mEditor = mSettings.edit();

		//Setting Calibration to false
		mEditor.putBoolean("calib_b", false);
		//mEditor.putBoolean("status", false);
		//mEditor.putBoolean("record_status", false);

		//Hiding the lower rows in the display if basic is chosen, used when statistics need not be displayed in the app
		if(CONSTS.BASIC)
		{
			TableRow t1= (TableRow)findViewById(R.id.tableRow_result_title);
			TableRow t2= (TableRow)findViewById(R.id.tableRow_minute);
			TableRow t3= (TableRow)findViewById(R.id.tableRow_hour);
			TableRow t4= (TableRow)findViewById(R.id.tableRow_day);
			TableRow t5= (TableRow)findViewById(R.id.tableRow_week);
			TableLayout tl = (TableLayout)findViewById(R.id.tableLayout1);
			t1.setVisibility(TableRow.GONE);
			t2.setVisibility(TableRow.GONE);
			t3.setVisibility(TableRow.GONE);
			t4.setVisibility(TableRow.GONE);
			t5.setVisibility(TableRow.GONE);
			tl.setBackgroundColor(0x000000);
		}
		seekBar= (SeekBar)findViewById(R.id.seekBar1);
		calib_tv = (TextView)findViewById(R.id.calib_tv);
		voice_tv = (TextView)findViewById(R.id.voice_tv);

		//Used for calibration
		calib_button = (Button) findViewById(R.id.calib_b);
		calib_button.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				if(calib_b)
				{
					calib_b = !calib_b;
					toggleCalib();
				}
			}
		});

		//store the currnt version No
		try
		{
			versionNo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Log.v(TAG, e.getMessage());
		}
		String versionNo_stored = mSettings.getString("versionno", "0.0");
		Log.i(TAG,"Store: "+versionNo_stored);
		Log.i(TAG,"Orig: "+versionNo);

		//If version Number changes, reset all the settings
		if(!versionNo.equals(versionNo_stored))
		{
			mEditor.putString("versionno", versionNo);
			mEditor.commit();
			reset();
		}

		
		//If app is not running, start it
		if(!mSettings.getBoolean("status", false))
		{
			intent = new Intent(AcousticAppActivity.this, AcousticAppService.class);
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

			//Start Service
			intent.putExtras(b);
			startService(intent);
			Toast.makeText(getApplicationContext(), "Service Started", Toast.LENGTH_SHORT).show();
		}

		//Thread to Update the UI
		mEditor.putBoolean("ui",true);
		mEditor.commit();
		update_bool=true;
		updateThread = new Thread(new UIUpdater());
		updateThread.start();
		Log.i(TAG,"Activity oncreate completed");
	}

	@Override
	public void onBackPressed()
	{
		//Switched off UI and Calibration Thread
		update_bool=false;
		mEditor.putBoolean("ui",false);
		mEditor.putBoolean("calib_b", false);
		mEditor.commit();
		super.onBackPressed();
	}

	@Override
	public void onPause()
	{
		//Switchs off UpdateUi Thread
		mEditor.putBoolean("ui",false);
		mEditor.commit();

		update_bool=false;
		super.onPause();
	}

	@Override
	public void onResume()
	{
		//Restarts UpdateUI thread
		mEditor.putBoolean("ui",true);
		mEditor.commit();

		update_bool=true;
		updateThread = new Thread(new UIUpdater());
		updateThread.start();
		super.onResume();
	}


	@Override
	public void onDestroy()
	{
		//Closes UI Thread
		update_bool=false;
		mEditor.putBoolean("ui",false);
		mEditor.putBoolean("calib_b", false);
		mEditor.commit();
		//		mEditor.putBoolean("status", false);
		//		mEditor.commit();
		super.onDestroy();
	}

	//Creates the Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case R.id.pref_item:
			Intent pref_Intent = new Intent(AcousticAppActivity.this, AcousticApp_Pref.class);
			startActivity(pref_Intent);
			return true;
		case R.id.calib_item:
			calib_b = !calib_b;
			if(!CONSTS.BASIC)
				toggleCalib();
			return true;
		case R.id.exit_item:
			onBackPressed();
			return true;
		}
		return false;
	}

	//Function to toggle if Calibration is enabled
	public void toggleCalib()
	{
		TableRow tc1= (TableRow)findViewById(R.id.tableRow_calib_title);
		TableRow tc2= (TableRow)findViewById(R.id.tableRow_voice);
		TableRow tc3= (TableRow)findViewById(R.id.tableRow_calibvalue);
		TableRow tc4= (TableRow)findViewById(R.id.tableRow_seekbar);
		TableRow tc5= (TableRow)findViewById(R.id.tableRow_calibbutton);
		if(calib_b)
		{
			mEditor.putBoolean("calib_b", true);
			mEditor.commit();
			tc1.setVisibility(TableRow.VISIBLE);
			tc2.setVisibility(TableRow.VISIBLE);
			tc3.setVisibility(TableRow.VISIBLE);
			tc4.setVisibility(TableRow.VISIBLE);
			tc5.setVisibility(TableRow.VISIBLE);
			int temp_calib=mSettings.getInt("calib", VOICE_THRESH);
			seekBar.setProgress(temp_calib);
			calib_tv.setText(temp_calib+" ");
			seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
			Toast.makeText(getApplicationContext(), "Calibration Mode On, Use Slider to calibrate voice/non-voice detection", Toast.LENGTH_SHORT).show();	
		}
		else
		{
			mEditor.putBoolean("calib_b", false);
			mEditor.commit();
			tc1.setVisibility(TableRow.GONE);
			tc2.setVisibility(TableRow.GONE);
			tc3.setVisibility(TableRow.GONE);
			tc4.setVisibility(TableRow.GONE);
			tc5.setVisibility(TableRow.GONE);
		}
	}

	//Seekbar to change the Calibration Value
	private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener()
	{

		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
		{
			calib_tv.setText(progress +" ");
			mEditor.putInt("calib", progress);
			mEditor.commit();
		}
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	};


	//Resets all settings
	public void reset()
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
	}

	//Class which control the UI, updates the UI, runs in a separate thread
	class UIUpdater implements Runnable 
	{ 
		TextView status_tv;
		TextView recordstatus_tv;
		TextView spl_tv;
		TextView minute_tv;
		TextView hour_tv;
		TextView day_tv;
		TextView week_tv;
		TextView curHealth_tv;
		TextView procHealth_tv;
		int temp=1;

		UIUpdater()
		{
			status_tv = (TextView)findViewById(R.id.status_tv);
			recordstatus_tv = (TextView)findViewById(R.id.recordstatus_tv);
			spl_tv = (TextView)findViewById(R.id.spl_tv);
			minute_tv = (TextView)findViewById(R.id.minute_tv);
			hour_tv = (TextView)findViewById(R.id.hour_tv);
			day_tv = (TextView)findViewById(R.id.day_tv);
			week_tv = (TextView)findViewById(R.id.week_tv);	
			curHealth_tv = (TextView)findViewById(R.id.curhealth_tv);	
			procHealth_tv = (TextView)findViewById(R.id.prochealth_tv);	
		}

		public void run() 
		{
			synchronized (this) 
			{ 
				Log.i(TAG,"Update UI Started");
				while(update_bool)
				{
					try 
					{
						//Refresh UI every 100 ms
						this.wait(100);
						update();
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
					}
				}
				Log.i(TAG,"Update  UI Stopped");
			}
		}

		void update()
		{
			//used to change UI elements
			handler.post(new Runnable() 
			{
				//@Override
				public void run() 
				{
					if(mSettings.getBoolean("status", false))
						status_tv.setText("Running ");
					else
						status_tv.setText("Not Running ");

					if(mSettings.getBoolean("record_status", false))
						recordstatus_tv.setText("Recording ");
					else
					{
						mEditor.putInt("spl_value", -1);
						mEditor.putString("voicevalue", "N/A ");
						mEditor.commit();
						recordstatus_tv.setText("Not Recording ");
					}
					int temp_spl = mSettings.getInt("spl_value", -1);
					if(temp_spl == -1)
						spl_tv.setText("N/A ");
					else
						spl_tv.setText(temp_spl+"dB ");


					//Health
					int rTotal = mSettings.getInt("rTotal", 0);
					int qTotal = mSettings.getInt("qTotal", 0);
					Float lastProc = mSettings.getFloat("lastProc", -1);
					
					if(lastProc==-1)
						curHealth_tv.setText("N/A  ");
					else
						curHealth_tv.setText((int)(lastProc*100)+"%  ");
					
					if(qTotal==0)
						procHealth_tv.setText("100%  ");
					else
						procHealth_tv.setText((qTotal*100)/rTotal+"%  ");
					
					
					
					if(!CONSTS.BASIC)
					{
						int minute_percent = mSettings.getInt("minute", -1);
						if(minute_percent<0)
							minute_tv.setText("N/A ");
						else
							minute_tv.setText(mSettings.getInt("minute", 0)+"% Voice ");

						int hour_percent = mSettings.getInt("hour", -1);
						if(hour_percent<0)
							hour_tv.setText("N/A ");
						else
							hour_tv.setText(hour_percent +"% Voice ");

						int day_percent = mSettings.getInt("day", -1);
						if(day_percent<0)
							day_tv.setText("N/A ");
						else
							day_tv.setText(day_percent +"% Voice ");

						int week_percent = mSettings.getInt("week", -1);
						if(week_percent<0)
							week_tv.setText("N/A ");
						else
							week_tv.setText(week_percent +"% Voice ");

						voice_tv.setText(mSettings.getString("voicevalue","N/A "));

					}
				}  
			});
		}
	}

}