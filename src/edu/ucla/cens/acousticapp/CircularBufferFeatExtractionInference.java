package edu.ucla.cens.acousticapp;


import android.content.Context;
import android.util.Log;

public class CircularBufferFeatExtractionInference {
	private int qMaxSize;// max queue size
	private int fp = 0;  // front pointer
	private int rp = 0;  // rear pointer
	private int qs = 0;  // size of queue
	private AudioData[] q;    // actual queue
	//private static Ml_Toolkit_Application appState;
	private AudioData[] tempQ;
	
	int lastLogged=0;
	int icount=0;

	//thread to write in the database
	private Thread t; 

	private static final String TAG = "AcousticAppQueue";	

	@SuppressWarnings("unchecked")
	public CircularBufferFeatExtractionInference(Context context, int size) {
		qMaxSize = size;
		fp = 0;
		rp = 0;
		qs = 0;
		q = new AudioData[qMaxSize];
	}

	public AudioData delete() {
		if (!emptyq()) {
			//will not decrease size to avoid race condition
			qs--;
			fp = (fp + 1)%qMaxSize;
			
			if(CONSTS.DEBUG)
			{
				if(Math.abs(qs-lastLogged)>20)
				{
					lastLogged=qs;
					Log.d("AcousticApp","Size:"+qs+" ,max:"+qMaxSize+" icount:"+icount+" ,used memory:"+(qs*0.288)+"kb");
				}
			}
			//T ob=q[fp];
			//q[fp]=null;
			return q[fp];
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void insert(short[] data,long timestamp,int sync_id) { 
		//insert case; if the queue is full then we will increase size of the queue by a factor of 2
		if (!fullq()) {
			qs++;
			rp = (rp + 1)%qMaxSize;
			//System.arraycopy(tempQ, 0, q[rp], 0, tempQ.length);//q[rp]
			if(q[rp]==null)
			{
				q[rp]= new AudioData(data.clone(), timestamp, sync_id);
				icount++;
			}
			else
			{
				
				q[rp].insert(data,timestamp,sync_id);
			}
			notify(); 
			// start the delete thread and start copying because there is element
		}
		else
		{
			//since queue full, double Size
			synchronized(this)
			{
				int temp_rp=rp;
				int temp_fp=fp;
				int temp_qMaxSize=qMaxSize*2;
				tempQ = new AudioData[temp_qMaxSize];

				for(int i=0;i<qs;i++)
				{
					temp_fp = (temp_fp + 1)%qMaxSize;
					tempQ[i]= q[temp_fp];
				}
				temp_fp=temp_qMaxSize-1;
				temp_rp=qs-1;

				q=tempQ;
				fp=temp_fp;
				rp=temp_rp;
				qMaxSize=temp_qMaxSize;
				
				Log.w(TAG, "Queue size increase to:"+temp_qMaxSize);
				
				if (!fullq()) {
					qs++;
					rp = (rp + 1)%qMaxSize;
					q[rp] = new AudioData(data.clone(), timestamp, sync_id);
					icount++;
					notify(); 
				}
				
				//Clearing tempQ
				tempQ=null;

			}
		}
	}


	public synchronized AudioData deleteAndHandleData() {
		//means that buffer doesn't yet have appState.writeAfterThisManyValues elements so sleep
		synchronized(this)
		{
			if(emptyq())
			{
				try {
					//Log.d(TAG, "No data feature extraction thread going to sleep" );
					wait();
				} catch(InterruptedException e) {
					//System.out.println("InterruptedException caught");
				}
			}
			//means there is data now
			return delete();
		}

	}

	public boolean emptyq() {
		return qs == 0;
	}

	public boolean fullq() {
		return qs == qMaxSize;
	}

	public int getQSize() {
		return qs;
	}

	public void printq() {
		System.out.print("Size: " + qs +
				", rp: " + rp + ", fp: " + fp + ", q: ");
		for (int i = 0; i < qMaxSize; i++)
			System.out.print("q[" + i + "]=" 
					+ q[i] + "; ");
		System.out.println();
	}
}