package org.rfcx.guardian.service;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.api.ApiCore;
import org.rfcx.guardian.utility.HttpPostMultipart;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ApiCheckInService extends Service {

	private static final String TAG = "RfcxGuardian-"+ApiCheckInService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

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

		if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		
		app.isRunning_ApiCheckIn = true;
		try {
			this.apiCheckIn.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
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
			
			try {
				while (apiCheckInService.runFlag) {
					String[] currentCheckIn = new String[] {null,null,null};
					try {
						currentCheckIn = app.checkInDb.dbQueued.getLatestRow();	
						List<String[]> stringParameters = new ArrayList<String[]>();
						stringParameters.add(new String[] { "json", currentCheckIn[2] });
						if ((currentCheckIn[0] != null) && app.isConnected) {
							if (((int) Integer.parseInt(currentCheckIn[3])) > app.apiCore.MAX_CHECKIN_ATTEMPTS) {
								Log.d(TAG,"Skipping CheckIn "+currentCheckIn[1]+" after "+app.apiCore.MAX_CHECKIN_ATTEMPTS+" failed attempts");
								app.checkInDb.dbSkipped.insert(currentCheckIn[0], currentCheckIn[1], currentCheckIn[2], currentCheckIn[3]);
								app.checkInDb.dbQueued.deleteSingleRow(currentCheckIn[1]);
							} else {
								app.apiCore.sendCheckIn(
									app.apiCore.getCheckInUrl(),
									stringParameters, 
									app.apiCore.loadCheckInFiles(currentCheckIn[1]),
									true, // allow (or block) file attachments (audio/screenshots)
									currentCheckIn[1]
								);
							}
						} else {
							Thread.sleep(5000);
						}
							
					} catch (Exception e) {
						Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
						apiCheckInService.runFlag = false;
						app.isRunning_ApiCheckIn = false;
					}
				}					
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
				apiCheckInService.runFlag = false;
				app.isRunning_ApiCheckIn = false;
			} finally {
				app.isRunning_ApiCheckIn = false;
			}
		}
	}

}
