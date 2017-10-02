package org.rfcx.guardian.api.checkin;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

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
	}
	
	private class ApiCheckInJob extends Thread {

		public ApiCheckInJob() {
			super("ApiCheckInJobService-ApiCheckInJob");
		}
		
		@Override
		public void run() {
			ApiCheckInJobService apiCheckInJobInstance = ApiCheckInJobService.this;
			
			app = (RfcxGuardian) getApplication();
			
			int prefsCheckInSkipThreshold = app.rfcxPrefs.getPrefAsInt("checkin_skip_threshold");
			long prefCheckInCyclePause = (long) app.rfcxPrefs.getPrefAsInt("checkin_cycle_pause");
			long prefsAudioCycleDuration = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int prefsCheckInBatteryCutoff = app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff");
			
			while (apiCheckInJobInstance.runFlag) {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				try {
					
					if (app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode")) {
						
						Log.v(logTag, "No CheckIn because offline mode is on");
						Thread.sleep(3*prefCheckInCyclePause);
						
					} else {
					
						String[] currentCheckIn = app.apiCheckInDb.dbQueued.getLatestRow();	
											
						// only proceed with check in process if:
						if (		// 1) there is a pending check in in the database
								(currentCheckIn[0] != null)
								// 2) there is an active network connection
							&& 	app.deviceConnectivity.isConnected()
								// 3) the device internal battery percentage is at or above the minimum charge threshold
							&&	app.apiCheckInUtils.isBatteryChargeSufficientForCheckIn()
							) {
							
							List<String[]> stringParameters = new ArrayList<String[]>();
							stringParameters.add(new String[] { 
									"meta", 
									app.apiCheckInUtils.packagePreFlightCheckInJson(currentCheckIn[2]) 
								});
							
							if (((int) Integer.parseInt(currentCheckIn[3])) > prefsCheckInSkipThreshold) {
								Log.d(logTag,"Skipping CheckIn "+currentCheckIn[1]+" after "+prefsCheckInSkipThreshold+" failed attempts");
								app.apiCheckInDb.dbSkipped.insert(currentCheckIn[0], currentCheckIn[1], currentCheckIn[2], currentCheckIn[3], currentCheckIn[4]);
								app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(currentCheckIn[1]);
							} else {
								List<String[]> checkInFiles = app.apiCheckInUtils.loadCheckInFiles(currentCheckIn[4]);
								if (ApiCheckInUtils.validateCheckInAttachments(checkInFiles)) {
									app.apiCheckInUtils.sendCheckIn(
										app.apiCheckInUtils.getCheckInUrl(),
										stringParameters, 
										checkInFiles,
										true, // allow (or, if false, block) file attachments (audio/screenshots)
										currentCheckIn[1]
									);	
								}
							}
						} else {
					
							// force a [brief] pause seconds before trying to check in again
							Thread.sleep(prefCheckInCyclePause);
							
							if (!app.apiCheckInUtils.isBatteryChargeSufficientForCheckIn()) {
								
								if (app.apiCheckInUtils.isBatteryChargedButBelowCheckInThreshold()) {
									// THIS NEEDS TO BE REVIEWED
									ShellCommands.executeCommand("reboot", null, false, app.getApplicationContext());
								}
								
								long extendCheckInLoopBy = (2 * prefsAudioCycleDuration) - prefCheckInCyclePause;
								Log.i(logTag, "CheckIns automatically disabled due to low battery level"
										+" (current: "+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)+"%, required: "+prefsCheckInBatteryCutoff+"%)."
										+" Waiting " + ( Math.round( ( extendCheckInLoopBy + prefCheckInCyclePause ) / 1000 ) ) + " seconds before next attempt.");
								Thread.sleep(extendCheckInLoopBy);
							}
							
						}
					}
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					apiCheckInJobInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			apiCheckInJobInstance.runFlag = false;

		}
	}

}
