package org.rfcx.guardian;

import org.rfcx.guardian.api.ApiCheckIn;
import org.rfcx.guardian.api.ApiCore;
import org.rfcx.guardian.audio.AudioCaptureService;
import org.rfcx.guardian.audio.AudioCore;
import org.rfcx.guardian.database.AlertDb;
import org.rfcx.guardian.database.AudioDb;
import org.rfcx.guardian.database.DeviceStateDb;
import org.rfcx.guardian.database.SmsDb;
import org.rfcx.guardian.device.AirplaneMode;
import org.rfcx.guardian.device.CpuUsage;
import org.rfcx.guardian.device.DeviceState;
import org.rfcx.guardian.device.DeviceStateService;
import org.rfcx.guardian.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.receiver.ConnectivityReceiver;
import org.rfcx.guardian.service.MonitorIntentService;
import org.rfcx.guardian.utility.DeviceGuid;

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
	public String version;
	Context context;
	public boolean verboseLog = false;
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	public String filesDir;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = new DeviceStateDb(this);
	public SmsDb smsDb = new SmsDb(this);
	public AlertDb alertDb = new AlertDb(this);
	public AudioDb audioDb = new AudioDb(this);

	// for obtaining device stats and characteristics
	private String deviceId = null;
	public DeviceState deviceState = new DeviceState();
	public CpuUsage deviceCpuUsage = new CpuUsage();
	
	// for viewing and controlling airplane mode
	public AirplaneMode airplaneMode = new AirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	
	// for transmitting api data
//	public ApiComm apiComm = new ApiComm();
	public ApiCore apiCore = new ApiCore();
	public ApiCheckIn apiCheckIn = new ApiCheckIn();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	// for analyzing captured audio
	public AudioCore audioCore = new AudioCore();
	
	// should services be disabled as if in a power emergency...
	public boolean isCrisisModeEnabled = false;
		
	// android service running flags
	public boolean isRunning_DeviceState = false;
	public boolean isEnabled_DeviceState = true;
	
	public boolean isRunning_AudioCapture = false;
	public boolean isEnabled_AudioCapture = true;
	
	public boolean isRunning_ApiComm = false;
	public boolean isEnabled_ApiComm = true;
	
	public boolean isRunning_ServiceMonitor = false;
	
	public boolean ignoreOffHours = false;
	public int monitorIntentServiceInterval = 180;
	
	public int dayBeginsAt = 9;
	public int dayEndsAt = 17;
	
	@Override
	public void onCreate() {
		super.onCreate();
		setAppVersion();
		Log.d(TAG, "Launching org.rfcx.guardian (v"+version+")");
//		airplaneMode.setOn(getApplicationContext());
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet(this);
		rfcxGuardianPrefs.loadPrefsOverride();
		setFilesDir();
		
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		launchIntentServices(getApplicationContext());
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
	
	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
		}
		return this.deviceId;
	}
	
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
		rfcxGuardianPrefs.writeGuidToFile(deviceId);
	}
	
	private void setFilesDir() {
		this.filesDir = getApplicationContext().getFilesDir().getPath();
	}

	public void launchAllServices(Context context) {
		if (isEnabled_DeviceState && !isRunning_DeviceState) {
			context.startService(new Intent(context, DeviceStateService.class));
		} else if (isRunning_DeviceState && this.verboseLog) {
			Log.d(TAG, "DeviceStateService already running. Not re-started...");
		}
		if (isEnabled_AudioCapture && !isRunning_AudioCapture) {
			context.startService(new Intent(context, AudioCaptureService.class));
		} else if (isRunning_AudioCapture && this.verboseLog) {
			Log.d(TAG, "AudioCaptureService already running. Not re-started...");
		}
	}
	
	public void suspendAllServices(Context context) {
		if (isEnabled_DeviceState && isRunning_DeviceState) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (!isRunning_DeviceState && this.verboseLog) {
			Log.d(TAG, "DeviceStateService not running. Not stopped...");
		}
		if (isEnabled_AudioCapture && isRunning_AudioCapture) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else if (!isRunning_AudioCapture && this.verboseLog) {
			Log.d(TAG, "AudioCaptureService not running. Not stopped...");
		}
	}
	
	public void launchIntentServices(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
//		PendingIntent apiCommServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ApiConnectIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, MonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
//		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), apiCore.getConnectivityInterval()*1000, apiCommServiceIntent);
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), monitorIntentServiceInterval*1000, monitorServiceIntent);
	}
	
	private void setAppVersion() {
		this.version = "0.0.0";
		try { this.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC);
		}
	}
	
}
