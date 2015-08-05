package org.rfcx.guardian.installer.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.utility.HttpGet;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ApiCheckVersionService extends Service {

	private static final String TAG = "Rfcx-Installer-"+ApiCheckVersionService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private ApiCheckVersion apiCheckVersion;

	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckVersion = new ApiCheckVersion();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		app.isRunning_ApiCheckVersion = true;
		try {
			this.apiCheckVersion.start();
			if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_ApiCheckVersion = false;
		this.apiCheckVersion.interrupt();
		this.apiCheckVersion = null;
	}
	
	private class ApiCheckVersion extends Thread {

		public ApiCheckVersion() {
			super("ApiCheckVersionService-ApiCheckVersion");
		}

		@Override
		public void run() {
			ApiCheckVersionService apiCheckVersionService = ApiCheckVersionService.this;

			HttpGet httpGet = new HttpGet();
			// setting customized rfcx authentication headers (necessary for API access)
			List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
			rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/"+app.getDeviceId() });
			rfcxAuthHeaders.add(new String[] { "x-auth-token", app.getDeviceToken() });
			httpGet.setCustomHttpHeaders(rfcxAuthHeaders);

			try {
				if (app.isConnected) {
					if (app.apiCore.apiCheckVersionEndpoint != null) {
						app.lastApiCheckTriggeredAt = Calendar.getInstance().getTimeInMillis();
						String getUrl =	(((app.getPref("api_domain")!=null) ? app.getPref("api_domain") : "https://api.rfcx.org")
										+ app.apiCore.apiCheckVersionEndpoint
										+ "?nocache="+Calendar.getInstance().getTimeInMillis());
						
						long sinceLastCheckIn = (Calendar.getInstance().getTimeInMillis() - app.apiCore.lastCheckInTime) / 1000;
						Log.d(TAG, "Since last checkin: "+sinceLastCheckIn);
						List<JSONObject> jsonResponse = httpGet.getAsJsonList(getUrl);
						if (app.verboseLog) { 
							for (JSONObject json : jsonResponse) {
								Log.d(TAG, json.toString());
							}
						}
						for (JSONObject jsonResponseItem : jsonResponse) {
							String appRole = jsonResponseItem.getString("role").toLowerCase();
							if (!appRole.equals(app.thisAppRole)) {
								app.targetAppRole = appRole;
								if (app.apiCore.apiCheckVersionFollowUp(app,appRole,jsonResponse)) {
									break;
								}
							}
						}
					} else {
						Log.d(TAG, "Cancelled because apiCheckVersionEndpoint is null...");
					}
				} else {
					Log.d(TAG, "Cancelled because there is no internet connectivity...");
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				app.isRunning_ApiCheckVersion = false;
				app.stopService("ApiCheckVersion");
			}
		}
	}

}
