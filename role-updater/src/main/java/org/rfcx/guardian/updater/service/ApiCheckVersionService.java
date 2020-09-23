package org.rfcx.guardian.updater.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCheckVersionService extends Service {

	private static final String SERVICE_NAME = "ApiCheckVersion";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckVersionService");

	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckVersion apiCheckVersion;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckVersion = new ApiCheckVersion();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckVersion.start();
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

			HttpGet httpGet = new HttpGet(app.getApplicationContext(), RfcxGuardian.APP_ROLE);
			// setting customized rfcx authentication headers (necessary for API access)
			List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
			rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/"+app.rfcxGuardianIdentity.getGuid() });
			rfcxAuthHeaders.add(new String[] { "x-auth-token", app.rfcxGuardianIdentity.getAuthToken() });
			httpGet.setCustomHttpHeaders(rfcxAuthHeaders);

			try {
				if (app.deviceConnectivity.isConnected()) {
					String getUrl =	app.rfcxPrefs.getPrefAsString("api_rest_protocol")
									+ "://"
									+ app.rfcxPrefs.getPrefAsString("api_rest_host")
									+"/v2/guardians/"+app.rfcxGuardianIdentity.getGuid()+"/software/all"
									+ "?role="+RfcxGuardian.APP_ROLE.toLowerCase()
									+ "&version="+ app.version
									+ "&battery="+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)
									+ "&timestamp="+System.currentTimeMillis()
									;

					Log.d(logTag, "Time elapsed since last update checkin: "+ DateTimeUtils.milliSecondDurationAsReadableString((System.currentTimeMillis() - app.apiCheckVersionUtils.lastCheckInTime)));

					List<JSONObject> jsonResponse = null;
					try {
						jsonResponse = httpGet.getAsJsonList(getUrl);
					} catch (Exception e) {
						RfcxLog.logExc(logTag, e);
					}

					if (jsonResponse == null) {
						Log.e(logTag, "Version check API request failed...");
						app.apiCheckVersionUtils.lastCheckInTriggered = 0;
					} else {
//							for (JSONObject jsonObj : jsonResponse) {
//								Log.d(logTag, jsonObj.toString());
//							}
						for (JSONObject jsonResponseItem : jsonResponse) {
							String targetAppRole = jsonResponseItem.getString("role").toLowerCase();
							if (!targetAppRole.equals(RfcxGuardian.APP_ROLE)) {
								if (app.apiCheckVersionUtils.apiCheckVersionFollowUp(targetAppRole, jsonResponse)) {
									break;
								}
							}
						}
					}
				} else {
					Log.d(logTag, "Cancelled because there is no internet connectivity...");
				}
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

}
