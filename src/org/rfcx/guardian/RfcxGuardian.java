package org.rfcx.guardian;

import java.util.Calendar;

import org.rfcx.guardian.api.ApiCore;
import org.rfcx.guardian.audio.AudioCore;
import org.rfcx.guardian.carrier.CarrierInteraction;
import org.rfcx.guardian.database.AudioDb;
import org.rfcx.guardian.database.CheckInDb;
import org.rfcx.guardian.database.DataTransferDb;
import org.rfcx.guardian.database.DeviceStateDb;
import org.rfcx.guardian.database.ScreenShotDb;
import org.rfcx.guardian.database.SmsDb;
import org.rfcx.guardian.device.DeviceCPUTuner;
import org.rfcx.guardian.device.DeviceCpuUsage;
import org.rfcx.guardian.device.DeviceState;
import org.rfcx.guardian.intentservice.CarrierCodeBalance;
import org.rfcx.guardian.intentservice.CarrierCodeTopUp;
import org.rfcx.guardian.intentservice.ServiceMonitor;
import org.rfcx.guardian.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.receiver.ConnectivityReceiver;
import org.rfcx.guardian.service.ApiCheckInService;
import org.rfcx.guardian.service.ApiCheckInTrigger;
import org.rfcx.guardian.service.AudioCaptureService;
import org.rfcx.guardian.service.CarrierCodeService;
import org.rfcx.guardian.service.DeviceCPUTunerService;
import org.rfcx.guardian.service.DeviceStateService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.DeviceAirplaneMode;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceScreenLock;
import org.rfcx.guardian.utility.DeviceScreenShot;
import org.rfcx.guardian.utility.ShellCommands;

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
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = "RfcxGuardian-"+RfcxGuardian.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	public String version = "0.0.0";
	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	private String deviceId = null;
	Context context;
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	// database access helpers
	public DeviceStateDb deviceStateDb = null;
	public SmsDb smsDb = null;
	public AudioDb audioDb = null;
	public ScreenShotDb screenShotDb = null;
	public CheckInDb checkInDb = null;
	public DataTransferDb dataTransferDb = null;

	// for obtaining device stats and characteristics
	public DeviceState deviceState = new DeviceState();
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	
	// for viewing and controlling airplane mode
	public DeviceAirplaneMode airplaneMode = new DeviceAirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	// for transmitting api data
	public ApiCore apiCore = new ApiCore();
	
	// for handling captured audio
	public AudioCore audioCore = new AudioCore();
	
	// for interacting with telecom carriers
	public CarrierInteraction carrierInteraction = new CarrierInteraction();
	public DeviceScreenLock deviceScreenLock = new DeviceScreenLock();
	public DeviceScreenShot deviceScreenShot = new DeviceScreenShot();
	
	
	
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
	public boolean isRunning_ApiCheckInTrigger = false;
	public boolean isRunning_CarrierCode = false;
	public boolean isRunning_CPUTuner = false;
	
	// Repeating IntentServices
	public boolean isRunning_ServiceMonitor = false;
	
	private boolean hasRun_OnBootServiceTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Context context = getApplicationContext();
		
		setAppVersion();
		setDbHandlers();
		
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.loadPrefsOverride();
		rfcxGuardianPrefs.checkAndSet(this);
		
		apiCore.init(this);

		(new ShellCommands()).executeCommandAsRoot("pm list features",null,context);
		(new DeviceCPUTuner()).set(context);
		deviceScreenShot.setupScreenShot(context);
		
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	    
	    onLaunchServiceTrigger();
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
		Log.i(TAG, "Preference changed: "+key);
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
	
	public int getAppVersionValue(String versionName) {
		try {
			int majorVersion = (int) Integer.parseInt(versionName.substring(0, versionName.indexOf(".")));
			int subVersion = (int) Integer.parseInt(versionName.substring(1+versionName.indexOf("."), versionName.lastIndexOf(".")));
			int updateVersion = (int) Integer.parseInt(versionName.substring(1+versionName.lastIndexOf(".")));
			return 1000*majorVersion+100*subVersion+updateVersion;
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return 0;
	}
	
	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
			Log.i(TAG,"Device GUID: "+this.deviceId);
			rfcxGuardianPrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}

	public String getPref(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	public int getPrefInt(String prefName) {
		return this.sharedPrefs.getInt(prefName, 0);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
	
	public void onLaunchServiceTrigger() {
		if (!hasRun_OnBootServiceTrigger) {
			// Service Monitor
			triggerIntentService("ServiceMonitor", 
					System.currentTimeMillis(),
					60*((int) Integer.parseInt(getPref("service_monitor_interval")))
					);
			// CarrierCodeTrigger-Balance, starting 2 minutes after app launch, runs repeatedly every 3 hours
			// TODO: this should be configurable in prefs, and also probably made more generic
			triggerIntentService("CarrierCodeTrigger-Balance", 
					System.currentTimeMillis()+( 2 *(60*1000)), 
					( 6 * 3600)
					);
			// CarrierCodeTrigger-TopUp, starting at the next instance of 30 seconds after midnight, repeating every 12 hours
			// TODO: this should be configurable in prefs, and also probably made to be more generic
			triggerIntentService("CarrierCodeTrigger-TopUp", 
					(new DateTimeUtils()).nextOccurenceOf(0,0,30).getTimeInMillis(), 
					( 12 *3600)
					);
			triggerService("DeviceState", true);
			triggerService("AudioCapture", true);
			triggerService("ApiCheckInTrigger", true);
			hasRun_OnBootServiceTrigger = true;
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
				PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ServiceMonitor.class), PendingIntent.FLAG_UPDATE_CURRENT);
				if (repeatIntervalSeconds == 0) { alarmManager.set(AlarmManager.RTC, startTimeMillis, monitorServiceIntent);
				} else { alarmManager.setInexactRepeating(AlarmManager.RTC, startTimeMillis, repeatInterval, monitorServiceIntent); }
			} else { Log.w(TAG, "Repeating IntentService 'ServiceMonitor' is already running..."); }
		} else if (intentServiceName.equals("CarrierCodeTrigger-Balance")) {
				PendingIntent carrierCodeTrigger = PendingIntent.getService(context, -1, new Intent(context, CarrierCodeBalance.class), PendingIntent.FLAG_UPDATE_CURRENT);
				String logMsg = "CarrierCodeTrigger-Balance will be launched at "+startTimeMillis;
				if (repeatIntervalSeconds == 0) { alarmManager.set(AlarmManager.RTC, startTimeMillis, carrierCodeTrigger);
				} else { alarmManager.setInexactRepeating(AlarmManager.RTC, startTimeMillis, repeatInterval, carrierCodeTrigger);
				logMsg += " and repeat every "+(repeatIntervalSeconds/3600)+" hours"; }
				Log.i(TAG, logMsg);
		} else if (intentServiceName.equals("CarrierCodeTrigger-TopUp")) {
				PendingIntent carrierCodeTrigger = PendingIntent.getService(context, -1, new Intent(context, CarrierCodeTopUp.class), PendingIntent.FLAG_UPDATE_CURRENT);
				String logMsg = "CarrierCodeTrigger-TopUp will be launched at "+startTimeMillis;
				if (repeatIntervalSeconds == 0) { alarmManager.set(AlarmManager.RTC, startTimeMillis, carrierCodeTrigger);
				} else { alarmManager.setRepeating(AlarmManager.RTC, startTimeMillis, repeatInterval, carrierCodeTrigger);
				// In this case we use setRepeating so that we can more precisely set the timing of the TopUp.
				logMsg += " and repeat every "+(repeatIntervalSeconds/3600)+" hours"; }
				Log.i(TAG, logMsg);
		} else {
			Log.w(TAG, "No IntentService named '"+intentServiceName+"'.");
		}
	}
	
	
	public void triggerService(String serviceName, boolean forceReTrigger) {
		context = getApplicationContext();
		boolean serviceAllowedInPrefs = this.sharedPrefs.getBoolean("enable_service_"+serviceName.toLowerCase(), true);
		if (forceReTrigger) Log.w(TAG,"Forcing [re]trigger of service "+serviceName);
		if (serviceName.equals("AudioCapture")) {
			if (!this.isRunning_AudioCapture || forceReTrigger) {
				context.stopService(new Intent(context, AudioCaptureService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, AudioCaptureService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.w(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("DeviceState")) {
			if (!this.isRunning_DeviceState || forceReTrigger) {
				context.stopService(new Intent(context, DeviceStateService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, DeviceStateService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.w(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("ApiCheckIn")) {
			if (!this.isRunning_ApiCheckIn || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckInService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, ApiCheckInService.class));
			}// else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.w(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("ApiCheckInTrigger")) {
			if (!this.isRunning_ApiCheckInTrigger || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckInTrigger.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, ApiCheckInTrigger.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.w(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("CarrierCode")) {
			if (!this.isRunning_CarrierCode || forceReTrigger) {
				context.stopService(new Intent(context, CarrierCodeService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, CarrierCodeService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.w(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else if (serviceName.equals("CPUTuner")) {
			if (!this.isRunning_CPUTuner || forceReTrigger) {
				context.stopService(new Intent(context, DeviceCPUTunerService.class));
				if (serviceAllowedInPrefs) context.startService(new Intent(context, DeviceCPUTunerService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
			if (!serviceAllowedInPrefs) Log.w(TAG, "Service '"+serviceName+"' is disabled in preferences, and cannot be triggered.");
		} else {
			Log.w(TAG, "There is no service named '"+serviceName+"'.");
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
		} else if (serviceName.equals("ApiCheckInTrigger")) {
			context.stopService(new Intent(context, ApiCheckInTrigger.class));
		} else if (serviceName.equals("CarrierCode")) {
			context.stopService(new Intent(context, CarrierCodeService.class));
		} else if (serviceName.equals("CPUTuner")) {
			context.stopService(new Intent(context, DeviceCPUTunerService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = getAppVersionValue(this.version);
		this.deviceStateDb = new DeviceStateDb(this,versionNumber);
		this.smsDb = new SmsDb(this,versionNumber);
		this.audioDb = new AudioDb(this,versionNumber);
		this.screenShotDb = new ScreenShotDb(this,versionNumber);
		this.checkInDb = new CheckInDb(this,versionNumber);
		this.dataTransferDb = new DataTransferDb(this,versionNumber);
	}
	
}
