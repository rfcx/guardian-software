package org.rfcx.guardian.utility.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RfcxServiceHandler {

	public RfcxServiceHandler(Context context, String appRole) {
		this.context = context;
		this.logTag = RfcxLog.generateLogTag(appRole, RfcxServiceHandler.class);
	}	

	Context context;
	private String logTag = RfcxLog.generateLogTag("Utils", RfcxServiceHandler.class);

	private Map<String, Class<?>> svcClasses = new HashMap<String, Class<?>>();
	private Map<String, boolean[]> svcRunStates = new HashMap<String, boolean[]>();
	private Map<String, boolean[]> svcAbsoluteRunStates = new HashMap<String, boolean[]>();
	
	// ...in that most services don't use/update this value yet
	// ...and it's not yet clear how it would be used in full
	private Map<String, long[]> svcLastReportedActiveAt = new HashMap<String, long[]>();

	public static String intentServiceTags(boolean isNotificationTag, String appRole, String svcName) {
		return (new StringBuilder())
				.append("org.rfcx.guardian.")
				.append(appRole.toLowerCase(Locale.US))
				.append( isNotificationTag ? ".RECEIVE_" : "." )
				.append(svcName.toUpperCase(Locale.US))
				.append( isNotificationTag ? "_NOTIFICATIONS" : "" )
				.toString();
	}
	
	public void triggerService(String[] svcToTrigger, boolean forceReTrigger) {
		
		String svcName = svcToTrigger[0];
		String svcId = svcName.toLowerCase(Locale.US);

		if (!this.svcClasses.containsKey(svcId)) {
			
			Log.e(logTag, (new StringBuilder()).append("There is no service named '").append(svcName).append("'.").toString());
			
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
						Log.i(logTag, (new StringBuilder()).append("Scheduled IntentService '").append(svcName).append("' (begins at ").append(DateTimeUtils.getDateTime(startTimeMillis)).append(")").toString());
					} else { 
						((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.RTC, startTimeMillis, repeatIntervalMillis, PendingIntent.getService(this.context, -1, new Intent(context, svcClasses.get(svcId)), PendingIntent.FLAG_UPDATE_CURRENT));
						// could also use setInexactRepeating() here instead, but this was appearing to lead to dropped events the first time around
						Log.i(logTag, (new StringBuilder()).append("Scheduled Repeating IntentService '").append(svcName).append("' (begins at ").append(DateTimeUtils.getDateTime(startTimeMillis)).append(", repeats approx. every ").append(DateTimeUtils.milliSecondDurationAsReadableString(repeatIntervalMillis)).append(")").toString());
					}

				// this means it's likely a service (rather than an intent service)
				} else if (svcToTrigger.length == 1) {
					
					this.context.stopService(new Intent(this.context, svcClasses.get(svcId)));
					this.context.startService(new Intent(this.context, svcClasses.get(svcId)));
					Log.i(logTag, (new StringBuilder()).append("Triggered Service '").append(svcName).append("'").toString());
					
				}
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		} else { 
//			Log.w(logTag, (new StringBuilder()).append("Service '").append(svcName).append("' is already running...").toString());
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
			Log.e(logTag, (new StringBuilder()).append("There is no service named '").append(svcToStop).append("'.").toString());
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
			
			Log.i(logTag, (new StringBuilder()).append("Launching ServiceSequence '").append(sequenceName).append("'.").toString());
			
			for (String serviceItemSerialized : serviceSequenceSerialized) {
				String[] serviceItem = new String[] { serviceItemSerialized };
				if (serviceItemSerialized.contains("|")) { serviceItem = serviceItemSerialized.split("\\|");  }
				triggerService(serviceItem, forceReTrigger);
			}		 
			
		} else {
			Log.w(logTag, (new StringBuilder()).append("ServiceSequence '").append(sequenceName).append("' has already run.").toString());
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
	
	public void reportAsActive(String svcName) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		this.svcLastReportedActiveAt.put(svcId, new long[] { System.currentTimeMillis() } );
	}
	
	public long getLastReportedActiveAt(String svcName) {
		
		String svcId = svcName.toLowerCase(Locale.US);
		if (this.svcLastReportedActiveAt.containsKey(svcId)) {
			return this.svcLastReportedActiveAt.get(svcId)[0];
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
	
	public void triggerOrForceReTriggerIfTimedOut(String svcName, long timeOutDuration) {

		long lastActiveAt = getLastReportedActiveAt(svcName);
		if ((lastActiveAt > 0) && ((System.currentTimeMillis() - lastActiveAt) > timeOutDuration)) {
			Log.e(logTag, (new StringBuilder()).append("Service '").append(svcName).append("' timed out... Forcing re-trigger...").toString());
			triggerService(svcName, true);
		} else {
			triggerService(svcName, false);
		}
	}
	
}
