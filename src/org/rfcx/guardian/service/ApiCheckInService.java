package org.rfcx.guardian.service;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
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
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		app.isRunning_ApiCheckIn = true;
		try {
			this.apiCheckIn.start();
			if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
			try {
				
				List<String[]> stringParameters = new ArrayList<String[]>();
				stringParameters.add(new String[] { "json", app.apiCore.getCheckInJson() });
			
				app.apiCore.queueCheckIn(
					app.apiCore.getCheckInUrl(),
					stringParameters, 
					app.apiCore.getCheckInFiles(),
					true // allow (or block) file attachments (audio/screenshots)
				);
					
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				app.isRunning_ApiCheckIn = false;
				app.stopService("ApiCheckIn");
			}
		}
	}

}
