package org.rfcx.src_android;

import java.util.ArrayList;
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
import org.rfcx.src_util.DeviceUuid;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class RfcxSource extends Application implements OnSharedPreferenceChangeListener {
	
	public static final String VERSION = "0.4.4";
	
	private static final String TAG = RfcxSource.class.getSimpleName();
	private SharedPreferences sharedPreferences;
	Context context;

	private void versionPrefsOverride() {
		Log.d(TAG, "Forcing Specific Prefs for version "+VERSION);
//		addPrefOverride("ignore_off_hours","false","boolean");
//		addPrefOverride("monitor_intentservice_interval","180","string");
//		addPrefOverride("day_begins_at_hour","9","string");
//		addPrefOverride("day_ends_at_hour","17","string");
//		addPrefOverride("api_interval","1800","string");
		runPrefsOverride();
	}
	
	public boolean verboseLogging = false;
	
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
	public int monitorIntentServiceInterval = 180;
	
	public int dayBeginsAt = 9;
	public int dayEndsAt = 17;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Launching org.rfcx.src_android (v"+VERSION+")");
		airplaneMode.setOn(getApplicationContext());
		checkSetPreferences();
		versionPrefsOverride();
		setDeviceId();
		
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

		this.verboseLogging = this.sharedPreferences.getBoolean("verbose_logging", this.verboseLogging);
		this.ignoreOffHours = this.sharedPreferences.getBoolean("ignore_off_hours", this.ignoreOffHours);
		this.monitorIntentServiceInterval = Integer.parseInt(this.sharedPreferences.getString("monitor_intentservice_interval", ""+this.monitorIntentServiceInterval));
		apiComm.setConnectivityInterval(Integer.parseInt(this.sharedPreferences.getString("api_interval", ""+apiComm.getConnectivityInterval())));
		airplaneMode.setAllowWifi(this.sharedPreferences.getBoolean("allow_wifi", airplaneMode.getAllowWifi()));
		apiComm.setApiDomain(this.sharedPreferences.getString("api_domain", "rfcx.org"));
		apiComm.setApiPort(Integer.parseInt(this.sharedPreferences.getString("api_port", "80")));
		apiComm.setApiEndpoint(this.sharedPreferences.getString("api_endpoint", "/api/1/checkin"));
		
		this.isServiceEnabled_AudioCapture = this.sharedPreferences.getBoolean("enable_service_audiocapture", this.isServiceEnabled_AudioCapture);
		this.isServiceEnabled_AudioProcess = this.sharedPreferences.getBoolean("enable_service_audioprocess", this.isServiceEnabled_AudioProcess);
		this.isServiceEnabled_DeviceState = this.sharedPreferences.getBoolean("enable_service_devicestate", this.isServiceEnabled_DeviceState);
		this.isServiceEnabled_ApiComm = this.sharedPreferences.getBoolean("enable_service_apicomm", this.isServiceEnabled_ApiComm);
		
		this.dayBeginsAt = Integer.parseInt(this.sharedPreferences.getString("day_begins_at_hour", ""+this.dayBeginsAt));
		this.dayEndsAt = Integer.parseInt(this.sharedPreferences.getString("day_ends_at_hour", ""+this.dayEndsAt));
		
		if (this.verboseLogging) Log.d(TAG, "Preferences saved.");
	}

	
	public UUID getDeviceId() {
		if (this.deviceId == null) setDeviceId();
		return this.deviceId;
	}
	
	private void setDeviceId() {
		this.deviceId = new DeviceUuid(getApplicationContext(), this.sharedPreferences).getDeviceUuid();
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
	
	
	public void launchAllIntentServices(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent apiCommServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ApiCommIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, MonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), apiComm.getConnectivityInterval()*1000, apiCommServiceIntent);
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), monitorIntentServiceInterval*1000, monitorServiceIntent);
	}
	

	public boolean setPreference(String name, String value, String type) {
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		Editor editor = this.sharedPreferences.edit();
		if (type.equals("boolean")) editor.putBoolean(name, Boolean.parseBoolean(value));
		else if (type.equals("int")) editor.putString(name, ""+value); // still not saving ints as ints 
		else if (type.equals("float")) editor.putString(name, ""+value); // still not saving floats as floats 
		else if (type.equals("long")) editor.putString(name, ""+value);  // still not saving longs as longs 
		else editor.putString(name, value);
		return editor.commit();
	}
	
	
	
	
	private ArrayList<String[]> prefsOverride = new ArrayList<String[]>();
	
	private void addPrefOverride(String name, String value, String type) {
		String[] stringArray = { name, value, type };
		this.prefsOverride.add(stringArray);
	}
	
	private void runPrefsOverride() {
		for (int i = 0; i < this.prefsOverride.size(); i++) {
			String[] thisPref = this.prefsOverride.get(i);
			Log.d(TAG, "Saving Pref: "+thisPref[0]+" > "+thisPref[1]+" > "+ (setPreference(thisPref[0],thisPref[1],thisPref[2]) ? "Success" : "Failure" ));
		}
	}
	
	
}
