package org.rfcx.guardian.api.service;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.HttpPostMultipart;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ApiCheckInService extends Service {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+ApiCheckInService.class.getSimpleName();

	private boolean runFlag = false;
	private ApiCheckIn apiCheckIn;

	private RfcxGuardian app = null;
	private Context context = null;
	
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
					
					if ((currentCheckIn[0] != null) && app.isConnected) {
						
						List<String[]> stringParameters = new ArrayList<String[]>();
						stringParameters.add(new String[] { "json", currentCheckIn[2] });
						stringParameters.add(new String[] { "audio", "" });
						stringParameters.add(new String[] { "screenshots", "" });
						stringParameters.add(new String[] { "messages", app.apiWebCheckIn.getMessagesAsJson() });
						
						if (((int) Integer.parseInt(currentCheckIn[3])) > app.apiWebCheckIn.MAX_CHECKIN_ATTEMPTS) {
							Log.d(TAG,"Skipping CheckIn "+currentCheckIn[1]+" after "+app.apiWebCheckIn.MAX_CHECKIN_ATTEMPTS+" failed attempts");
							app.checkInDb.dbSkipped.insert(currentCheckIn[0], currentCheckIn[1], currentCheckIn[2], currentCheckIn[3], currentCheckIn[4]);
							app.checkInDb.dbQueued.deleteSingleRowByAudioAttachment(currentCheckIn[1]);
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
						Thread.sleep(5000);
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
