package org.rfcx.guardian.api.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class ApiCheckInService extends Service {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+ApiCheckInService.class.getSimpleName();

	private boolean runFlag = false;
	private ApiCheckIn apiCheckIn;

	private RfcxGuardian app = null;
	private Context context = null;
	
	GZipUtils gZipUtils = new GZipUtils();
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckIn = new ApiCheckIn();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		
		app = (RfcxGuardian) getApplication();
		context = app.getApplicationContext();

		Log.v(TAG, "Starting service: "+TAG);
		
		app.isRunning_ApiCheckIn = true;
		try {
			this.apiCheckIn.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isRunning_ApiCheckIn = false;
		this.apiCheckIn.interrupt();
		this.apiCheckIn = null;
	}
	
	private class ApiCheckIn extends Thread {

		public ApiCheckIn() {
			super("ApiCheckInService-ApiCheckIn");
		}
		
		@Override
		public void run() {
			ApiCheckInService apiCheckInService = ApiCheckInService.this;
			app = (RfcxGuardian) getApplication();
			
			while (apiCheckInService.runFlag) {
				String[] currentCheckIn = new String[] {null,null,null};
				try {
					
					currentCheckIn = app.checkInDb.dbQueued.getLatestRow();	
										
					// only proceed with check in process if:
					if (	// 1) there is a pending check in in the database
							(currentCheckIn[0] != null)
							// 2) there is an active network connection
						&& 	app.isConnected
							// 3) the device internal battery percentage is at or above the minimum charge threshold
						&&	app.apiWebCheckIn.isBatteryChargeSufficientForCheckIn()
						) {
						
						List<String[]> stringParameters = new ArrayList<String[]>();
						stringParameters.add(new String[] { 
								"meta", 
								app.apiWebCheckIn.packagePreFlightCheckInJson(currentCheckIn[2]) 
							});
						
						if (((int) Integer.parseInt(currentCheckIn[3])) > app.apiWebCheckIn.maximumCheckInAttemptsBeforeSkip) {
							Log.d(TAG,"Skipping CheckIn "+currentCheckIn[1]+" after "+app.apiWebCheckIn.maximumCheckInAttemptsBeforeSkip+" failed attempts");
							app.checkInDb.dbSkipped.insert(currentCheckIn[0], currentCheckIn[1], currentCheckIn[2], currentCheckIn[3], currentCheckIn[4]);
							app.checkInDb.dbQueued.deleteSingleRowByAudioAttachmentId(currentCheckIn[1]);
						} else {
							app.apiWebCheckIn.sendCheckIn(
								app.apiWebCheckIn.getCheckInUrl(),
								stringParameters, 
								app.apiWebCheckIn.loadCheckInFiles(currentCheckIn[4]),
								true, // allow (or, if false, block) file attachments (audio/screenshots)
								currentCheckIn[1]
							);
						}
					} else {
						// wait 5 seconds before trying to check in again
						long checkInLoopDelay = 5000;
						Thread.sleep(checkInLoopDelay);
						
						if (!app.apiWebCheckIn.isBatteryChargeSufficientForCheckIn()) {
							long extendCheckInLoopBy = 2*app.apiWebCheckIn.audioCaptureInterval-checkInLoopDelay;
							Log.i(TAG, "CheckIns disabled due to low battery level"
									+" (current: "+app.deviceBattery.getBatteryChargePercentage(context, null)+"%, required: "+app.apiWebCheckIn.pauseCheckInsIfBatteryPercentageIsBelow+"%)."
									+" Waiting "+(Math.round((extendCheckInLoopBy+checkInLoopDelay)/1000))+" seconds before next attempt.");
							Thread.sleep(extendCheckInLoopBy);
						}
					}
						
				} catch (Exception e) {
					Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
					apiCheckInService.runFlag = false;
					app.isRunning_ApiCheckIn = false;
				}
			}
			
			apiCheckInService.runFlag = false;
			app.isRunning_ApiCheckIn = false;

		}
	}

}
