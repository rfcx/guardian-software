package org.rfcx.guardian.audio;

import org.rfcx.guardian.audio.capture.AudioCapture;
import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.audio.encode.AudioEncode;
import org.rfcx.guardian.audio.service.AudioCaptureService;
import org.rfcx.guardian.audio.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.RfcxPrefs;
import org.rfcx.guardian.utility.RfcxRoleVersions;
import org.rfcx.guardian.utility.device.DeviceBattery;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application {

	public String version;
	Context context;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String APP_ROLE = "Audio";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxPrefs rfcxPrefs = null;
	
	// database access helpers
	public AudioDb audioDb = null;
	
	// prefs (WILL BE SET DYNAMICALLY)
//	public int AUDIO_CYCLE_DURATION = (int) Integer.parseInt(   "90000"   );
//	public String AUDIO_ENCODE_CODEC = "aac".toLowerCase();
//	public int AUDIO_ENCODE_BITRATE = (int) Integer.parseInt(   "16384"   );
//	public int AUDIO_ENCODE_QUALITY = (int) Integer.parseInt(   "9"   );
//	public int AUDIO_BATTERY_CUTOFF = (int) Integer.parseInt(   "60"   );


	// capturing and encoding audio
	public final static int AUDIO_SAMPLE_RATE = 8000;
	public AudioCapture audioCapture = new AudioCapture();
	public AudioEncode audioEncode = new AudioEncode();
	
	public DeviceBattery deviceBattery = new DeviceBattery();
	
	// Background Services
	public boolean isRunning_AudioCapture = false;
	
	// Repeating IntentServices
	public boolean isRunning_ServiceMonitor = false;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), this.APP_ROLE);
		
		this.version = RfcxRoleVersions.getAppVersion(getApplicationContext());
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		
		initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}

	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.APP_ROLE, "installer")).getDeviceId();
			rfcxPrefs.writeGuidToFile(this.deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext())).getDeviceToken();
		}
		return this.deviceToken;
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				// captures and saves audio stream
				triggerService("AudioCapture", true);
				// Service Monitor
				triggerIntentService("ServiceMonitor", 
						System.currentTimeMillis(),
						3 * Math.round( this.rfcxPrefs.getPrefAsInt("audio_cycle_duration") / 1000 )
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
		int versionNumber = RfcxRoleVersions.getAppVersionValue(this.version);
		this.audioDb = new AudioDb(this,versionNumber);
	}
    
}
