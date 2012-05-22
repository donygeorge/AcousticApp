package edu.ucla.cens.acousticapp;

//class to store each frame of Audio
public class AudioData
{
	public short data[];
	public long timestamp;
	public int sync_id;
	public int FRAME_STEP=128;
	public AudioData()
	{
		data = new short[FRAME_STEP];	//default 512 bytes
	}
	public AudioData(short[] data, long timestamp,int sync_id)
	{
		//System.arraycopy(data, 0, this.data, 0, FRAME_STEP);
		//Log.i(TAG, "Count recorded:"+count*16);
		this.data=data;			
		this.timestamp = timestamp;
		this.sync_id = sync_id;
	}
	
	public void insert(short[] data_inp, long timestamp_inp,int sync_id_inp)
	{
		if(data != null && data.length==data_inp.length)
		{
			System.arraycopy(data_inp, 0, data, 0, data_inp.length);
		}
		else
		{
			data = data_inp.clone();
		}
		timestamp =  timestamp_inp;
		sync_id = sync_id_inp;
	}
}