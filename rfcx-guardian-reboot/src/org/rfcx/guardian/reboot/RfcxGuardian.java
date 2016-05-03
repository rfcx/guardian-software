package org.rfcx.guardian.reboot;

import org.rfcx.guardian.reboot.database.RebootDb;
import org.rfcx.guardian.reboot.service.RebootIntentService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceGuid;
import org.rfcx.guardian.utility.device.DeviceToken;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.RfcxPrefs;
import org.rfcx.guardian.utility.RfcxRoleVersions;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String APP_ROLE = "Reboot";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxPrefs rfcxPrefs = null;
	
	// database access helpers
	public RebootDb rebootDb = null;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {

		super.onCreate();
		
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), this.APP_ROLE);
		
		this.version = RfcxRoleVersions.getAppVersion(getApplicationContext());
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
	
	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.APP_ROLE, "installer")).getDeviceId();
			rfcxPrefs.writeGuidToFile(this.deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext())).getDeviceToken();
		}
		return this.deviceToken;
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				// reboots system at 5 minutes before midnight every day
				PendingIntent rebootIntentService = PendingIntent.getService(context, -1, new Intent(context, RebootIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				AlarmManager rebootAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);		
				rebootAlarmManager.setRepeating(AlarmManager.RTC, (new DateTimeUtils()).nextOccurenceOf(23,55,0).getTimeInMillis(), 24*60*60*1000, rebootIntentService);
				this.hasRun_OnLaunchServiceTrigger = true;
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRoleVersions.getAppVersionValue(this.version);
		this.rebootDb = new RebootDb(this,versionNumber);
	}
    
}
