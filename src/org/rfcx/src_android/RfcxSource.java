package org.rfcx.src_android;

import java.util.UUID;

import org.rfcx.src_api.ApiComm;
import org.rfcx.src_api.ApiCommService;
import org.rfcx.src_api.ConnectivityReceiver;
import org.rfcx.src_audio.AudioCaptureService;
import org.rfcx.src_audio.AudioProcessService;
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
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class RfcxSource extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = RfcxSource.class.getSimpleName();
	public static final boolean VERBOSE = true;
	
	private boolean lowPowerMode = false;
	
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
	
//	// device power management
//	PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//	PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RfcxWakeLock");
	
	// android service running flags
	public boolean isServiceRunning_DeviceState = false;
	public boolean isServiceRunning_AudioCapture = false;
	public boolean isServiceRunning_AudioProcess = false;
	
	public boolean areServicesHalted_ExpensiveServices = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		checkSetPreferences();
		setLowPowerMode(lowPowerMode);
		
	    this.registerReceiver(batteryDeviceStateReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();

		this.unregisterReceiver(batteryDeviceStateReceiver);
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
		
		airplaneMode.setAllowWifi(this.sharedPreferences.getBoolean("allow_wifi", false));
		apiComm.setApiDomain(this.sharedPreferences.getString("api_domain", "rfcx.org"));
		apiComm.setConnectivityInterval(Integer.parseInt(this.sharedPreferences.getString("api_interval", "300")));
		apiComm.setApiPort(Integer.parseInt(this.sharedPreferences.getString("api_port", "80")));
		apiComm.setApiEndpoint(this.sharedPreferences.getString("api_endpoint", "/api/1/checkin"));
		
		if (RfcxSource.VERBOSE) Log.d(TAG, "Preferences saved.");
	}
	
	public UUID getDeviceId() {
		if (deviceId == null) {
			FactoryDeviceUuid uuidFactory = new FactoryDeviceUuid(context, this.sharedPreferences);
			deviceId = uuidFactory.getDeviceUuid();
		}
		return deviceId;
	}
	
	public void launchAllServices(Context context) {
		
		if (DeviceState.SERVICE_ENABLED && !isServiceRunning_DeviceState) {
			context.startService(new Intent(context, DeviceStateService.class));
		} else if (isServiceRunning_DeviceState && RfcxSource.VERBOSE) {
			Log.d(TAG, "DeviceStateService already running. Not re-started...");
		}
		if (AudioState.CAPTURE_SERVICE_ENABLED && !isServiceRunning_AudioCapture) {
			context.startService(new Intent(context, AudioCaptureService.class));
		} else if (isServiceRunning_AudioCapture && RfcxSource.VERBOSE) {
			Log.d(TAG, "AudioCaptureService already running. Not re-started...");
		}
		if (AudioState.PROCESS_SERVICE_ENABLED && !isServiceRunning_AudioProcess) {
			context.startService(new Intent(context, AudioProcessService.class));
		} else if (isServiceRunning_AudioProcess && RfcxSource.VERBOSE) {
			Log.d(TAG, "AudioProcessService already running. Not re-started...");
		}
		areServicesHalted_ExpensiveServices = false;
	}
	
	public void suspendAllServices(Context context) {

		if (DeviceState.SERVICE_ENABLED && isServiceRunning_DeviceState) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (!isServiceRunning_DeviceState && RfcxSource.VERBOSE) {
			Log.d(TAG, "DeviceStateService not running. Not stopped...");
		}
		if (AudioState.CAPTURE_SERVICE_ENABLED && isServiceRunning_AudioCapture) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else if (!isServiceRunning_AudioCapture && RfcxSource.VERBOSE) {
			Log.d(TAG, "AudioCaptureService not running. Not stopped...");
		}
		if (AudioState.PROCESS_SERVICE_ENABLED && isServiceRunning_AudioProcess) {
			context.stopService(new Intent(context, AudioProcessService.class));
		} else if (!isServiceRunning_AudioProcess && RfcxSource.VERBOSE) {
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

		PendingIntent apiCommServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ApiCommService.class), PendingIntent.FLAG_UPDATE_CURRENT);
//		PendingIntent audioProcessServiceIntent = PendingIntent.getService(context, -1, new Intent(context, AudioProcessServiceInt.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		// Launch APIComm IntentService
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), apiComm.getConnectivityInterval()*1000, apiCommServiceIntent);
		
		// Launch AudioProcessing IntentService
//		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), 150*1000, audioProcessServiceIntent);
	}
	
	
}
