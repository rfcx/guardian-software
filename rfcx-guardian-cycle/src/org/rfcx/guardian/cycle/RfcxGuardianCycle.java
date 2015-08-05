package org.rfcx.guardian.cycle;

import org.rfcx.guardian.cycle.service.RebootIntentService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardianCycle extends Application implements OnSharedPreferenceChangeListener {

	private static final String TAG = "RfcxGuardianCycle-"+RfcxGuardianCycle.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	public String version;
	Context context;
	public boolean verboseLog = true;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String thisAppRole = "cycle";
	public String targetAppRole = "updater";
	
	private RfcxGuardianCyclePrefs rfcxGuardianCyclePrefs = new RfcxGuardianCyclePrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianCyclePrefs.createPrefs(this);
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		rfcxGuardianCyclePrefs.initializePrefs();
		rfcxGuardianCyclePrefs.checkAndSet(this);
		
		setAppVersion();
		
		(new ShellCommands()).executeCommandAsRoot("pm list features",null,getApplicationContext());
		
		initializeRoleIntentServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {
		rfcxGuardianCyclePrefs.checkAndSet(this);
	}
	
	public void appPause() {
	}
	
	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (this.verboseLog) { Log.d(TAG, "Preference changed: "+key); }
		rfcxGuardianCyclePrefs.checkAndSet(this);
	}
	
	private void setAppVersion() {
		try {
			this.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.trim();
			rfcxGuardianCyclePrefs.writeVersionToFile(this.version);
		} catch (NameNotFoundException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC);
		}
	}

	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
			if (this.verboseLog) { Log.d(TAG,"Device GUID: "+this.deviceId); }
			rfcxGuardianCyclePrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext(), this.sharedPrefs)).getDeviceToken();
			rfcxGuardianCyclePrefs.writeTokenToFile(deviceToken);
		}
		return this.deviceToken;
	}
	
	public void initializeRoleIntentServices(Context context) {
		try {
			
			// reboots system at 5 minutes before midnight every day
			PendingIntent rebootIntentService = PendingIntent.getService(context, -1, new Intent(context, RebootIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager rebootAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);		
			rebootAlarmManager.setRepeating(AlarmManager.RTC, (new DateTimeUtils()).nextOccurenceOf(23,55,0).getTimeInMillis(), 24*60*60*1000, rebootIntentService);
			
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	}
	
	public String getPref(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
    
}
