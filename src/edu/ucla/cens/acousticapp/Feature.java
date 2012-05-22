package edu.ucla.cens.acousticapp;

public class Feature 
{
	public enum Feature_type 
	{
		ENERGY,
		ZCR,
		SPL,
		RAW	
	}
	
	Feature_type feature;
	
	Boolean energy;
	Boolean zcr;
	Boolean raw;
	Boolean spl;
	Boolean adv;
	
	Feature()
	{
		energy=false;
		zcr=false;
		raw=false;
		spl =false;
		adv=false;
	}
	
	void setEnergy(Boolean b)
	{
		energy=b;
	}
	Boolean getEnergy()
	{
		return energy;
	}
	void setZcr(Boolean b)
	{
		zcr=b;
	}
	Boolean getZcr()
	{
		return zcr;
	}
	void setRaw(Boolean b)
	{
		raw=b;
	}
	Boolean getRaw()
	{
		return raw;
	}
	void setSpl(Boolean b)
	{
		spl=b;
	}
	Boolean getSpl()
	{
		return spl;
	}
	void setAdv(Boolean b)
	{
		adv=b;
	}
	Boolean getAdv()
	{
		return adv;
	}
}
