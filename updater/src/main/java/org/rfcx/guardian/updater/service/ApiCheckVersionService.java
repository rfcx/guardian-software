package org.rfcx.guardian.updater.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.http.HttpGet;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCheckVersionService extends Service {

	private static final String SERVICE_NAME = "ApiCheckVersion";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckVersionService.class.getSimpleName());

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

			HttpGet httpGet = new HttpGet(app.getApplicationContext(),RfcxGuardian.APP_ROLE);
			// setting customized rfcx authentication headers (necessary for API access)
			List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
			rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/"+app.rfcxDeviceGuid.getDeviceGuid() });
			rfcxAuthHeaders.add(new String[] { "x-auth-token", app.rfcxDeviceGuid.getDeviceToken() });
			httpGet.setCustomHttpHeaders(rfcxAuthHeaders);

			try {
				if (app.deviceConnectivity.isConnected()) {
					if (app.apiCheckVersionUtils.apiCheckVersionEndpoint != null) {
						app.lastApiCheckTriggeredAt = System.currentTimeMillis();
						String getUrl =	app.rfcxPrefs.getPrefAsString("api_rest_protocol")
										+ "://"
										+ app.rfcxPrefs.getPrefAsString("api_rest_host")
										+ app.apiCheckVersionUtils.apiCheckVersionEndpoint
										+ "?role="+app.APP_ROLE.toLowerCase()
										+ "&version="+app.version
										+ "&battery="+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)
										+ "&timestamp="+System.currentTimeMillis()
										;
						
						long sinceLastCheckIn = (System.currentTimeMillis() - app.apiCheckVersionUtils.lastCheckInTime) / 1000;
						Log.d(logTag, "Since last checkin: "+sinceLastCheckIn);
						List<JSONObject> jsonResponse = httpGet.getAsJsonList(getUrl);
						for (JSONObject json : jsonResponse) {
							Log.d(logTag, json.toString());
						}
						for (JSONObject jsonResponseItem : jsonResponse) {
							String appRole = jsonResponseItem.getString("role").toLowerCase();
							if (!appRole.equals(RfcxGuardian.APP_ROLE)) {
								app.targetAppRole = appRole;
								if (app.apiCheckVersionUtils.apiCheckVersionFollowUp(app,appRole,jsonResponse)) {
									break;
								}
							}
						}
					} else {
						Log.d(logTag, "Cancelled because apiCheckVersionEndpoint is null...");
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
