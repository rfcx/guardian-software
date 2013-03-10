package org.rfcx.src_android;

import java.util.UUID;

import org.rfcx.src_api.ApiComm;
import org.rfcx.src_api.ApiCommService;
import org.rfcx.src_api.ConnectivityReceiver;
import org.rfcx.src_audio.AudioCaptureService;
import org.rfcx.src_audio.AudioState;
import org.rfcx.src_database.AudioDb;
import org.rfcx.src_database.DeviceStateDb;
import org.rfcx.src_device.AirplaneMode;
import org.rfcx.src_device.AirplaneModeReceiver;
import org.rfcx.src_device.BatteryReceiver;
import org.rfcx.src_device.DeviceState;
import org.rfcx.src_device.DeviceStateService;
import org.rfcx.src_util.DeviceCpuUsage;
import org.rfcx.src_util.FactoryDeviceUuid;

import android.app.Application;
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
	
	private static final String TAG = RfcxSource.class.getSimpleName();
	private static final boolean LOG_VERBOSE = true;
	private SharedPreferences sharedPreferences;
	Context context;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = new DeviceStateDb(this);
	public AudioDb audioDb = new AudioDb(this);

	// for obtaining device stats and characteristics
	private UUID deviceId = null;
	public DeviceState deviceState = new DeviceState();
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	private final BroadcastReceiver batteryDeviceStateReceiver = new BatteryReceiver();
	
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
	public boolean isServiceRunning_ApiComm = false;
	public boolean isServiceRunning_AudioCapture = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onCreate()"); }
		
		checkSetPreferences();

	    this.registerReceiver(batteryDeviceStateReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onTerminate()"); }

		this.unregisterReceiver(batteryDeviceStateReceiver);
		this.unregisterReceiver(airplaneModeReceiver);
		this.unregisterReceiver(connectivityReceiver);
	}
	
	public void appResume() {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "appResume()"); }
		checkSetPreferences();
	}
	
	public void appPause() {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "appPause()"); }
	}
	
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onSharedPreferenceChanged()"); }
		checkSetPreferences();
	}
	
	private void checkSetPreferences() {
		Log.d(TAG, "checkSetPreferences()");
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		
		airplaneMode.setAllowWifi(this.sharedPreferences.getBoolean("allow_wifi", false));
		apiComm.setDomain(this.sharedPreferences.getString("api_domain", "api.rfcx.org"));
	}
	
	public UUID getDeviceId() {
		if (deviceId == null) {
			FactoryDeviceUuid uuidFactory = new FactoryDeviceUuid(context, this.sharedPreferences);
			deviceId = uuidFactory.getDeviceUuid();
		}
		return deviceId;
	}
	
	public void launchServices(Context context) {
		
		if (AudioState.isAudioEnabled() && !isServiceRunning_AudioCapture) {
			context.startService(new Intent(context, AudioCaptureService.class));
		} else if (isServiceRunning_AudioCapture) {
			Log.d(TAG, "AudioCaptureService already running. Not re-started...");
		}
		if (DeviceStateService.isDeviceStateEnabled() && !isServiceRunning_DeviceState) {
			context.startService(new Intent(context, DeviceStateService.class));
		} else if (isServiceRunning_DeviceState) {
			Log.d(TAG, "DeviceStateService already running. Not re-started...");
		}
		if (ApiComm.isApiCommEnabled() && !isServiceRunning_ApiComm) {
			context.startService(new Intent(context, ApiCommService.class));
		} else if (isServiceRunning_ApiComm) {
			Log.d(TAG, "ApiCommService already running. Not re-started...");
		}
	}
	
	public void suspendServices(Context context) {

		if (AudioState.isAudioEnabled() && isServiceRunning_AudioCapture) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else if (!isServiceRunning_AudioCapture) {
			Log.d(TAG, "AudioCaptureService not running. Not stopped...");
		}
		if (DeviceStateService.isDeviceStateEnabled() && isServiceRunning_DeviceState) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (!isServiceRunning_DeviceState) {
			Log.d(TAG, "DeviceStateService not running. Not stopped...");
		}
		if (ApiComm.isApiCommEnabled() && isServiceRunning_ApiComm) {
			context.stopService(new Intent(context, ApiCommService.class));
		} else if (!isServiceRunning_ApiComm) {
			Log.d(TAG, "ApiCommService not running. Not stopped...");
		}
	}
	
	
	public static boolean verboseLog() {
		return LOG_VERBOSE;
	}

		
}
