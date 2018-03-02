package guardian.api.checkin;

import java.util.ArrayList;
import java.util.List;

import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiCheckInJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInJobService.class);
	
	private static final String SERVICE_NAME = "ApiCheckInJob";
	
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
		return START_STICKY;
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
				
			try {
				
				while (		!app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode")
						&&	(app.apiCheckInDb.dbQueued.getCount() > 0)
					) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					long prefsAudioCycleDuration = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
					int prefsCheckInSkipThreshold = app.rfcxPrefs.getPrefAsInt("checkin_skip_threshold");
					boolean prefsEnableBatteryCutoffs = app.rfcxPrefs.getPrefAsBoolean("battery_cutoffs_enabled");
					int prefsCheckInBatteryCutoff = app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff");
					
					if (!app.deviceConnectivity.isConnected()) {
						Log.v(logTag, "No CheckIn because guardian currently has no connectivity."
							+" Waiting " + ( Math.round( ( prefsAudioCycleDuration / 4 ) / 1000 ) ) + " seconds before next attempt.");
						Thread.sleep( prefsAudioCycleDuration / 4 );
						
					} else if (prefsEnableBatteryCutoffs && !app.apiCheckInUtils.isBatteryChargeSufficientForCheckIn()) {
						Log.v(logTag, "CheckIns currently disabled due to low battery level"
							+" (current: "+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)+"%, required: "+prefsCheckInBatteryCutoff+"%)."
							+" Waiting " + ( Math.round( ( prefsAudioCycleDuration * 2 ) / 1000 ) ) + " seconds before next attempt.");
						Thread.sleep( prefsAudioCycleDuration * 2 );
						
						// reboots guardian in situations where battery charge percentage doesn't reflect charge state
						if (app.apiCheckInUtils.isBatteryChargedButBelowCheckInThreshold()) {
							app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getApplicationContext().getContentResolver());
						}
						
					} else {
							
						String[] latestQueuedCheckIn = app.apiCheckInDb.dbQueued.getLatestRow();	
						
						// only proceed with checkin process if there is a queued checkin in the database
						if (latestQueuedCheckIn[0] != null) {
							
							if (((int) Integer.parseInt(latestQueuedCheckIn[3])) > prefsCheckInSkipThreshold) {
								
								Log.d(logTag,"Skipping CheckIn "+latestQueuedCheckIn[1]+" after "+prefsCheckInSkipThreshold+" failed attempts");
								app.apiCheckInDb.dbSkipped.insert(latestQueuedCheckIn[0], latestQueuedCheckIn[1], latestQueuedCheckIn[2], latestQueuedCheckIn[3], latestQueuedCheckIn[4]);
								app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(latestQueuedCheckIn[1]);
								
							} else {
								
								// MQTT
								app.apiCheckInUtils.sendMqttCheckIn(latestQueuedCheckIn);
								Thread.sleep(500);
								
								// HTTP
//									List<String[]> stringParameters = new ArrayList<String[]>();
//									stringParameters.add(new String[] { "meta", app.apiCheckInUtils.packageHttpCheckInJson(latestQueuedCheckIn[2]) });
//									List<String[]> checkInFiles = app.apiCheckInUtils.loadHttpCheckInFiles(latestQueuedCheckIn[4]);
//									
//									if (ApiCheckInUtils.validateHttpCheckInAttachments(checkInFiles)) {
//										app.apiCheckInUtils.sendHttpCheckIn(
//											app.apiCheckInUtils.getHttpCheckInUrl(),
//											stringParameters, 
//											checkInFiles,
//											true, // allow (or, if false, block) file attachments (audio/screenshots)
//											latestQueuedCheckIn[1]
//										);	
//									}
								
							}
						} else {
							Log.d(logTag, "Queued checkin entry in database was invalid.");
							
						}
						
					}
				}
			
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				
			} finally {
				apiCheckInJobInstance.runFlag = false;
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
			
			if (app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode")) { Log.v(logTag, "No CheckIn because offline mode is on"); }
			
			apiCheckInJobInstance.runFlag = false;
			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
	}

}
