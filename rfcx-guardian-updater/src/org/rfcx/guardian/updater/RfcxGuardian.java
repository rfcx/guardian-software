package org.rfcx.guardian.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.updater.api.ApiCore;
import org.rfcx.guardian.updater.receiver.ConnectivityReceiver;
import org.rfcx.guardian.updater.service.ApiCheckVersionService;
import org.rfcx.guardian.updater.service.DownloadFileService;
import org.rfcx.guardian.updater.service.InstallAppService;
import org.rfcx.guardian.updater.service.ApiCheckVersionIntentService;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.RfcxConstants;
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

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+RfcxGuardian.class.getSimpleName();
	public String version;
	Context context;
	public boolean verboseLog = true;
	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastApiCheckTriggeredAt = Calendar.getInstance().getTimeInMillis();
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String thisAppRole = "updater";
	public static final String targetAppRoleApiEndpoint = "all";
	public String targetAppRole = "";
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
		
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	public ApiCore apiCore = new ApiCore();
	
	public boolean isRunning_ApiCheckVersion = false;
	public boolean isRunning_DownloadFile = false;
	public boolean isRunning_InstallApp = false;
	
	public boolean isRunning_UpdaterService = false;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet(this);
		
		setAppVersion();
				
		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		apiCore.setApiCheckVersionEndpoint(getDeviceId());
		
	    initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
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
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				int delayAfterAppLaunchInMinutes = 5;
				long apiCheckVersionInterval = ((getPref("apicheckversion_interval")!=null) ? Integer.parseInt(getPref("apicheckversion_interval")) : 180)*60*1000;
				PendingIntent updaterIntentService = PendingIntent.getService(context, -1, new Intent(context, ApiCheckVersionIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				AlarmManager updaterAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);		
				updaterAlarmManager.setInexactRepeating(AlarmManager.RTC, (System.currentTimeMillis()+delayAfterAppLaunchInMinutes*60*1000), apiCheckVersionInterval, updaterIntentService);
				if (verboseLog) { Log.d(TAG, "ApiCheckVersion will run every "+getPref("apicheckversion_interval")+" minute(s), starting at "+(new Date((System.currentTimeMillis()+delayAfterAppLaunchInMinutes*60*1000))).toLocaleString()); }
				this.hasRun_OnLaunchServiceTrigger = true;	
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
	}
	
	public String getPref(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
	
	public void triggerService(String serviceName, boolean forceReTrigger) {
		context = getApplicationContext();
		
		if (serviceName.equals("ApiCheckVersion")) {
			if (!this.isRunning_ApiCheckVersion || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckVersionService.class));
				context.startService(new Intent(context, ApiCheckVersionService.class));
			} else {
				if (this.verboseLog) { Log.d(TAG, "Service ApiCheckVersion is already running..."); }
			}
		} else if (serviceName.equals("DownloadFile")) {
			if (!this.isRunning_DownloadFile || forceReTrigger) {
				context.stopService(new Intent(context, DownloadFileService.class));
				context.startService(new Intent(context, DownloadFileService.class));
			} else {
				if (this.verboseLog) { Log.d(TAG, "Service DownloadFile is already running..."); }
			}
		} else if (serviceName.equals("InstallApp")) {
			if (!this.isRunning_InstallApp || forceReTrigger) {
				context.stopService(new Intent(context, InstallAppService.class));
				context.startService(new Intent(context, InstallAppService.class));
			} else {
				if (this.verboseLog) { Log.d(TAG, "Service InstallApp is already running..."); }
			}
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("ApiCheckVersion")) {
			context.stopService(new Intent(context, ApiCheckVersionService.class));
		} else if (serviceName.equals("DownloadFile")) {
			context.stopService(new Intent(context, DownloadFileService.class));
		} else if (serviceName.equals("InstallApp")) {
			context.stopService(new Intent(context, InstallAppService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}	
	}
	
	public boolean isRoleInstalled(String appRole) {
		String mainAppPath = getApplicationContext().getFilesDir().getAbsolutePath();
		return (new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/org.rfcx.guardian."))+"/org.rfcx.guardian."+appRole)).exists();
	}

	private String getValueFromGuardianTxtFile(String appRole, String fileNameNoExt) {
    	context = getApplicationContext();
    	try {
    		String mainAppPath = context.getFilesDir().getAbsolutePath();
    		Log.d(TAG,mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+this.thisAppRole).length()))+"."+appRole+"/files/txt/"+fileNameNoExt+".txt");
    		File txtFile = new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+this.thisAppRole).length()))+"."+appRole+"/files/txt",fileNameNoExt+".txt");
    		if (txtFile.exists()) {
				FileInputStream input = new FileInputStream(txtFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[12];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String txtFileContents = fileContent.toString().trim();
	    		input.close();
	    		return txtFileContents;
    		} else {
    			Log.e(TAG, "No file '"+fileNameNoExt+"' saved by org.rfcx.guardian."+appRole+"...");
    		}
    	} catch (FileNotFoundException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
    	} catch (IOException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
    	return null;
	}
	
	private String getValueFromGuardianTargetRoleTxtFile(String fileNameNoExt) {
		return getValueFromGuardianTxtFile(this.targetAppRole, fileNameNoExt);
	}
	
    public String getCurrentGuardianTargetRoleVersion() {
    	return getValueFromGuardianTargetRoleTxtFile("version");
    }
    
    public String getCurrentGuardianRoleVersion(String appRole) {
    	return getValueFromGuardianTxtFile(appRole, "version");
    }
    
}
