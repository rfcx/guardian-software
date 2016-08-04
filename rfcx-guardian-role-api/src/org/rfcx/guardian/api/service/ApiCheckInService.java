package org.rfcx.guardian.api.service;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.api.api.ApiWebCheckIn;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCheckInService extends Service {

	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckInService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ApiCheckIn";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckIn serviceObject;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.serviceObject = new ApiCheckIn();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.serviceObject.start();
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
		this.serviceObject.interrupt();
		this.serviceObject = null;
	}
	
	private class ApiCheckIn extends Thread {

		public ApiCheckIn() {
			super("ApiCheckInService-ApiCheckIn");
		}
		
		@Override
		public void run() {
			ApiCheckInService serviceInstance = ApiCheckInService.this;
			
			app = (RfcxGuardian) getApplication();
			
			int prefsCheckInSkipThreshold = app.rfcxPrefs.getPrefAsInt("checkin_skip_threshold");
			long prefCheckInCyclePause = (long) app.rfcxPrefs.getPrefAsInt("checkin_cycle_pause");
			long prefsAudioCycleDuration = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int prefsCheckInBatteryCutoff = app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff");
			
			while (serviceInstance.runFlag) {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				try {
					
					String[] currentCheckIn = app.checkInDb.dbQueued.getLatestRow();	
										
					// only proceed with check in process if:
					if (	// 1) there is a pending check in in the database
							(currentCheckIn[0] != null)
							// 2) there is an active network connection
						&& 	app.deviceConnectivity.isConnected()
							// 3) the device internal battery percentage is at or above the minimum charge threshold
						&&	app.apiWebCheckIn.isBatteryChargeSufficientForCheckIn()
						) {
						
						List<String[]> stringParameters = new ArrayList<String[]>();
						stringParameters.add(new String[] { 
								"meta", 
								app.apiWebCheckIn.packagePreFlightCheckInJson(currentCheckIn[2]) 
							});
						
						if (((int) Integer.parseInt(currentCheckIn[3])) > prefsCheckInSkipThreshold) {
							Log.d(logTag,"Skipping CheckIn "+currentCheckIn[1]+" after "+prefsCheckInSkipThreshold+" failed attempts");
							app.checkInDb.dbSkipped.insert(currentCheckIn[0], currentCheckIn[1], currentCheckIn[2], currentCheckIn[3], currentCheckIn[4]);
							app.checkInDb.dbQueued.deleteSingleRowByAudioAttachmentId(currentCheckIn[1]);
						} else {
							List<String[]> checkInFiles = app.apiWebCheckIn.loadCheckInFiles(currentCheckIn[4]);
							if (ApiWebCheckIn.validateCheckInAttachments(checkInFiles)) {
								app.apiWebCheckIn.sendCheckIn(
									app.apiWebCheckIn.getCheckInUrl(),
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
						
						if (!app.apiWebCheckIn.isBatteryChargeSufficientForCheckIn()) {
							
							if (app.apiWebCheckIn.isBatteryChargedButBelowCheckInThreshold()) {
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
						
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					serviceInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			serviceInstance.runFlag = false;

		}
	}

}
