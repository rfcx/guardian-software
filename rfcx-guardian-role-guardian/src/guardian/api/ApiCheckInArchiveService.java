package guardian.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiCheckInArchiveService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInArchiveService.class);
	
	private static final String SERVICE_NAME = "ApiCheckInArchive";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckInArchive apiCheckInArchive;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInArchive = new ApiCheckInArchive();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckInArchive.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.apiCheckInArchive.interrupt();
		this.apiCheckInArchive = null;
//		Log.v(logTag, "Stopping service: "+logTag);
	}
	
	private class ApiCheckInArchive extends Thread {

		public ApiCheckInArchive() {
			super("ApiCheckInArchiveService-ApiCheckInArchive");
		}
		
		@Override
		public void run() {
			ApiCheckInArchiveService apiCheckInArchiveInstance = ApiCheckInArchiveService.this;
			
			app = (RfcxGuardian) getApplication();
				
			try {
				
//				while (!app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode") && (apiCheckInDb.dbQueued.getCount() > 0)) {
//
//					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
//					
//					long prefsAudioCycleDuration = (long) Math.round( app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * 1000 );
//					int prefsCheckInSkipThreshold = app.rfcxPrefs.getPrefAsInt("checkin_skip_threshold");
//					boolean prefsEnableBatteryCutoffs = app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_battery");
//					int prefsCheckInBatteryCutoff = app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff");
//					
//					if (!app.deviceConnectivity.isConnected()) {
//						Log.v(logTag, "No CheckIn because guardian currently has no connectivity."
//							+" Waiting " + ( Math.round( ( prefsAudioCycleDuration / 2 ) / 1000 ) ) + " seconds before next attempt.");
//						Thread.sleep( prefsAudioCycleDuration / 2 );
//						
//					} else if (prefsEnableBatteryCutoffs && !app.apiCheckInUtils.isBatteryChargeSufficientForCheckIn()) {
//						Log.v(logTag, "CheckIns currently disabled due to low battery level"
//							+" (current: "+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)+"%, required: "+prefsCheckInBatteryCutoff+"%)."
//							+" Waiting " + ( Math.round( ( prefsAudioCycleDuration * 2 ) / 1000 ) ) + " seconds before next attempt.");
//						Thread.sleep( prefsAudioCycleDuration * 2 );
//						
//						// reboots guardian in situations where battery charge percentage doesn't reflect charge state
//						if (app.apiCheckInUtils.isBatteryChargedButBelowCheckInThreshold()) {
//							app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getApplicationContext().getContentResolver());
//						}
//						
//					} else {
//						
//						// grab only most recently queued checkin
//						for (String[] latestQueuedCheckIn : apiCheckInDb.dbQueued.getLatestRowsWithLimit(1) ) {
//							
//							if (latestQueuedCheckIn[0] != null) {
//								
//								if (((int) Integer.parseInt(latestQueuedCheckIn[3])) > prefsCheckInSkipThreshold) {
//									
//									Log.d(logTag,"Skipping CheckIn "+latestQueuedCheckIn[1]+" after "+prefsCheckInSkipThreshold+" failed attempts");
//									apiCheckInDb.dbSkipped.insert(latestQueuedCheckIn[0], latestQueuedCheckIn[1], latestQueuedCheckIn[2], latestQueuedCheckIn[3], latestQueuedCheckIn[4]);
//									apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(latestQueuedCheckIn[1]);
//									
//								} else if ((new File(latestQueuedCheckIn[4])).exists()) {
//									
//									// MQTT
//									app.apiCheckInUtils.sendMqttCheckIn(latestQueuedCheckIn);
//									
//									// HTTP
//									//app.apiCheckInUtils.prepAndSendHttpCheckIn(latestQueuedCheckIn);
//								}
//								
//							} else {
//								
//								Log.d(logTag, "Queued checkin entry in database was invalid.");
//							}
//						}
//					}
//					
//					Thread.sleep(500);
//				}
			
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				
			} finally {
				apiCheckInArchiveInstance.runFlag = false;
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
			
			apiCheckInArchiveInstance.runFlag = false;
			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
	}

}
