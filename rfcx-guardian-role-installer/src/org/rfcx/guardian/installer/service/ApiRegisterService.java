package org.rfcx.guardian.installer.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.utility.http.HttpPostMultipart;
import org.rfcx.guardian.utility.rfcx.RfcxConstants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ApiRegisterService extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiRegisterService.class.getSimpleName();

	private ApiRegister apiRegister;

	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiRegister = new ApiRegister();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		app.isRunning_ApiRegister = true;
		try {
			this.apiRegister.start();
			Log.d(TAG, "Starting service: "+TAG);
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_ApiRegister = false;
		this.apiRegister.interrupt();
		this.apiRegister = null;
	}
	
	private class ApiRegister extends Thread {

		public ApiRegister() {
			super("ApiRegisterService-ApiRegister");
		}

		@Override
		public void run() {
			ApiRegisterService apiRegisterService = ApiRegisterService.this;

			HttpPostMultipart httpPostMultipart = new HttpPostMultipart();
			// setting customized rfcx authentication headers (necessary for API access)
			List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
			rfcxAuthHeaders.add(new String[] { "x-auth-user", "register" });
			rfcxAuthHeaders.add(new String[] { "x-auth-token", app.rfcxPrefs.getPrefAsString("install_api_registration_token") });
			httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);

			try {
				if (app.isConnected) {
					if (app.apiCore.apiRegisterEndpoint != null) {
						String postUrl =	(((app.rfcxPrefs.getPrefAsString("api_url_base")!=null) ? app.rfcxPrefs.getPrefAsString("api_url_base") : "https://api.rfcx.org")
										+ app.apiCore.apiRegisterEndpoint
										);
						
						List<String[]> registrationParameters = new ArrayList<String[]>();
						registrationParameters.add(new String[] {  "guid", app.getDeviceId() });
						registrationParameters.add(new String[] {  "token", app.getDeviceToken() });
						
						String stringRegistrationResponse = httpPostMultipart.doMultipartPost(postUrl, registrationParameters, new ArrayList<String[]>());
						JSONArray jsonRegistrationResponse = new JSONArray(stringRegistrationResponse);
						
						Log.d(TAG, stringRegistrationResponse);
						
					} else {
						Log.d(TAG, "Cancelled because apiRegisterEndpoint is null...");
					}
				} else {
					Log.d(TAG, "Cancelled because there is no internet connectivity...");
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			} finally {
				app.isRunning_ApiRegister = false;
				app.stopService("ApiRegister");
			}
		}
	}

}
