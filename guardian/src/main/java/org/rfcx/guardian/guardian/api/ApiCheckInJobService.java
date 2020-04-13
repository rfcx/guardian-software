package org.rfcx.guardian.guardian.api;

import java.io.File;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiCheckInJobService extends Service {

	private static final String SERVICE_NAME = "ApiCheckInJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckInJob apiCheckInJob;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInJob = new ApiCheckInJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckInJob.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.apiCheckInJob.interrupt();
		this.apiCheckInJob = null;
//		Log.v(logTag, "Stopping service: "+logTag);
	}
	
	private class ApiCheckInJob extends Thread {

		public ApiCheckInJob() {
			super("ApiCheckInJobService-ApiCheckInJob");
		}
		
		@Override
		public void run() {
			ApiCheckInJobService apiCheckInJobInstance = ApiCheckInJobService.this;
			
			app = (RfcxGuardian) getApplication();
				
			while (		apiCheckInJobInstance.runFlag
					&&	app.rfcxPrefs.getPrefAsBoolean("enable_checkin_publish") 
					&& 	(app.apiCheckInDb.dbQueued.getCount() > 0)
				) {

				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				try {
						
					long prefsAudioCycleDuration = (long) Math.round( app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * 1000 );
					int prefsCheckInSkipThreshold = app.rfcxPrefs.getPrefAsInt("checkin_skip_threshold");
					boolean prefsEnableBatteryCutoffs = app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_battery");
					int prefsCheckInBatteryCutoff = app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff");
					
					if (!app.deviceConnectivity.isConnected()) {
						Log.v(logTag, "No CheckIn because org.rfcx.guardian.guardian currently has no connectivity."
							+" Waiting " + ( Math.round( ( prefsAudioCycleDuration / 2 ) / 1000 ) ) + " seconds before next attempt.");
						Thread.sleep( prefsAudioCycleDuration / 2 );
						
					} else if (prefsEnableBatteryCutoffs && !app.apiCheckInUtils.isBatteryChargeSufficientForCheckIn()) {
						Log.v(logTag, "CheckIns currently disabled due to low battery level"
							+" (current: "+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)+"%, required: "+prefsCheckInBatteryCutoff+"%)."
							+" Waiting " + ( Math.round( ( prefsAudioCycleDuration * 2 ) / 1000 ) ) + " seconds before next attempt.");
						Thread.sleep( prefsAudioCycleDuration * 2 );
						
						// reboots org.rfcx.guardian.guardian in situations where battery charge percentage doesn't reflect charge state
						if (app.apiCheckInUtils.isBatteryChargedButBelowCheckInThreshold()) {
							app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getApplicationContext().getContentResolver());
						}
						
					} else {
						
						// grab only most recently queued checkin
						for (String[] latestQueuedCheckIn : app.apiCheckInDb.dbQueued.getLatestRowsWithLimit(1) ) {
							
							if (latestQueuedCheckIn[0] != null) {
								
								if (((int) Integer.parseInt(latestQueuedCheckIn[3])) > prefsCheckInSkipThreshold) {
									
									Log.d(logTag,"Skipping CheckIn "+latestQueuedCheckIn[1]+" after "+prefsCheckInSkipThreshold+" failed attempts");
									app.apiCheckInDb.dbSkipped.insert(latestQueuedCheckIn[0], latestQueuedCheckIn[1], latestQueuedCheckIn[2], latestQueuedCheckIn[3], latestQueuedCheckIn[4]);
									app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(latestQueuedCheckIn[1]);
									
								} else if (!(new File(latestQueuedCheckIn[4])).exists()) {
									
									Log.d(logTag,"Disqualifying CheckIn because audio file could not be found.");
									app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(latestQueuedCheckIn[1]);
									
								} else {
									
									// Publish CheckIn to API
									app.apiCheckInUtils.sendMqttCheckIn(latestQueuedCheckIn);
								}
								
							} else {
								
								Log.d(logTag, "Queued checkin entry in database was invalid.");
							}
						}
					}
					
					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					// Putting this slight delay here between cycles does mitigate some check in preparation issues, 
					//	...but it's definitely a lame hack...
					// Hopefully we can improve this some time.
					Thread.sleep(500);
					
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					apiCheckInJobInstance.runFlag = false;
				}			
					
			}
			
			if (!app.rfcxPrefs.getPrefAsBoolean("enable_checkin_publish")) {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				Log.v(logTag, "CheckIn publication is explicitly disabled.");
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			apiCheckInJobInstance.runFlag = false;
		}
	}

}
