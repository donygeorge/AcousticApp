package edu.ucla.cens.acousticapp;

public class CONSTS 
{
	//uploading raw data
	public static final Boolean UPLOAD =false;
	
	//Advanced Features hidden
	public static final Boolean BASIC =true;
	
	//Use Settings file
	public static final Boolean SETTINGFILE =false;
	
	//Used in Recorder to skip frames for Processing
	public static final Boolean SKIP =false;
	public static final int SKIP_INT = 3;
	
	//Should commit data to SystemLog?
	public static final Boolean COMMIT =true;
	
	//Used for debugging
	public static final Boolean DEBUG =false;
	
	//Battery Update to SystemLog
	public static final Boolean BATTERY =false;
	
	//Extended Sleep (eg:used to sleep for 30 minutes every 30 minutes- used for Case Study in the Lab)
	public static Boolean EXTENDED = false;
	public static int EXTENDED_SLEEP = 30;
	public static int EXTENDED_AWAKE = 30;
	
	
	//default
	public static final Boolean ENERGY_B=true;
	public static final Boolean ZCR_B=true;
	public static final Boolean RAW_B=false;
	public static final Boolean SPL_B=true;
	public static final Boolean ADV_B=true;
	
	public static final int INT_MIN=60;
	public static final int INT_MAX=60;
	public static final int INTERVAL = 600;
	
	//used for partial Json creation
	public static Boolean PARTIAL = true;
	public static int PARTIAL_COUNT =625; //10 seconds
}
