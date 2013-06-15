package org.rfcx.src_android;

import java.util.UUID;

import org.rfcx.src_api.ApiComm;
import org.rfcx.src_api.ApiCommIntentService;
import org.rfcx.src_audio.AudioCaptureService;
import org.rfcx.src_audio.AudioProcessService;
import org.rfcx.src_audio.AudioState;
import org.rfcx.src_database.DeviceStateDb;
import org.rfcx.src_database.SmsDb;
import org.rfcx.src_device.AirplaneMode;
import org.rfcx.src_device.DeviceState;
import org.rfcx.src_device.DeviceStateService;
import org.rfcx.src_monitor.MonitorIntentService;
import org.rfcx.src_receiver.AirplaneModeReceiver;
import org.rfcx.src_receiver.ConnectivityReceiver;
import org.rfcx.src_util.DeviceCpuUsage;
import org.rfcx.src_util.FactoryDeviceUuid;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class RfcxSource extends Application implements OnSharedPreferenceChangeListener {
	
	public static final String VERSION = "0.1.3";
	
	private static final String TAG = RfcxSource.class.getSimpleName();
	public boolean verboseLogging = false;
	
	private boolean lowPowerMode = false;
	
	private SharedPreferences sharedPreferences;
	Context context;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = new DeviceStateDb(this);
	public SmsDb smsDb = new SmsDb(this);

	// for obtaining device stats and characteristics
	private UUID deviceId = null;
	public DeviceState deviceState = new DeviceState();
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	
	// for viewing and controlling airplane mode
	public AirplaneMode airplaneMode = new AirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	
	// for transmitting api data
	public ApiComm apiComm = new ApiComm();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	// for analyzing captured audio
	public AudioState audioState = new AudioState();
		
	// android service running flags
	public boolean isServiceRunning_DeviceState = false;
	public boolean isServiceEnabled_DeviceState = true;
	
	public boolean isServiceRunning_AudioCapture = false;
	public boolean isServiceEnabled_AudioCapture = true;
	
	public boolean isServiceRunning_AudioProcess = false;
	public boolean isServiceEnabled_AudioProcess = true;
	
	public boolean isServiceRunning_ApiComm = false;
	public boolean isServiceEnabled_ApiComm = true;
	
	public boolean isServiceRunning_ServiceMonitor = false;
	
	public boolean ignoreOffHours = false;
	public int monitorIntentServiceInterval = 120;
	
	public int dayBeginsAt = 6;
	public int dayEndsAt = 19;
	
	@Override
	public void onCreate() {
		super.onCreate();
		airplaneMode.setOn(getApplicationContext());
		checkSetPreferences();
		setLowPowerMode(lowPowerMode);
		
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
		checkSetPreferences();
	}
	
	public void appPause() {
	}
	
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		checkSetPreferences();
	}
	
	private void checkSetPreferences() {
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);

		this.verboseLogging = this.sharedPreferences.getBoolean("verbose_logging", false);
		this.ignoreOffHours = this.sharedPreferences.getBoolean("ignore_off_hours", false);
		this.monitorIntentServiceInterval = Integer.parseInt(this.sharedPreferences.getString("monitor_intentservice_interval", "120"));
		airplaneMode.setAllowWifi(this.sharedPreferences.getBoolean("allow_wifi", false));
		apiComm.setApiDomain(this.sharedPreferences.getString("api_domain", "rfcx.org"));
		apiComm.setConnectivityInterval(Integer.parseInt(this.sharedPreferences.getString("api_interval", "300")));
		apiComm.setApiPort(Integer.parseInt(this.sharedPreferences.getString("api_port", "80")));
		apiComm.setApiEndpoint(this.sharedPreferences.getString("api_endpoint", "/api/1/checkin"));
		
		this.isServiceEnabled_AudioCapture = this.sharedPreferences.getBoolean("enable_service_audiocapture", true);
		this.isServiceEnabled_AudioProcess = this.sharedPreferences.getBoolean("enable_service_audioprocess", true);
		this.isServiceEnabled_DeviceState = this.sharedPreferences.getBoolean("enable_service_devicestate", true);
		this.isServiceEnabled_ApiComm = this.sharedPreferences.getBoolean("enable_service_apicomm", true);
		
//		this.dayBeginsAt = Integer.parseInt(this.sharedPreferences.getString("hour_day_begins", "6"));
//		this.dayEndsAt = Integer.parseInt(this.sharedPreferences.getString("hour_day_ends", "19"));
		
		if (this.verboseLogging) Log.d(TAG, "Preferences saved.");
	}
	
	public UUID getDeviceId() {
		if (deviceId == null) {
			FactoryDeviceUuid uuidFactory = new FactoryDeviceUuid(context, this.sharedPreferences);
			deviceId = uuidFactory.getDeviceUuid();
		}
		return deviceId;
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
		if (isServiceEnabled_AudioProcess && !isServiceRunning_AudioProcess) {
			context.startService(new Intent(context, AudioProcessService.class));
		} else if (isServiceRunning_AudioProcess && this.verboseLogging) {
			Log.d(TAG, "AudioProcessService already running. Not re-started...");
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
		if (isServiceEnabled_AudioProcess && isServiceRunning_AudioProcess) {
			context.stopService(new Intent(context, AudioProcessService.class));
		} else if (!isServiceRunning_AudioProcess && this.verboseLogging) {
			Log.d(TAG, "AudioProcessService not running. Not stopped...");
		}
	}
	
	public void setLowPowerMode(boolean lowPowerMode) {
		this.lowPowerMode = lowPowerMode;
		if (lowPowerMode) {
			deviceState.serviceSamplesPerMinute = 12;
		} else {
			deviceState.serviceSamplesPerMinute = 60;
		}
	}
	
	public boolean isInLowPowerMode() {
		return lowPowerMode;
	}
	
	
	public void launchAllIntentServices(Context context) {

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent apiCommServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ApiCommIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), apiComm.getConnectivityInterval()*1000, apiCommServiceIntent);
		
		PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, MonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), monitorIntentServiceInterval*1000, monitorServiceIntent);
	}

	
}
