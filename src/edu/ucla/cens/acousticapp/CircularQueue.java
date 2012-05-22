package edu.ucla.cens.acousticapp;

import java.text.DecimalFormat;

import android.util.Log;


public class CircularQueue
{
    private static final String TAG = "AcousticAppService";
    
    private static final boolean VERBOSE = false;

    private int[] data;
    private int mLastValue;
    private int mSize;
    private int mHead;
    private DecimalFormat mDF;

    public CircularQueue(int size)
    {
        mSize = size;
        mHead = -1;
        mLastValue = 0;
        data = new int[mSize];

        for (int i = 0; i < mSize; i++)
            data[i] = -1;

        mDF = new DecimalFormat();
        mDF.setMaximumFractionDigits(3);



    }

    /**
     * Adds the difference between the given value and the previously
     * added value to the circular queue. 
     *
     * @param       value           new value
     */
    public void add(int value)
    {

//        if (mLastValue == -1)
//        {
//            mLastValue = value;
//            return;
//        }

//        int curValue;
        mHead = (mHead + 1) % mSize;

//        curValue = value - mLastValue;
        data[mHead] = value;
//        mLastValue = value;

    }

    public void display(String inp)
    {
    	String op=inp+":  head: "+mHead + "  Data: ";
    	for(int i=0;i<data.length;i++)
    	{
    		op+= data[i] + ", ";
    	}
    	Log.i(TAG,op);
    }

    
    
    
    /**
     * Returns the sum of all the values in the queue.
     *
     * @return          current sum of the queue
     */
    public int getSum()
    {
        int sum = 0;

        if(mHead<0)
        	return -1;
        
        for (int i = 0; i < mSize; i++)
        {
        	if(data[i]<0)
        		return -1;
            sum += data[i];
        }
        return sum;

    }
    
    public int[] getArray()
    {
    	return data;
    }
    
    public void loadValues(int[] inp_arr)
    {
    	data = inp_arr;
    }

    /**
     * Returns the size of the queue.
     *
     * @return          size of the queue
     */
    public int getSize()
    {
        return mSize;
    }

}
