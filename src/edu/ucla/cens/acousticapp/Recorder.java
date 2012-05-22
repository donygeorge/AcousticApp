package edu.ucla.cens.acousticapp;

import java.lang.Object;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
//import android.util.Log;

import edu.ucla.cens.systemlog.Log;
import edu.ucla.cens.systemlog.ISystemLog;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Recorder implements Runnable 
{
	//Loads NDK Library
	static {  
		System.loadLibrary("computeFeatures");  
	}

	private native double[] features(short[] array);
	private native void audioFeatureExtractionInit();
	private native void audioFeatureExtractionDestroy();
	//private native void helloLog(String logThis);  

	/** Name of this application */
	public static final String APP_NAME = "AcousticApp";
	public static final int VOICE_THRESH = 300;
	public static final int SAMPLERATE = 8000;
	public static final int INT_MIN=CONSTS.INT_MIN;
	public static final int INT_MAX=CONSTS.INT_MAX;

	private static final String TAG = "AcousticApp";
	private static final String DATA_TAG = "AcousticAppData";
	private String versionNo = "1.0";

	private MyQueuePopper myQueuePopper;
	private CircularBufferFeatExtractionInference cirBuffer; 

	private int audioFormat;
	private int frequency;
	private int channelConfig;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private short nChannels;
	private short bSamples;

	private volatile boolean isPaused;
	//private File fileName;
	private volatile boolean isRecording;
	private final Object mutex = new Object();

	private long start=0;

	private int FRAME_SIZE = 256;
	private int FRAME_STEP = FRAME_SIZE/2; 
	private int	framePeriod;
	public long timeRec=0;
	short[] tempBuffer;

	private String deviceName="";

	//Arraylists to store fetures
	private ArrayList energy_arr=new ArrayList();
	private ArrayList zcr_arr=new ArrayList();
	private ArrayList voice_arr=new ArrayList();
	private ArrayList spl_arr=new ArrayList();
	private ArrayList adv1_arr=new ArrayList();		//no_correlation_peak
	private ArrayList adv2_arr=new ArrayList();		//max_correlation_peak_value
	private ArrayList adv3_arr=new ArrayList();		//max_correlation_peak_lag
	private ArrayList adv4_arr=new ArrayList();		//spectral_entropy
	private ArrayList adv5_arr=new ArrayList();		//relative_spectral_entropy
	private ArrayList adv6_arr=new ArrayList();		//adv_energy
	//private ArrayList adv7_arr=new ArrayList();		//correlation_peak_value_array
	//private ArrayList adv8_arr=new ArrayList();		//correlation_peak_lag_array
	double dataArray[];
	//double temp7_arr[];
	//double temp8_arr[];
	double [] filtered;

	// values for A-weighting filter from here:
	// http://www.mathworks.com/matlabcentral/fileexchange/21384-continuous-sound-and-vibration-analysis
	final double [] spl_b = {0.44929985, -0.89859970, -0.44929985, 1.79719940, -0.44929985, -0.89859970, 0.44929985};
	final double [] spl_a = {1.00000000, -3.22907881, 3.35449488, -0.73178437, -0.62716276, 0.17721420, 0.05631717};


	//to create Json Objects
	private JsonManager jsonManager;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

	//count
	int qcount=0;
	int rcount=0;
	Boolean procComplete=true;

	private int sync_id_counter = 0;
	private long tempTimeStamp = 0;

	private AudioRecord recordInstance;
	private MediaRecorder mrecordInstance;
	AcousticAppService obj;
	private Feature feature;
	private int frameLength=-1;
	int bufferRead;
	int bufferSize;

	//Preferences
	private SharedPreferences mSettings;
	private SharedPreferences.Editor mEditor;

	//To create Raw Audio Files
	private int payloadSize=0;
	RandomAccessFile fWriter;
	File file;
	private String fDirPath;
	private String fPath;
	private String fPath_new;
	private Boolean fileStatus_b=false;
	private boolean cAudio_b=false;



	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	static SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	static Calendar cal;
	int count=0;

	//use for json partial
	int partno=0;
	int partcount=0;
	String now_str;

	//Constructor
	public Recorder(AcousticAppService ob, int sampleRate_in, int channelConfig_in, int audioFormat_in, int frameLength_inp,
			String deviceName_in, String versionNo_in, Feature feature_in) 
	{
		super();
		Log.setAppName(APP_NAME);
		frequency=sampleRate_in;
		channelConfig=channelConfig_in;
		audioFormat=audioFormat_in;
		frameLength = frameLength_inp;
		deviceName=deviceName_in;
		versionNo =versionNo_in;
		feature=feature_in;
		obj=ob;
		fDirPath=Environment.getExternalStorageDirectory().getAbsolutePath() +"/AcousticAppData";
		String temp_filename = getfilename();
		fPath=fDirPath+"/"+"p"+temp_filename+".wav";
		fPath_new=fDirPath+"/"+temp_filename+".wav";

		timeRec=0;//keeps track of the time in Msec recorded in the current run, used for skipping frames

		//Sets the framesize
		if(frameLength>0)
			FRAME_SIZE = frameLength * 8;
		FRAME_STEP = (int)FRAME_SIZE/2;
		framePeriod = FRAME_STEP;

		//Initialiazing Audio Parameters
		if (audioFormat == AudioFormat.ENCODING_PCM_16BIT)
		{
			bSamples = 16;
		}
		else
		{
			bSamples = 8;
		}

		if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO)
		{
			nChannels = 1;
		}
		else
		{
			nChannels = 2;
		}

		this.setPaused(false);
		log("in recorder constructor, frameLength: "+frameLength);

		mSettings = PreferenceManager.getDefaultSharedPreferences(obj);
		mEditor = mSettings.edit();

		jsonManager= new JsonManager("AcousticApp",versionNo,deviceName);

		//add a new buffer for putting audio-stuff
		cirBuffer = new CircularBufferFeatExtractionInference(null, 200);

		//start a new thread for reading audio stuff
		myQueuePopper = new MyQueuePopper(cirBuffer);

		Log.i(TAG, "Raw Recording:"+feature.getRaw());

		//If Raw audio recording Needed
		if(feature.getRaw())
		{ 
			fileStatus_b=true;
			payloadSize=0;
			try 
			{

				File folder = new File(fDirPath);
				boolean success = false;
				if(!folder.exists())
				{
					success = folder.mkdir();
				}         
				else
				{
					success=true;
				}

				if(success)
				{
					file = new File(fPath);
					fWriter = new RandomAccessFile(file, "rw");
					fWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
					fWriter.writeBytes("RIFF");
					fWriter.writeInt(0); // Final file size not known yet, write 0 
					fWriter.writeBytes("WAVE");
					fWriter.writeBytes("fmt ");
					fWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
					fWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
					fWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
					fWriter.writeInt(Integer.reverseBytes(frequency)); // Sample rate
					fWriter.writeInt(Integer.reverseBytes(frequency*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
					fWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
					fWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
					fWriter.writeBytes("data");
					fWriter.writeInt(0); // Data chunk size not known yet, write 0
					log("File initialized");

				}
				else
				{
					Log.e(TAG,"Error in creating Raw data Folder");
					fileStatus_b =  false;
				}
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				Log.i(TAG,"Ex in creating Raw data File :"+e);
				fileStatus_b =  false;
			}
		}

	}

	//Start Recording
	public void run()
	{
		start=SystemClock.elapsedRealtime();
		rcount=0;

		//partial
		now_str=now();
		partno=0;
		partcount=0;

		// Wait until we start recording…
		synchronized (mutex) 
		{
			while (!this.isRecording) 
			{
				try 
				{
					mutex.wait();
				} 
				catch (InterruptedException e) 
				{
					throw new IllegalStateException("Wait Interrupted!",e);
				}
			}
		}

		//Start Processing Thread
		myQueuePopper.start();
		log("qpopper strted");

		// File Stuff
		/*if (this.fileName == null) 
		{
			throw new IllegalStateException("filename is null");
		}
		BufferedOutputStream bufferedStreamInstance = null;
		if (fileName.exists()) 
		{
			fileName.delete();
		}

		try 
		{
			fileName.createNewFile();
		} 
		catch (IOException e) 
		{

			Log.i(TAG,"Exception creating file");
			throw new IllegalStateException("Cannot create file:"  + fileName.toString());
		}

		try 
		{
			bufferedStreamInstance = new BufferedOutputStream(new FileOutputStream(this.fileName));
		} 
		catch (FileNotFoundException e) 
		{

			Log.i(TAG,"Exception opening file");
			throw new IllegalStateException("Cannot Open File", e);
		}

		DataOutputStream dataOutputStreamInstance =	new DataOutputStream(bufferedStreamInstance);
		 */

		// We’re important…
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		// Allocate Recorder and Start Recording…
		bufferRead = 0;
		bufferSize= framePeriod * 100 * bSamples * nChannels / 8;

		if (bufferSize < AudioRecord.getMinBufferSize(frequency, channelConfig, audioFormat))
		{ 
			// Check to make sure buffer size is not smaller than the smallest allowed one 
			bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfig, audioFormat);
			framePeriod = bufferSize / ( 2 * bSamples * nChannels / 8 );
		}

		Log.v(TAG,"Setting FramPeriod to: " + framePeriod);

		recordInstance = new AudioRecord(
				MediaRecorder.AudioSource.MIC, frequency,
				channelConfig, audioFormat,
				bufferSize);

		//AudioRecord recordInstance= findAudioRecord();
		//		if(recordInstance == null)
		//		{
		//			Log.i(TAG,"AUDIO RECORS IS SILL NULL!!!");
		//		}


		tempBuffer = new short[framePeriod*bSamples/16*nChannels];

		try
		{
			int temp_i=0;
			while(recordInstance.getState()!=AudioRecord.STATE_INITIALIZED)
			{
				if(temp_i==20)
				{
					Log.w(TAG, "Audio Record failed to initialize even after 100 ms");
					closeFile();
					return;
				}
				Thread.sleep(5);
			}
			recordInstance.startRecording();
		}
		catch(Exception e)
		{
			Log.e(TAG,"Ex : "+e);
		}

		while (this.isRecording) 
		{
			// Are we paused?
			synchronized (mutex) 
			{
				if (this.isPaused) 
				{
					try 
					{
						mutex.wait(250);
					} 
					catch (InterruptedException e) 
					{
						throw new IllegalStateException("Wait() interrupted!",e);
					}
					continue;
				}
			}

			//tempBuffer_byte = new byte[tempBuffer.length*2];
			bufferRead = recordInstance.read(tempBuffer, 0, tempBuffer.length);			

			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) 
			{
				throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} 
			else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) 
			{
				throw new IllegalStateException("read() returned AudioRecord.ERROR_BAD_VALUE");
			} 
			else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) 
			{
				throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}

			try 
			{
				//put data in circular buffer for processing
				tempTimeStamp = System.currentTimeMillis();
				++sync_id_counter;
				rcount++;

				// if SKIP, Currently only process 1/3 frames
				timeRec+=(int)(FRAME_STEP*1000/frequency);
				if(!CONSTS.SKIP || (timeRec/1000)%CONSTS.SKIP_INT==0)
				{
					//AudioData temp_audioData = new AudioData(tempBuffer.clone(),tempTimeStamp,sync_id_counter%16384);
					cirBuffer.insert(tempBuffer,tempTimeStamp,sync_id_counter%16384);
					//					if(timeRec%1000<=16)
					//						log("Saving Frame:"+timeRec);
				}
				else
				{
					//					log("Skipping Frame:"+timeRec);
				}

				if(feature.raw && fileStatus_b)
				{
					for(int i=0;i<tempBuffer.length;i++)
					{
						fWriter.writeShort(Short.reverseBytes(tempBuffer[i])); // Write buffer to file
					}
					payloadSize += tempBuffer.length*2;
				}
				/*for (int idxBuffer = 0; idxBuffer < bufferRead; ++idxBuffer) 
				{
					dataOutputStreamInstance.writeShort(tempBuffer[idxBuffer]);
				}*/
			} 
			catch (Exception e) 
			{
				Log.e("AcousticAppService","Ex in writing to Raw data File :"+e.getMessage());
				fileStatus_b =  false;
				//throw new IllegalStateException("dataOutputStreamInstance.writeShort(curVal)");
			}

		}
		//Log.i(TAG,"in recorder STopeed");


		// Close resources…
		try 
		{
			recordInstance.stop();
			//bufferedStreamInstance.close();
			recordInstance.release();	
			closeFile();			
			log("Freeing Recorder resources");
		} 
		catch (Exception e) 
		{
			throw new IllegalStateException("Cannot close ");
		}


		int diff=(int)(SystemClock.elapsedRealtime()-start)/1000;

		try 
		{ 
			myQueuePopper.join();
			cirBuffer= null;
			myQueuePopper=null;
		} 
		catch (InterruptedException e) 
		{

			Log.e(TAG,"Exception aftr join");
			e.printStackTrace(); 
		}
		Log.i(TAG, "Time for recording:"+diff);
		Log.i(TAG, "Time for Processing:"+(int)(SystemClock.elapsedRealtime()-start)/1000);

		//Health
		mEditor.putInt("rTotal", mSettings.getInt("rTotal", 0)+1);
		if(procComplete)
		{
			mEditor.putInt("qTotal", mSettings.getInt("qTotal", 0)+1);
			mEditor.putFloat("lastProc", 1);
		}
		else
		{
			if(rcount!=0)
				mEditor.putFloat("lastProc", (float)qcount/rcount);
			else
				mEditor.putFloat("lastProc", -1);
		}
		mEditor.commit();

		if(!CONSTS.PARTIAL)
		{
			if(feature.getEnergy())
				jsonManager.writeObject(System.currentTimeMillis(), now(), diff, rcount, qcount, procComplete, FRAME_SIZE, "energy",  energy_arr);
			if(feature.getSpl())
				jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,FRAME_SIZE,"spl",  spl_arr);
			if(feature.getZcr())
			{
				jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,FRAME_SIZE,"zcr", zcr_arr);
				int percent=0;
				int vcount=0;
				int tcount=0;
				int voice_thresh = mSettings.getInt("calib", VOICE_THRESH);
				if(zcr_arr.size() == 0)
				{
					percent =0;
				}
				else if(zcr_arr.size() < 3)
				{
					for(int i=0;i<zcr_arr.size();i++)
					{
						if((Integer)(zcr_arr.get(i))<voice_thresh)
						{
							vcount++;
						}
						tcount++;
					}
					percent = (int)(vcount * 100 /tcount);
				}
				else
				{
					for(int j= 2; j < zcr_arr.size(); j++)
					{
						long avg=0;
						for(int i = 0; i<3; i ++)
						{
							avg+= (Integer)zcr_arr.get(j- i);
						}
						avg /=3;
						if(avg<voice_thresh)
						{
							vcount++;
						}
						tcount++;
					}
					percent = (int)(vcount * 100 /tcount);
				}
				mEditor.putInt("minute", percent);
				voice_arr.add(percent);
				//jsonManager.writeObject(System.currentTimeMillis(), now(),diff, "voice", voice_arr);
				//obj.addhistory(percent);
				mEditor.commit();

				//for()
			}
			if(feature.getAdv())
			{
				jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,FRAME_SIZE,"no_correlation_peak",  adv1_arr);
				jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,FRAME_SIZE,"max_correlation_peak_value",  adv2_arr);
				//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"max_correlation_peak_lag",  adv3_arr);
				//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"spectral_entropy",  adv4_arr);
				jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,FRAME_SIZE,"relative_spectral_entropy",  adv5_arr);
				//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"adv_energy",  adv6_arr);
				//jsonManager.writeObject(System.currentTimeMillis(), now(), diff, "correlation_peak_value_array",  adv7_arr);
				//jsonManager.writeObject(System.currentTimeMillis(), now(), diff, "correlation_peak_lag_array",  adv8_arr);
				audioFeatureExtractionDestroy();
			}
		}
		log("Recording Stopped in Recorder/writing JSON");
		jsonManager=null;
	}

	void closeFile()
	{
		//Log.i(TAG,"Close file calld");

		if(feature.getRaw())
		{
			try
			{
				if(fileStatus_b)
				{
					if(fWriter!=null)
					{
						fWriter.seek(4); // Write size to RIFF header
						fWriter.writeInt(Integer.reverseBytes(36+payloadSize));

						fWriter.seek(40); // Write size to Subchunk2Size field
						fWriter.writeInt(Integer.reverseBytes(payloadSize));
						Log.i(TAG,"Closing Raw data file");

						fWriter.close(); // Remove prepared file
						fWriter= null;
						file.renameTo(new File(fPath_new));
						obj.uploadRawData();
					}
				}
				else
				{
					if(fWriter!=null)
					{
						fWriter.close(); // Remove prepared file
						fWriter= null;
						file.delete();
					}
				}
			}
			catch(Exception e)
			{
				fWriter=null;
				Log.e(TAG,"Ex in creating Raw data File :"+e);
			}
			fileStatus_b =  false;
		}
	}

	String getfilename()
	{
		cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}

	public boolean isMounted()
	{
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	//	private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
	//	public AudioRecord findAudioRecord() 
	//	{
	//		for (int rate : mSampleRates) 
	//		{
	//			for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) 
	//			{
	//				for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) 
	//				{
	//					try 
	//					{
	//						Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
	//								+ channelConfig);
	//						int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
	//
	//						if (bufferSize != AudioRecord.ERROR_BAD_VALUE) 
	//						{
	//							// check if we can instantiate and have a success
	//							AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
	//							if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
	//								return recorder;
	//						}
	//					} catch (Exception e) 
	//					{
	//						Log.e(TAG, rate + "Exception, keep trying.",e);
	//					}
	//				}
	//			}
	//		}
	//		return null;
	//	}

	/*public void setFileName(File fileName) 
	{
		this.fileName = fileName;
	}

	public File getFileName() 
	{
		return fileName;
	}*/

	/**
	 * @param isRecording
	 *            the isRecording to set
	 */
	public void setRecording(boolean isRecording) 
	{
		synchronized (mutex) 
		{
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	/**
	 * @return the isRecording
	 */
	public boolean isRecording() 
	{
		synchronized (mutex) 
		{
			return isRecording;
		}
	}
	/**
	 * @param isPaused
	 *            the isPaused to set
	 */
	public void setPaused(boolean isPaused) 
	{
		synchronized (mutex) 
		{
			this.isPaused = isPaused;
		}
	}

	/**
	 * @return the isPaused
	 */
	public boolean isPaused() 
	{
		synchronized (mutex) 
		{
			return isPaused;
		}
	}

	public static String now() 
	{
		cal = Calendar.getInstance();
		return sdf.format(cal.getTime());
	}

	public class MyQueuePopper extends Thread 
	{
		CircularBufferFeatExtractionInference obj;
		private short[] audioFrame;	
		private AudioData audioFromQueueData;
		int audioZCR;
		int audioEnergy;
		int audioSpl;

		public MyQueuePopper(CircularBufferFeatExtractionInference obj)
		{
			this.obj=obj;
			qcount=0;

			audioFrame = new short[FRAME_SIZE];

			//initialize the first half with zeros
			for(int i=0; i < FRAME_STEP; i++)
				audioFrame[i] = 0;

			if(feature.getAdv())
			{
				audioFeatureExtractionInit();
			}
			//audioFeatureExtractionInit();
		}

		@Override
		public void run() 
		{
			log("qpopper run");
			qcount=0;

			Boolean startRec_b=false;
			Boolean stoppedOnce_b=false;

			while(!obj.emptyq() || isRecording()) 
			{
				if(startRec_b)
				{
					if(!(mSettings.getBoolean("record_status", true)))
						stoppedOnce_b=true;
				}
				if(isRecording())
					startRec_b=true;
				else
				{
					if(startRec_b)
					{
						//android.util.Log.w(TAG, "Checking if new rec started:"+stoppedOnce_b);
						//android.util.Log.w(TAG, "rec status:"+mSettings.getBoolean("record_status", false));
						if(mSettings.getBoolean("record_status", false) && stoppedOnce_b)
						{
							Log.w(TAG, "Terminating Processing thread since new Recording started");
							procComplete=false;
							return;
						}
					}
				}

				audioFromQueueData = (AudioData)(obj.deleteAndHandleData());
				qcount++;//=FRAME_STEP;

				System.arraycopy(audioFromQueueData.data, 0, audioFrame, FRAME_STEP, FRAME_STEP);

				//debugging audio
				//Log.i(TAG ,"Audio read " + audioFromQueueData.data.length);
				//Log.i(TAG,"Audio read " + audioFromQueueData.timestamp + " " + audioFromQueueData.data.length);
				//Log.i(TAG, "Audio window data:"+Arrays.toString(audioFromQueueData.data));


				partcount++;

				//compute energy 
				if(feature.getEnergy())
				{
					audioEnergy = energy(audioFrame);
					energy_arr.add(audioEnergy);
					//log("Audio energy : "+audioEnergy);
				}

				if(feature.getZcr())
				{
					audioZCR = zcr(audioFrame);
					zcr_arr.add(audioZCR);
					//log("Audio ZCR : "+audioZCR);
				}

				if(feature.getSpl())
				{
					audioSpl = spl(audioFrame);
					spl_arr.add(audioSpl);


					//Since committing to the shared preference is memory intensive and calls the garbage collector almost once
					//a second, we try to reduce the number of times the value is committed
					//we commit only if the UI is on AND
					//(There is a considerable difference from the previous value OR once a few seconds)
					if(mSettings.getBoolean("ui", true))
					{
						int temp_sp=mSettings.getInt("spl_value", -1);
						if((Math.abs(temp_sp-audioSpl)>5 && qcount%25==0)||(qcount%250==0))
						{
							if(Math.abs(temp_sp-audioSpl)>5)
								log("GC : commiting due to diff:"+Math.abs(temp_sp-audioSpl));
							else
								log("GC : commiting due timer");

							mEditor.putInt("spl_value", audioSpl);
							mEditor.commit();
						}
					}

					//log("Audio SPL : "+audioSpl);
				}

				//add Features


				if(feature.getAdv())
				{
					try
					{
						dataArray=features(audioFrame);
						adv1_arr.add((int)dataArray[0]);
						adv2_arr.add(dataArray[1]);
						adv3_arr.add(dataArray[2]);
						adv4_arr.add(dataArray[3]);
						adv5_arr.add(dataArray[4]);
						adv6_arr.add(dataArray[5]);

						//temp7_arr=new double[(int) dataArray[0]];
						//System.arraycopy(dataArray, 6, temp7_arr, 0, (int)dataArray[0]);
						//adv7_arr.add(enclose(temp7_arr));
						//Log.i(TAG, temp7_arr[0]+"");

						//temp8_arr=new double[(int) dataArray[0]];
						//System.arraycopy(dataArray, 6+(int)dataArray[0], temp8_arr, 0, (int)dataArray[0]);
						//adv8_arr.add(enclose(temp8_arr));		


					}
					catch(Exception e)
					{
						Log.e(TAG, "Error in Parsing Advanced features");
					}
				}
				if(!CONSTS.BASIC)
				{
					if(zcr_arr.size()>=3)
					{
						long avg=0;
						for(int i = 0; i<3; i ++)
						{
							avg += (Integer) zcr_arr.get(zcr_arr.size()- i - 1);
						}
						avg /=3;
						int voice_thresh = mSettings.getInt("calib", VOICE_THRESH);

						if(avg>voice_thresh)
						{
							mEditor.putString("voicevalue","Not Speech ");
							//log("Non Voice");
						}
						else
						{
							mEditor.putString("voicevalue","Speech ");
							//log("Voice");
						}
						mEditor.commit();
					}
				}

				if(CONSTS.PARTIAL)
				{
					if(partcount%CONSTS.PARTIAL_COUNT==0)
					{
						Log.v("AcousticAppControl", "Upload, partno:"+partno+" ,count:"+energy_arr.size());
						if(feature.getEnergy())
						{
							jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, energy_arr.size(), FRAME_SIZE, "energy",  energy_arr);
							energy_arr.clear();
						}
						if(feature.getSpl())
						{
							jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, spl_arr.size(),FRAME_SIZE,"spl",  spl_arr);
							spl_arr.clear();
						}
						if(feature.getZcr())
						{
							jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno ,zcr_arr.size(),FRAME_SIZE,"zcr", zcr_arr);
							zcr_arr.clear();
						}
						if(feature.getAdv())
						{
							jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno ,adv1_arr.size(),FRAME_SIZE,"no_correlation_peak",  adv1_arr);
							jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, adv2_arr.size(),FRAME_SIZE,"max_correlation_peak_value",  adv2_arr);
							//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"max_correlation_peak_lag",  adv3_arr);
							//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"spectral_entropy",  adv4_arr);
							jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, adv5_arr.size(),FRAME_SIZE,"relative_spectral_entropy",  adv5_arr);
							//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"adv_energy",  adv6_arr);
							//jsonManager.writeObject(System.currentTimeMillis(), now(), diff, "correlation_peak_value_array",  adv7_arr);
							//jsonManager.writeObject(System.currentTimeMillis(), now(), diff, "correlation_peak_lag_array",  adv8_arr);
							adv1_arr.clear();
							adv2_arr.clear();
							adv5_arr.clear();
						}

						partno=partcount/CONSTS.PARTIAL_COUNT;
					}
				}


				//log("Ratio :  : "+ (double)audioEnergy/audioZCR);////////////////////////////
				//audioFeatureExtractionDestroy();

				//done for overlapping window
				System.arraycopy(audioFromQueueData.data, 0, audioFrame, 0, FRAME_STEP);
				//Log.i(TAG, "equeue:"+!obj.emptyq() +" isrec:"+ isRecording());
			}

			if(CONSTS.PARTIAL && (partno*CONSTS.PARTIAL_COUNT)<partcount)
			{
				Log.v("AcousticAppControl", "Sp Upload, partno:"+partno+" ,count:"+energy_arr.size());
				
				if(feature.getEnergy())
				{
					jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, energy_arr.size(), FRAME_SIZE, "energy",  energy_arr);
					energy_arr.clear();
				}
				if(feature.getSpl())
				{
					jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, spl_arr.size(),FRAME_SIZE,"spl",  spl_arr);
					spl_arr.clear();
				}
				if(feature.getZcr())
				{
					jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno ,zcr_arr.size(),FRAME_SIZE,"zcr", zcr_arr);
					zcr_arr.clear();
				}
				if(feature.getAdv())
				{
					jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno ,adv1_arr.size(),FRAME_SIZE,"no_correlation_peak",  adv1_arr);
					jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, adv2_arr.size(),FRAME_SIZE,"max_correlation_peak_value",  adv2_arr);
					//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"max_correlation_peak_lag",  adv3_arr);
					//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"spectral_entropy",  adv4_arr);
					jsonManager.writePartObject(System.currentTimeMillis(), now_str, partno, adv5_arr.size(),FRAME_SIZE,"relative_spectral_entropy",  adv5_arr);
					//jsonManager.writeObject(System.currentTimeMillis(), now(), diff,rcount, qcount, procComplete,"adv_energy",  adv6_arr);
					//jsonManager.writeObject(System.currentTimeMillis(), now(), diff, "correlation_peak_value_array",  adv7_arr);
					//jsonManager.writeObject(System.currentTimeMillis(), now(), diff, "correlation_peak_lag_array",  adv8_arr);
					adv1_arr.clear();
					adv2_arr.clear();
					adv5_arr.clear();
				}

				partno=partcount/CONSTS.PARTIAL_COUNT;
			}

		}

	}
	private String enclose(double arr[])
	{
		String op="(";
		for(int i=0;i<arr.length;i++)
		{
			op+=arr[i];
			if(i!=arr.length-1)
			{
				op+=",";
			}
		}
		op+=")";
		return op;
	}

	private int energy(short[] array)
	{
		long sum=0;
		for (int i = 0; i < array.length; i++) 
		{
			//Log.i(TAG, "audio : "+array[i]);
			sum += array[i]*array[i];
		}
		sum=(long)Math.sqrt(sum/array.length);
		return (int)sum;
	}

	private int zcr(short[] array)
	{
		long sum=0;
		for (int i = 1; i < array.length; i++) 
		{
			//Log.i(TAG, "audio : "+array[i]);
			sum += Math.abs(sign(array[i])-sign(array[i-1]));
		}
		sum= (sum*1000)/array.length;
		return (int)sum;
	}

	private int spl(short[] data)
	{
		final int bsize = 7;
		final int asize = 7;	
		int dataSize=data.length;

		if(filtered ==null || filtered.length!=(data.length + 8) )
			filtered = new double [data.length + 8];

		double finalanswer = 0.0;
		double finalanswer1 = 0.0;
		double printanswer = 0.0;
		int i = 0;
		int j = 0;
		int k = 0;

		double myzero = 0.0;
		double divmeby = (dataSize + myzero);

		// Filter
		for (i = 0; i < dataSize; i++) {
			filtered[i]=0;
			for (j = i, k = 0; (j >= 0 && k < bsize); j--, k++) {
				filtered[i] += spl_b[k] * (data[j] + myzero);
			}
			for (j = i-1, k = 1; (j >= 0 && k < asize); j--, k++) {
				filtered[i] -= spl_a[k] * filtered[j];
			}
		}

		// LEQ calculation approximatly from here:
		// http://digital.ni.com/public.nsf/allkb/FCE0EC0A6B193A028625722E006DE298
		// square, sum, divide all in double
		for (i = 0; i < dataSize; i++) {
			finalanswer = finalanswer + (((filtered[i] * filtered[i]))); // divmeby);
			finalanswer1 = finalanswer1 + (((data[i] * data[i]))); // divmeby);
		}

		finalanswer = finalanswer / divmeby;
		finalanswer1/= divmeby;
		finalanswer1 = Math.sqrt(finalanswer1);

		if (finalanswer != 0.0 && !Double.isNaN(finalanswer) && !Double.isInfinite(finalanswer)) {
			printanswer = Math.log10(finalanswer);
		}

		printanswer*=10.0;

		double P0 = 0.000002;
		double ans01 = 20 * Math.log10(finalanswer1 / P0)-80;
		double ans02 = 20 * Math.log10(finalanswer1);

		//log("ans-online	:"+ans01);
		//log("ans-naive	:"+ans02);
		//log("ans-martin	:"+printanswer);

		return (int)(printanswer);
	}

	public int sign(short inp)
	{
		if(inp>0)
			return 1;
		else if (inp==0)
			return 0;
		else
			return -1;
	}



	public class JsonManager
	{
		String appName;
		String version;
		String userName;
		JSONObject object;	

		JsonManager(String appName_inp, String version_inp, String userName_inp)
		{
			appName=appName_inp;
			version=version_inp;
			userName=userName_inp;
		}

		public JSONObject writeObject(long timestamp, String currentTime, int diff, int rcount_inp, int qcount_inp, Boolean procComplete_inp,int frameSize_inp, String featureName, ArrayList arr_inp) 
		{
			object = new JSONObject();
			synchronized(this)
			{
				try 
				{
					object.put("appname", appName);
					object.put("version", version);
					object.put("user", userName);
					object.put("timestamp", new Long(timestamp));
					object.put("duration", new Long(diff));
					object.put("date", currentTime);
					object.put("recCount",	rcount_inp);
					object.put("procCount", qcount_inp);
					object.put("procComplete", procComplete_inp);
					object.put("frameSize", frameSize_inp);
					object.put("feature", featureName);
					object.put("data", arr_inp);

					if(CONSTS.COMMIT)
						Log.i(DATA_TAG,""+object.toString());
					//log(object+"");
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
			return object;
		}

		public JSONObject writePartObject(long timestamp, String currentTime, int part,int count, int frameSize_inp, String featureName, ArrayList arr_inp) 
		{
			object = new JSONObject();
			synchronized(this)
			{
				try 
				{
					object.put("appname", appName);
					object.put("version", version);
					object.put("user", userName);
					object.put("timestamp", new Long(timestamp));
					object.put("part", new Integer(part));
					object.put("count", new Integer(count));
					object.put("date", currentTime);
					object.put("frameSize", frameSize_inp);
					object.put("feature", featureName);
					object.put("data", arr_inp);

					if(CONSTS.COMMIT)
						Log.i(DATA_TAG,""+object.toString());
					//log(object+"");
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
			return object;
		}

	}

	public void log(String inp)
	{
		if(CONSTS.DEBUG)
			android.util.Log.i(TAG,inp+"");
	}
}
