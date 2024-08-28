package com.ericsson.lmf.np;

import java.util.HashMap;
import java.util.Map;

public class MeasurementDataWrapper {
	private Map<String, String> internalData;
	  
	public MeasurementDataWrapper() {
		this.internalData = new HashMap<String, String>();
	}
	  
	public MeasurementDataWrapper(Map<String, String> data) {
	    this.internalData = new HashMap<String, String>();
	    addData(data);
	}
	  
	public void addData(Map<String, String> data) {
	    if (data != null)
	      for (String key : data.keySet())
	        this.internalData.put(key, data.get(key));  
	}
	  
	public void addData(String key, String value) {
	    this.internalData.put(key, value);
	}
	  
	public Map<String, String> getData() {
	    return this.internalData;
	}
}
