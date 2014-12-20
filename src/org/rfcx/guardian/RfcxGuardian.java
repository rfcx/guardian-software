package org.rfcx.guardian;

import java.util.UUID;

import org.rfcx.guardian.api.ApiCheckIn;
import org.rfcx.guardian.api.ApiConnectIntentService;
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
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.DeviceUuid;
import org.rfcx.guardian.utility.HttpHttpsPostMultiPart;

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
	public String version;
	Context context;
	public boolean verboseLogging = false;
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	public String filesDir;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = new DeviceStateDb(this);
	public SmsDb smsDb = new SmsDb(this);
	public AlertDb alertDb = new AlertDb(this);
	public AudioDb audioDb = new AudioDb(this);

	// for obtaining device stats and characteristics
	private UUID deviceId = null;
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
	public boolean isServiceRunning_DeviceState = false;
	public boolean isServiceEnabled_DeviceState = true;
	
	public boolean isServiceRunning_AudioCapture = false;
	public boolean isServiceEnabled_AudioCapture = true;
	
	public boolean isServiceRunning_AudioProcess = false;
	public boolean isServiceEnabled_AudioProcess = true;
	
	public boolean isServiceRunning_AudioEncode = false;
	public boolean isServiceEnabled_AudioEncode = true;
	
	public boolean isServiceRunning_ApiComm = false;
	public boolean isServiceEnabled_ApiComm = true;
	
	public boolean isServiceRunning_ServiceMonitor = false;
	
	public boolean ignoreOffHours = false;
	public int monitorIntentServiceInterval = 180;
	
	public int dayBeginsAt = 9;
	public int dayEndsAt = 17;
	
	public HttpHttpsPostMultiPart httpPostMultiPart = new HttpHttpsPostMultiPart();
	
	public void testThePost() {
		Log.d(TAG, "not testing http multipart post");
//		httpPostMultiPart.executePostMultiPart(getApplicationContext());
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		setAppVersion();
		Log.d(TAG, "Launching org.rfcx.guardian (v"+version+")");
		airplaneMode.setOn(getApplicationContext());
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet();
		rfcxGuardianPrefs.loadPrefsOverride();
		setFilesDir();
		
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		launchAllIntentServices(getApplicationContext());
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		this.unregisterReceiver(airplaneModeReceiver);
		this.unregisterReceiver(connectivityReceiver);
	}
	
	public void appResume() {
		rfcxGuardianPrefs.checkAndSet();
	}
	
	public void appPause() {
	}
	
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		rfcxGuardianPrefs.checkAndSet();
	}
	
	public UUID getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = new DeviceUuid(getApplicationContext(), this.sharedPrefs).getDeviceUuid();
		}
		return this.deviceId;
	}
	
	private void setFilesDir() {
		this.filesDir = getApplicationContext().getFilesDir().getPath();
	}

	public void launchAllServices(Context context) {
		if (isServiceEnabled_DeviceState && !isServiceRunning_DeviceState) {
			context.startService(new Intent(context, DeviceStateService.class));
		} else if (isServiceRunning_DeviceState && this.verboseLogging) {
			Log.d(TAG, "DeviceStateService already running. Not re-started...");
		}
		if (isServiceEnabled_AudioCapture && !isServiceRunning_AudioCapture) {
			context.startService(new Intent(context, AudioCaptureService.class));
		} else if (isServiceRunning_AudioCapture && this.verboseLogging) {
			Log.d(TAG, "AudioCaptureService already running. Not re-started...");
		}
	}
	
	public void suspendAllServices(Context context) {
		if (isServiceEnabled_DeviceState && isServiceRunning_DeviceState) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (!isServiceRunning_DeviceState && this.verboseLogging) {
			Log.d(TAG, "DeviceStateService not running. Not stopped...");
		}
		if (isServiceEnabled_AudioCapture && isServiceRunning_AudioCapture) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else if (!isServiceRunning_AudioCapture && this.verboseLogging) {
			Log.d(TAG, "AudioCaptureService not running. Not stopped...");
		}
	}
	
	public void launchAllIntentServices(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent apiCommServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ApiConnectIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, MonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), apiCore.getConnectivityInterval()*1000, apiCommServiceIntent);
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), monitorIntentServiceInterval*1000, monitorServiceIntent);
	}
	
	private void setAppVersion() {
		try {
			this.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Failed to retrieve app version.");
			this.version = "0.0.0";
		}
	}
	
}
