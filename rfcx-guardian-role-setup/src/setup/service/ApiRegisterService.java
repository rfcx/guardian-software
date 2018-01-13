package setup.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.rfcx.guardian.utility.http.HttpPostMultipart;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import setup.RfcxGuardian;

public class ApiRegisterService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiRegisterService.class);
	
	private static final String SERVICE_NAME = "ApiRegister";

	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiRegister apiRegister;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiRegister = new ApiRegister();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiRegister.start();
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
			rfcxAuthHeaders.add(new String[] { "x-auth-token", app.getPref("install_api_registration_token") });
			httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);

			try {
				if (app.deviceConnectivity.isConnected()) {
					if (app.apiCore.apiRegisterEndpoint != null) {
						String postUrl =	(((app.getPref("api_url_base")!=null) ? app.getPref("api_url_base") : "https://api.rfcx.org")
										+ app.apiCore.apiRegisterEndpoint
										);
						
						List<String[]> registrationParameters = new ArrayList<String[]>();
						registrationParameters.add(new String[] {  "guid", app.getDeviceGuid() });
						registrationParameters.add(new String[] {  "token", app.rfcxDeviceGuid.getDeviceToken() });
						
						String stringRegistrationResponse = httpPostMultipart.doMultipartPost(postUrl, registrationParameters, new ArrayList<String[]>());
						JSONArray jsonRegistrationResponse = new JSONArray(stringRegistrationResponse);
						
						Log.d(logTag, stringRegistrationResponse);
						
					} else {
						Log.d(logTag, "Cancelled because apiRegisterEndpoint is null...");
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
