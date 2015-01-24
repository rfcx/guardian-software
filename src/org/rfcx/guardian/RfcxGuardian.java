package org.rfcx.guardian;

import java.util.Calendar;

import org.rfcx.guardian.api.ApiCore;
import org.rfcx.guardian.audio.AudioCore;
import org.rfcx.guardian.database.AlertDb;
import org.rfcx.guardian.database.AudioDb;
import org.rfcx.guardian.database.DeviceStateDb;
import org.rfcx.guardian.database.ScreenShotDb;
import org.rfcx.guardian.database.SmsDb;
import org.rfcx.guardian.device.AirplaneMode;
import org.rfcx.guardian.device.CpuUsage;
import org.rfcx.guardian.device.DeviceState;
import org.rfcx.guardian.intentservice.ApiCheckInTriggerIntentService;
import org.rfcx.guardian.intentservice.ServiceMonitorIntentService;
import org.rfcx.guardian.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.receiver.ConnectivityReceiver;
import org.rfcx.guardian.service.ApiCheckInService;
import org.rfcx.guardian.service.AudioCaptureService;
import org.rfcx.guardian.service.CarrierCodeService;
import org.rfcx.guardian.service.DeviceStateService;
import org.rfcx.guardian.telecom.CarrierInteraction;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceScreenShot;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.util.Log;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = RfcxGuardian.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	public String version = "0.0.0";
	public boolean verboseLog = false;
	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	private String deviceId = null;
	Context context;
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	// database access helpers
	public DeviceStateDb deviceStateDb = new DeviceStateDb(this);
	public SmsDb smsDb = new SmsDb(this);
	public AlertDb alertDb = new AlertDb(this);
	public AudioDb audioDb = new AudioDb(this);
	public ScreenShotDb screenShotDb = new ScreenShotDb(this);

	// for obtaining device stats and characteristics
	public DeviceState deviceState = new DeviceState();
	public CpuUsage deviceCpuUsage = new CpuUsage();
	
	// for viewing and controlling airplane mode
	public AirplaneMode airplaneMode = new AirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	// for transmitting api data
	public ApiCore apiCore = new ApiCore();
	
	// for handling captured audio
	public AudioCore audioCore = new AudioCore();
	
	// should services be disabled as if in a power emergency...
//	public boolean isCrisisModeEnabled = false;
//	public boolean ignoreOffHours = false;
//	public int monitorIntentServiceInterval = 180;
//	public int dayBeginsAt = 9;
//	public int dayEndsAt = 17;
		
	// Background Services
	public boolean isRunning_DeviceState = false;
	public boolean isRunning_AudioCapture = false;
	public boolean isRunning_ApiCheckIn = false;
	public boolean isRunning_CarrierCode = false;
	
	// Repeating IntentServices
	public boolean isRunning_ServiceMonitor = false;
	public boolean isRunning_ApiCheckInTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		setAppVersion();
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet(this);
		rfcxGuardianPrefs.loadPrefsOverride();
		Log.d(TAG, "Device GUID: "+getDeviceId());
		
//		(new DeviceScreenShot()).saveScreenShot(getApplicationContext());
		(new DeviceScreenShot()).checkModuleInstalled();
		
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	    
	    onBootServiceTrigger();
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		this.unregisterReceiver(airplaneModeReceiver);
		this.unregisterReceiver(connectivityReceiver);
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
			Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC);
		}
	}
	
	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
			if (this.verboseLog) { Log.d(TAG,"Device GUID: "+this.deviceId); }
			rfcxGuardianPrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}

	public String getPrefString(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	public int getPrefInt(String prefName) {
		return this.sharedPrefs.getInt(prefName, 0);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
	
	public void onBootServiceTrigger() {
		triggerIntentService("ServiceMonitor", 600);
//		triggerIntentService("ApiCheckInTrigger", getPref("api_checkin_interval"));
		triggerService("DeviceState", true);
		triggerService("AudioCapture", true);
	}
	
	public void triggerIntentService(String intentServiceName, int repeatIntervalSeconds) {
		Context context = getApplicationContext();
		long repeatInterval = 300000;
		try { repeatInterval = repeatIntervalSeconds*1000; } catch (Exception e) { e.printStackTrace(); }
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if (intentServiceName.equals("ServiceMonitor")) {
			if (!this.isRunning_ServiceMonitor) {
				PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ServiceMonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), repeatInterval, monitorServiceIntent);
			} else if (this.verboseLog) { Log.d(TAG, "Repeating IntentService 'ServiceMonitor' is already running..."); }
		} else if (intentServiceName.equals("ApiCheckInTrigger")) {
			if (!this.isRunning_ApiCheckInTrigger) {
				PendingIntent apiCheckInTrigger = PendingIntent.getService(context, -1, new Intent(context, ApiCheckInTriggerIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), repeatInterval, apiCheckInTrigger);
			} else if (this.verboseLog) { Log.d(TAG, "Repeating IntentService 'ApiCheckInTrigger' is already running..."); }
			
		} else {
			Log.e(TAG, "No IntentService named '"+intentServiceName+"'.");
		}
	}
	
	
	public void triggerService(String serviceName, boolean forceReTrigger) {
		context = getApplicationContext();
		boolean serviceAllowedInPrefs = this.sharedPrefs.getBoolean("enable_service_"+serviceName.toLowerCase(), true);
		if (serviceName.equals("AudioCapture")) {
			if (!this.isRunning_AudioCapture || forceReTrigger) {
				context.stopService(new Intent(context, AudioCaptureService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, AudioCaptureService.class));
			} else if (this.verboseLog) { Log.d(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.e(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("DeviceState")) {
			if (!this.isRunning_DeviceState || forceReTrigger) {
				context.stopService(new Intent(context, DeviceStateService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, DeviceStateService.class));
			} else if (this.verboseLog) { Log.d(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.e(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("ApiCheckIn")) {
			if (!this.isRunning_ApiCheckIn || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckInService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, ApiCheckInService.class));
			} else if (this.verboseLog) { Log.d(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.e(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("CarrierCode")) {
			if (!this.isRunning_CarrierCode || forceReTrigger) {
				context.stopService(new Intent(context, CarrierCodeService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, CarrierCodeService.class));
			} else if (this.verboseLog) { Log.d(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.e(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("AudioCapture")) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else if (serviceName.equals("DeviceState")) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (serviceName.equals("ApiCheckIn")) {
			context.stopService(new Intent(context, ApiCheckInService.class));
		} else if (serviceName.equals("CarrierCode")) {
			context.stopService(new Intent(context, CarrierCodeService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
}
