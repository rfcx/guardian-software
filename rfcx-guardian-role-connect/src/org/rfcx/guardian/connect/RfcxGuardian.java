package org.rfcx.guardian.connect;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application {

	public String version;
	Context context;
	
	public static final String APP_ROLE = "Connect";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxDeviceId = (new RfcxDeviceId()).init(getApplicationContext());
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(getApplicationContext(), TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		
		initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				
				this.hasRun_OnLaunchServiceTrigger = true;
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			}
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version, TAG);
	}
	
}
