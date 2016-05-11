package org.rfcx.guardian.utility;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceHandler {

	private static final String TAG = "Rfcx-Utils-"+ServiceHandler.class.getSimpleName();
	Context context;

	private Map<String, Class<?>> serviceClasses = new HashMap<String, Class<?>>();
	private Map<String, boolean[]> serviceRunStates = new HashMap<String, boolean[]>();
	
	public ServiceHandler init(Context context) {
		this.context = context;
		return this;
	}
	
	public void setServiceClass(String serviceName, Class<?> serviceClass) {
		this.serviceClasses.remove(serviceName);
		this.serviceClasses.put(serviceName, serviceClass);
		setRunState(serviceName, false);
	}
	
	public boolean isRunning(String serviceName) {
		return this.serviceRunStates.get(serviceName)[0];
	}
	
	public void setRunState(String serviceName, boolean isRunning) {
		this.serviceRunStates.remove(serviceName);
		this.serviceRunStates.put(serviceName, new boolean[] { false } );
	}

	public void triggerService(String serviceName, boolean forceReTrigger) {

		if (forceReTrigger) { Log.w(TAG,"Forcing [re]trigger of service "+serviceName); }
		
		if (!this.serviceClasses.containsKey(serviceName)) {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		} else if (!this.isRunning(serviceName) || forceReTrigger) {
			this.context.stopService(new Intent(this.context, serviceClasses.get(serviceName)));
			this.context.startService(new Intent(this.context, serviceClasses.get(serviceName)));
		} else { 
			Log.w(TAG, "Service '"+serviceName+"' is already running...");
		}
	}
	
	public void stopService(String serviceName) {
		
		if (!this.serviceClasses.containsKey(serviceName)) {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		} else { 
			this.context.stopService(new Intent(this.context, serviceClasses.get(serviceName)));
		}
	}
	
}
