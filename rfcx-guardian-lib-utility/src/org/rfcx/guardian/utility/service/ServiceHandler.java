package org.rfcx.guardian.utility.service;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceHandler {

	private static final String TAG = "Rfcx-Utils-"+ServiceHandler.class.getSimpleName();
	
	Context context;

	private Map<String, Class<?>> svcClasses = new HashMap<String, Class<?>>();
	private Map<String, boolean[]> svcRunStates = new HashMap<String, boolean[]>();
	private Map<String, boolean[]> svcAbsoluteRunStates = new HashMap<String, boolean[]>();

	public void triggerService(String svcName, boolean forceReTrigger) {
		
		if (!this.svcClasses.containsKey(svcName)) {
			Log.e(TAG, "There is no service named '"+svcName+"'.");
		} else if (!this.isRunning(svcName) || forceReTrigger) {
			this.context.stopService(new Intent(this.context, svcClasses.get(svcName)));
			this.context.startService(new Intent(this.context, svcClasses.get(svcName)));
			if (forceReTrigger) { Log.w(TAG,"Forced [re]trigger of service "+svcName); }
		} else { 
			Log.w(TAG, "Service '"+svcName+"' is already running...");
		}
	}
	
	public void stopService(String svcName) {
		
		if (!this.svcClasses.containsKey(svcName)) {
			Log.e(TAG, "There is no service named '"+svcName+"'.");
		} else { 
			this.context.stopService(new Intent(this.context, svcClasses.get(svcName)));
		}
	}	public void setServiceClass(String serviceName, Class<?> serviceClass) {
		this.svcClasses.remove(serviceName);
		this.svcClasses.put(serviceName, serviceClass);
		setRunState(serviceName, false);
	}
	
	// Getters and Setters

	public void setContext(Context context) {
		this.context = context;
	}
	
	public boolean isRunning(String svcName) {
		return this.svcRunStates.get(svcName)[0];
	}

	public boolean hasRun(String svcName) {
		return this.svcAbsoluteRunStates.get(svcName)[0];
	}
	
	public void setRunState(String svcName, boolean isRunning) {
		this.svcRunStates.remove(svcName);
		this.svcRunStates.put(svcName, new boolean[] { isRunning } );
	}
	
	public void setAbsoluteRunState(String svcName, boolean hasRun) {
		this.svcAbsoluteRunStates.remove(svcName);
		this.svcAbsoluteRunStates.put(svcName, new boolean[] { hasRun } );
	}
	
}
