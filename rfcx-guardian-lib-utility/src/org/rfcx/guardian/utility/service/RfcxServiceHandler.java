package org.rfcx.guardian.utility.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RfcxServiceHandler {

	public RfcxServiceHandler(Context context, String roleName) {
		this.context = context;
		this.logTag = "Rfcx-"+roleName+"-"+RfcxServiceHandler.class.getSimpleName();
	}	

	Context context;
	private String logTag = "Rfcx-Utils-"+RfcxServiceHandler.class.getSimpleName();

	private Map<String, Class<?>> svcClasses = new HashMap<String, Class<?>>();
	private Map<String, boolean[]> svcRunStates = new HashMap<String, boolean[]>();
	private Map<String, boolean[]> svcAbsoluteRunStates = new HashMap<String, boolean[]>();
	
	// svcLastActiveAt is not very well implemented yet... 
	// ...in that most services don't use/update this value
	// ...and it's not yet clear how it would be used in full
	private Map<String, long[]> svcLastActiveAt = new HashMap<String, long[]>();

	public void triggerService(String[] svcToTrigger, boolean forceReTrigger) {
		
		String svcName = svcToTrigger[0];
		String svcId = svcName.toLowerCase(Locale.US);

		if (!this.svcClasses.containsKey(svcId)) {
			
			Log.e(logTag, "There is no service named '"+svcName+"'.");
			
		} else if (!this.isRunning(svcName) || forceReTrigger) {
			try {
				// this means it's likely an intent service (rather than a service)
				if (svcToTrigger.length > 1) {
					
					String svcStart = svcToTrigger[1];
					String svcRepeat = svcToTrigger[2];
					
					long startTimeMillis = System.currentTimeMillis();
					if (	!svcStart.equals("0") 
						&& 	!svcStart.equalsIgnoreCase("now")
						) { try {
							startTimeMillis = (long) Long.parseLong(svcStart);
						} catch (Exception e) { RfcxLog.logExc(logTag, e); } 
					}
					
					long repeatIntervalMillis = 0;
					if (	!svcRepeat.equals("0") 
						&& 	!svcRepeat.equalsIgnoreCase("norepeat")
						) { try {
							repeatIntervalMillis = (long) Long.parseLong(svcRepeat);
						} catch (Exception e) { RfcxLog.logExc(logTag, e); } 
					}

					if (repeatIntervalMillis == 0) { 
						((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, startTimeMillis, PendingIntent.getService(this.context, -1, new Intent(context, svcClasses.get(svcId)), PendingIntent.FLAG_UPDATE_CURRENT));
						Log.i(logTag,"Scheduled IntentService '"+svcName+"' (begins at "+DateTimeUtils.getDateTime(startTimeMillis)+")");
					} else { 
						((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.RTC, startTimeMillis, repeatIntervalMillis, PendingIntent.getService(this.context, -1, new Intent(context, svcClasses.get(svcId)), PendingIntent.FLAG_UPDATE_CURRENT));
						// could also use setInexactRepeating() here instead, but this was sometimes appearing to lead to dropped events the first time around
						Log.i(logTag,"Scheduled Repeating IntentService '"+svcName+"' (begins at "+DateTimeUtils.getDateTime(startTimeMillis)+", repeats approx. every "+DateTimeUtils.milliSecondsAsMinutes(repeatIntervalMillis)+")");
					}

				// this means it's likely a service (rather than an intent service)
				} else if (svcToTrigger.length == 1) {
					
					this.context.stopService(new Intent(this.context, svcClasses.get(svcId)));
					this.context.startService(new Intent(this.context, svcClasses.get(svcId)));
					Log.i(logTag,"Triggered Service '"+svcName+"'");
					
				}
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		} else { 
//			Log.w(logTag, "Service '"+svcName+"' is already running...");
		}
	}
	
	public void triggerService(String svcToTrigger, boolean forceReTrigger) {
		triggerService(new String[] { svcToTrigger }, forceReTrigger);
	}
	
	public void triggerIntentService(String svcToTrigger, long startTimeMillis, long repeatIntervalMillis) {
		triggerService(new String[] { svcToTrigger, ""+startTimeMillis, ""+repeatIntervalMillis }, false);
	}
	
	public void triggerIntentServiceImmediately(String svcToTrigger) {
		triggerService(new String[] { svcToTrigger, "0", "0" }, false);
	}
	
	public void stopService(String svcToStop) {
		
		String svcId = svcToStop.toLowerCase(Locale.US);
		
		if (!this.svcClasses.containsKey(svcId)) {
			Log.e(logTag, "There is no service named '"+svcToStop+"'.");
		} else { 
			try {
				this.context.stopService(new Intent(this.context, svcClasses.get(svcId)));
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
	}	
	
	public void triggerServiceSequence(String sequenceName, String[] serviceSequenceSerialized, boolean forceReTrigger) {
		
		if (!hasRun(sequenceName.toLowerCase(Locale.US))) {
			
			Log.i(logTag, "Launching ServiceSequence '"+sequenceName+"'.");
			
			for (String serviceItemSerialized : serviceSequenceSerialized) {
				String[] serviceItem = new String[] { serviceItemSerialized };
				if (serviceItemSerialized.contains("|")) { serviceItem = serviceItemSerialized.split("\\|");  }
				triggerService(serviceItem, forceReTrigger);
			}		 
			
		} else {
			Log.w(logTag, "ServiceSequence '"+sequenceName+"' has already run.");
		}
	}
	
	// Getters and Setters
	
	public boolean isRunning(String svcName) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		
		if (this.svcRunStates.containsKey(svcId)) {
			try {
				return this.svcRunStates.get(svcId)[0];
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return false;
	}

	public boolean hasRun(String svcName) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		
		if (this.svcAbsoluteRunStates.containsKey(svcId)) {
			try {
				return this.svcAbsoluteRunStates.get(svcId)[0];
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return false;
	}
	
	public void setRunState(String svcName, boolean isRunning) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		this.svcRunStates.put(svcId, new boolean[] { isRunning } );
		if (isRunning) setAbsoluteRunState(svcName, true);
	}
	
	public void setAbsoluteRunState(String svcName, boolean hasRun) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		this.svcAbsoluteRunStates.put(svcId, new boolean[] { hasRun } );
	}
	
	public void setLastActiveAt(String svcName, long lastActiveAt) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		this.svcLastActiveAt.put(svcId, new long[] { lastActiveAt } );
	}
	
	public long getLastActiveAt(String svcName) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		if (this.svcLastActiveAt.containsKey(svcId)) {
			return this.svcLastActiveAt.get(svcId)[0];
		} else {
			return 0;
		}
	}
	
	public void addService(String svcName, Class<?> svcClass) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		this.svcClasses.put(svcId, svcClass);
		setRunState(svcName, false);
		setAbsoluteRunState(svcName, false);
	}
	
}
