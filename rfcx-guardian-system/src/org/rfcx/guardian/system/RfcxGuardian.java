package org.rfcx.guardian.system;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import org.rfcx.guardian.system.database.DataTransferDb;
import org.rfcx.guardian.system.database.DeviceStateDb;
import org.rfcx.guardian.system.device.DeviceCpuUsage;
import org.rfcx.guardian.system.device.DeviceScreenShot;
import org.rfcx.guardian.system.device.DeviceState;
import org.rfcx.guardian.system.service.DeviceCPUTunerService;
import org.rfcx.guardian.system.service.DeviceScreenShotService;
import org.rfcx.guardian.system.service.DeviceStateService;
import org.rfcx.guardian.system.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.RfcxConstants;
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

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+RfcxGuardian.class.getSimpleName();

	public String version;
	Context context;
	public boolean verboseLog = true;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String thisAppRole = "system";
	public final String targetAppRole = "updater";
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);

	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	
	// database access helpers
	public DeviceStateDb deviceStateDb = null;
	public DataTransferDb dataTransferDb = null;

	// for obtaining device stats and characteristics
	public DeviceState deviceState = new DeviceState();
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	
	// Background Services
	public boolean isRunning_DeviceState = false;
	public boolean isRunning_CPUTuner = false;
	public boolean isRunning_DeviceScreenShot = false;
	
	// Repeating IntentServices
	public boolean isRunning_ServiceMonitor = false;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet(this);
		
		setAppVersion();
		setDbHandlers();
		
		(new ShellCommands()).triggerNeedForRootAccess(getApplicationContext());
		
		initializeRoleServices(getApplicationContext());
		
		
		
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {
		rfcxGuardianPrefs.checkAndSet(this);
	}
	
	public void appPause() {
	}
	
	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (this.verboseLog) { Log.d(TAG, "Preference changed: "+key); }
		rfcxGuardianPrefs.checkAndSet(this);
	}
	
	private void setAppVersion() {
		try {
			this.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.trim();
			rfcxGuardianPrefs.writeVersionToFile(this.version);
		} catch (NameNotFoundException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : RfcxConstants.NULL_EXC);
		}
	}
	
	public int getAppVersionValue(String versionName) {
		try {
			int majorVersion = (int) Integer.parseInt(versionName.substring(0, versionName.indexOf(".")));
			int subVersion = (int) Integer.parseInt(versionName.substring(1+versionName.indexOf("."), versionName.lastIndexOf(".")));
			int updateVersion = (int) Integer.parseInt(versionName.substring(1+versionName.lastIndexOf(".")));
			return 1000*majorVersion+100*subVersion+updateVersion;
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return 0;
	}

	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
			if (this.verboseLog) { Log.d(TAG,"Device GUID: "+this.deviceId); }
			rfcxGuardianPrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext(), this.sharedPrefs)).getDeviceToken();
			rfcxGuardianPrefs.writeTokenToFile(deviceToken);
		}
		return this.deviceToken;
	}
	
	public String getPref(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				
				// force CPUTuner Config (requires root access)
				triggerService("CPUTuner", true);
				// Service Monitor
				triggerIntentService("ServiceMonitor", 
						System.currentTimeMillis(),
						60*((int) Integer.parseInt(getPref("service_monitor_interval"))) );
				// background service for gather system stats
				triggerService("DeviceState", true);
				// background service for taking screenshots
				triggerService("ScreenShot", true);
				
				hasRun_OnLaunchServiceTrigger = true;
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
	}
	
	public void triggerIntentService(String intentServiceName, long startTimeMillis, int repeatIntervalSeconds) {
		Context context = getApplicationContext();
		if (startTimeMillis == 0) { startTimeMillis = System.currentTimeMillis(); }
		long repeatInterval = 300000;
		try { repeatInterval = repeatIntervalSeconds*1000; } catch (Exception e) { e.printStackTrace(); }
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if (intentServiceName.equals("ServiceMonitor")) {
			if (!this.isRunning_ServiceMonitor) {
				PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ServiceMonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				if (repeatIntervalSeconds == 0) { alarmManager.set(AlarmManager.RTC, startTimeMillis, monitorServiceIntent);
				} else { alarmManager.setInexactRepeating(AlarmManager.RTC, startTimeMillis, repeatInterval, monitorServiceIntent); }
			} else { Log.w(TAG, "Repeating IntentService 'ServiceMonitor' is already running..."); }
		} else {
			Log.w(TAG, "No IntentService named '"+intentServiceName+"'.");
		}
	}
	
	
	public void triggerService(String serviceName, boolean forceReTrigger) {
		context = getApplicationContext();
		if (forceReTrigger) Log.w(TAG,"Forcing [re]trigger of service "+serviceName);
		if (serviceName.equals("DeviceState")) {
			if (!this.isRunning_DeviceState || forceReTrigger) {
				context.stopService(new Intent(context, DeviceStateService.class));
				context.startService(new Intent(context, DeviceStateService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else if (serviceName.equals("CPUTuner")) {
			if (!this.isRunning_CPUTuner || forceReTrigger) {
				context.stopService(new Intent(context, DeviceCPUTunerService.class));
				context.startService(new Intent(context, DeviceCPUTunerService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else if (serviceName.equals("ScreenShot")) {
			if (!this.isRunning_DeviceScreenShot || forceReTrigger) {
				context.stopService(new Intent(context, DeviceScreenShotService.class));
				context.startService(new Intent(context, DeviceScreenShotService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else {
			Log.w(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("DeviceState")) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (serviceName.equals("CPUTuner")) {
			context.stopService(new Intent(context, DeviceCPUTunerService.class));
		} else if (serviceName.equals("ScreenShot")) {
			context.stopService(new Intent(context, DeviceScreenShotService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = getAppVersionValue(this.version);
		this.deviceStateDb = new DeviceStateDb(this,versionNumber);
		this.dataTransferDb = new DataTransferDb(this,versionNumber);
	}
 
	
}
