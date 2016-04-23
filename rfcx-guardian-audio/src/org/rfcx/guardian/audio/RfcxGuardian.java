package org.rfcx.guardian.audio;

import org.rfcx.guardian.audio.capture.AudioCapture;
import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.audio.device.DeviceBattery;
import org.rfcx.guardian.audio.encode.AudioEncode;
import org.rfcx.guardian.audio.service.AudioCaptureService;
import org.rfcx.guardian.audio.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.RfcxConstants;

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
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String thisAppRole = "audio";
	public final String targetAppRole = "updater";
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	// database access helpers
	public AudioDb audioDb = null;

	// capturing and encoding audio
	public final static int AUDIO_SAMPLE_RATE = 8000;
	public AudioCapture audioCapture = new AudioCapture();
	public AudioEncode audioEncode = new AudioEncode();
	
	// prefs (WILL BE SET DYNAMICALLY)
	public int AUDIO_CYCLE_DURATION = (int) Integer.parseInt(   "90000"   );
	public String AUDIO_CODEC = "aac".toLowerCase();
	public int AUDIO_BITRATE = (int) Integer.parseInt(   "16384"   );
	public int AUDIO_BATTERY_CUTOFF = (int) Integer.parseInt(   "60"   );
	
	public DeviceBattery deviceBattery = new DeviceBattery();
	
	// Background Services
	public boolean isRunning_AudioCapture = false;
	
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
		Log.d(TAG, "Preference changed: "+key);
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
			rfcxGuardianPrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext(), this.sharedPrefs)).getDeviceToken();
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
				// captures and saves audio stream
				triggerService("AudioCapture", true);
				// Service Monitor
				triggerIntentService("ServiceMonitor", 
						System.currentTimeMillis(),
						3 * Math.round( AUDIO_CYCLE_DURATION / 1000 )
						);

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
		if (serviceName.equals("AudioCapture")) {
			if (!this.isRunning_AudioCapture || forceReTrigger) {
				context.stopService(new Intent(context, AudioCaptureService.class));
				context.startService(new Intent(context, AudioCaptureService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else {
			Log.w(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("AudioCapture")) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = getAppVersionValue(this.version);
		this.audioDb = new AudioDb(this,versionNumber);
	}
    
}
