package org.rfcx.guardian.system;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import org.rfcx.guardian.system.database.DataTransferDb;
import org.rfcx.guardian.system.database.DeviceStateDb;
import org.rfcx.guardian.system.database.ScreenShotDb;
import org.rfcx.guardian.system.device.DeviceCpuUsage;
import org.rfcx.guardian.system.device.DeviceScreenLock;
import org.rfcx.guardian.system.device.DeviceState;
import org.rfcx.guardian.system.service.DeviceScreenShotService;
import org.rfcx.guardian.system.service.DeviceStateService;
import org.rfcx.guardian.system.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.RfcxPrefs;
import org.rfcx.guardian.utility.RfcxRoleVersions;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceDiskUsage;

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

public class RfcxGuardian extends Application {

	public String version;
	Context context;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String APP_ROLE = "System";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxPrefs rfcxPrefs = null;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = null;
	public DataTransferDb dataTransferDb = null;
	public ScreenShotDb screenShotDb = null;

	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();

	// prefs (WILL BE SET DYNAMICALLY)
//	public int AUDIO_CYCLE_DURATION = (int) Integer.parseInt(   "90000"   );
//	public int SCREENSHOT_CYCLE_DURATION = (int) Integer.parseInt(   "90000"   );

	// for obtaining device stats and characteristics
	public DeviceState deviceState = new DeviceState();
	
	public DeviceBattery deviceBattery = new DeviceBattery();
	public DeviceDiskUsage deviceDiskUsage = new DeviceDiskUsage();
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	public DeviceScreenLock deviceScreenLock = new DeviceScreenLock();
	
	// Background Services
	public boolean isRunning_DeviceState = false;
	public boolean isRunning_DeviceScreenShot = false;
	
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
				
				// Service Monitor
				triggerIntentService("ServiceMonitor", 
						System.currentTimeMillis(),
						3 * Math.round( this.rfcxPrefs.getPrefAsInt("audio_cycle_duration") / 1000 )
						);
				// background service for gather system stats
				triggerService("DeviceState", true);
				// background service for taking screenshots
				triggerService("ScreenShot", true);
				
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
		if (serviceName.equals("DeviceState")) {
			if (!this.isRunning_DeviceState || forceReTrigger) {
				context.stopService(new Intent(context, DeviceStateService.class));
				context.startService(new Intent(context, DeviceStateService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else if (serviceName.equals("ScreenShot")) {
			if (!this.isRunning_DeviceScreenShot || forceReTrigger) {
				context.stopService(new Intent(context, DeviceScreenShotService.class));
				context.startService(new Intent(context, DeviceScreenShotService.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else {
			Log.w(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("DeviceState")) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (serviceName.equals("ScreenShot")) {
			context.stopService(new Intent(context, DeviceScreenShotService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRoleVersions.getAppVersionValue(this.version);
		this.deviceStateDb = new DeviceStateDb(this,versionNumber);
		this.dataTransferDb = new DataTransferDb(this,versionNumber);
		this.screenShotDb = new ScreenShotDb(this,versionNumber);
	}
 
    public boolean findOrCreateLogcatCaptureScript() {
    	try {
	     	File logcatCaptureScript = new File(this.getFilesDir().getAbsolutePath()+"/bin/logcat_capture");
	     	
	        if (!logcatCaptureScript.exists()) {
	    		try {
	    			InputStream inputStream = this.getAssets().open("logcat_capture");
	    		    OutputStream outputStream = new FileOutputStream(this.getFilesDir().getAbsolutePath()+"/bin/logcat_capture");
	    		    byte[] buf = new byte[1024];
	    		    int len;
	    		    while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
	    		    inputStream.close();
	    		    outputStream.close();
	    		    (new FileUtils()).chmod(logcatCaptureScript, 0755);
	    		    return logcatCaptureScript.exists();
	    		} catch (IOException e) {
	    			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
	    			return false;
	    		}
	        } else {
	        	return true;
	        }
    	} catch (Exception e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
    		return false;
    	}
    }
	
	
}
