package org.rfcx.guardian.guardian.api.methods.checkin;

import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
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
//		Log.v(logTag, "Starting service: "+logTag);
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
			long lastCheckInEndTime = System.currentTimeMillis();
			String lastCheckInId = null;
				
			while (		apiCheckInJobInstance.runFlag
					&&	!app.apiCheckInHealthUtils.isApiCheckInDisabled(true)
					&& 	( (app.apiCheckInDb.dbQueued.getCount() > 0) || !app.apiMqttUtils.isConnectedToBroker() )
				) {

				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				try {
						
					long prefsAudioCycleDuration = Math.round( app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * 1000 );
					int prefsCheckInFailureLimit = app.rfcxPrefs.getPrefAsInt("checkin_failure_limit");
					
					if (!app.apiCheckInHealthUtils.isApiCheckInAllowed(true, true)) {

						int waitLoopIterationCount = !app.deviceConnectivity.isConnected() ? 1 : 4;

						// This ensures that the service registers as active more frequently than the wait loop duration
						for (int waitLoopIteration = 0; waitLoopIteration < waitLoopIterationCount; waitLoopIteration++) {
							app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
							Thread.sleep( Math.round( prefsAudioCycleDuration / 2 ) );
						}

						if (app.deviceConnectivity.isConnected()) {
							app.apiMqttUtils.initializeFailedCheckInThresholds();
							app.apiMqttUtils.closeConnectionToBroker();
						}

//						// reboots org.rfcx.guardian.guardian in situations where battery charge percentage doesn't reflect charge state
//						if (app.apiCheckInUtils.isBatteryChargedButBelowCheckInThreshold()) {
//							app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getResolver());
//						}
						
					} else {
						
						// grab only most recently queued checkin
						for (String[] latestQueuedCheckIn : app.apiCheckInDb.dbQueued.getLatestRowsWithLimit(1) ) {
							
							if (latestQueuedCheckIn[0] != null) {

								if ((Integer.parseInt(latestQueuedCheckIn[3])) >= prefsCheckInFailureLimit) {
									
									Log.d(logTag,"Skipping CheckIn "+latestQueuedCheckIn[1]+" after "+prefsCheckInFailureLimit+" failed attempts");
									app.apiCheckInUtils.skipSingleCheckIn(latestQueuedCheckIn);

								} else if (!FileUtils.exists(latestQueuedCheckIn[4])) {
									
									Log.d(logTag,"Disqualifying CheckIn because audio file could not be found.");
									app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(latestQueuedCheckIn[1]);
									
								} else {

									// This conditional helps avoid double checkins that might occur before a checkin publication has been registered as complete
									if (	!latestQueuedCheckIn[1].equalsIgnoreCase(lastCheckInId)
										|| 	(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(lastCheckInEndTime) > 2000 )
									) {

										// Publish CheckIn to MQTT Broker
										app.apiMqttUtils.sendMqttCheckIn(latestQueuedCheckIn);
										lastCheckInEndTime = System.currentTimeMillis();

									} else {
										Thread.sleep(333);
									}

									lastCheckInId = latestQueuedCheckIn[1];
								}
								
							} else {
								
								Log.d(logTag, "Queued checkin entry in database was invalid.");
							}
						}

						if (!app.apiMqttUtils.isConnectedToBroker()) {
							long loopDelayBeforeReconnectAttempt = Math.round(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")/10);
							if (loopDelayBeforeReconnectAttempt < 8) { loopDelayBeforeReconnectAttempt = 8; }
							Log.e(logTag, "Broker not connected. Delaying "+loopDelayBeforeReconnectAttempt+" seconds and trying again...");
							Thread.sleep(loopDelayBeforeReconnectAttempt*1000);
							app.apiMqttUtils.confirmOrCreateConnectionToBroker(true);
						}

					}
					
					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
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
