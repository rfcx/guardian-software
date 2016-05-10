package org.rfcx.guardian.installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.rfcx.guardian.installer.api.ApiCore;
import org.rfcx.guardian.installer.receiver.ConnectivityReceiver;
import org.rfcx.guardian.installer.service.ApiCheckVersionService;
import org.rfcx.guardian.installer.service.ApiRegisterService;
import org.rfcx.guardian.installer.service.DownloadFileService;
import org.rfcx.guardian.installer.service.InstallAppService;
import org.rfcx.guardian.installer.service.ApiCheckVersionIntentService;
import org.rfcx.guardian.installer.service.DeviceCPUTunerService;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.device.DeviceBattery;

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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Installer";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	
	public static final String targetAppRoleApiEndpoint = "updater";
	public String targetAppRole = "updater";
	
	public SharedPreferences sharedPrefs = null;

	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastApiCheckTriggeredAt = Calendar.getInstance().getTimeInMillis();
	
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	public ApiCore apiCore = new ApiCore();
	
	// for checking battery level
	public DeviceBattery deviceBattery = new DeviceBattery();
	
	public boolean isRunning_ApiRegister = false;
	public boolean isRunning_ApiCheckVersion = false;
	public boolean isRunning_DownloadFile = false;
	public boolean isRunning_InstallApp = false;
	public boolean isRunning_CPUTuner = false;
	
	public boolean isRunning_UpdaterService = false;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();

		this.rfcxDeviceId = (new RfcxDeviceId()).init(getApplicationContext());
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), APP_ROLE);
		
		PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.prefs, true);
		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		this.sharedPrefs.registerOnSharedPreferenceChangeListener(this);
		this.syncSharedPrefs();
		
		this.version = RfcxRole.getRoleVersion(getApplicationContext(), TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		// install external binary
		this.installExternalExecutable("fb2png", false);
		this.installExternalExecutable("logcat_capture", true);
		
		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		this.apiCore.targetAppRoleApiEndpoint = this.targetAppRoleApiEndpoint;
		this.apiCore.setApiCheckVersionEndpoint(this.rfcxDeviceId.getDeviceGuid());
		this.apiCore.setApiRegisterEndpoint();
		
	    initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
		this.unregisterReceiver(connectivityReceiver);
	}
	
	public void appResume() {
		syncSharedPrefs();
	}
	
	public void appPause() {
		
	}
	
	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {
		Log.d(TAG, "Pref changed: "+prefKey+" = "+this.sharedPrefs.getString(prefKey, null));
		syncSharedPrefs();
	}
	
	private void syncSharedPrefs() {
		for ( Map.Entry<String,?> pref : this.sharedPrefs.getAll().entrySet() ) {
			this.rfcxPrefs.setPref(this.APP_ROLE, pref.getKey(), pref.getValue().toString());
		}
	}
	
	public boolean setPref(String prefKey, String prefValue) {
		return this.sharedPrefs.edit().putString(prefKey,prefValue).commit();
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {

				// force CPUTuner Config (requires root access)
				triggerService("CPUTuner", true);
				
				int delayAfterAppLaunchInMinutes = 2;
				PendingIntent updaterIntentService = PendingIntent.getService(context, -1, new Intent(context, ApiCheckVersionIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				AlarmManager updaterAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);		
				updaterAlarmManager.setInexactRepeating(AlarmManager.RTC, ( System.currentTimeMillis() + ( delayAfterAppLaunchInMinutes * (60 * 1000) ) ), this.rfcxPrefs.getPrefAsInt("install_cycle_duration"), updaterIntentService);
				Log.d(TAG, "ApiCheckVersion will run every " + Math.round( this.rfcxPrefs.getPrefAsInt("install_cycle_duration") / (60*1000) ) + " minute(s), starting at "+(new Date(( System.currentTimeMillis() + ( delayAfterAppLaunchInMinutes * (60 * 1000) ) ))).toLocaleString());
				this.hasRun_OnLaunchServiceTrigger = true;	
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			}
		}
	}
	
	public void triggerService(String serviceName, boolean forceReTrigger) {
		context = getApplicationContext();
		
		if (serviceName.equals("ApiCheckVersion")) {
			if (!this.isRunning_ApiCheckVersion || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckVersionService.class));
				context.startService(new Intent(context, ApiCheckVersionService.class));
			} else {Log.d(TAG, "Service ApiCheckVersion is already running..."); }
		} else if (serviceName.equals("ApiRegister")) {
			if (!this.isRunning_ApiRegister || forceReTrigger) {
				context.stopService(new Intent(context, ApiRegisterService.class));
				context.startService(new Intent(context, ApiRegisterService.class));
			} else {Log.d(TAG, "Service ApiRegister is already running..."); }
		} else if (serviceName.equals("DownloadFile")) {
			if (!this.isRunning_DownloadFile || forceReTrigger) {
				context.stopService(new Intent(context, DownloadFileService.class));
				context.startService(new Intent(context, DownloadFileService.class));
			} else {Log.d(TAG, "Service DownloadFile is already running..."); }
		} else if (serviceName.equals("InstallApp")) {
			if (!this.isRunning_InstallApp || forceReTrigger) {
				context.stopService(new Intent(context, InstallAppService.class));
				context.startService(new Intent(context, InstallAppService.class));
			} else { Log.d(TAG, "Service InstallApp is already running..."); }
		} else if (serviceName.equals("CPUTuner")) {
			if (!this.isRunning_CPUTuner || forceReTrigger) {
				context.stopService(new Intent(context, DeviceCPUTunerService.class));
				context.startService(new Intent(context, DeviceCPUTunerService.class));
			} else { Log.d(TAG, "Service CPUTuner is already running..."); }
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("ApiCheckVersion")) {
			context.stopService(new Intent(context, ApiCheckVersionService.class));
		} else if (serviceName.equals("ApiRegister")) {
			context.stopService(new Intent(context, ApiRegisterService.class));
		} else if (serviceName.equals("DownloadFile")) {
			context.stopService(new Intent(context, DownloadFileService.class));
		} else if (serviceName.equals("InstallApp")) {
			context.stopService(new Intent(context, InstallAppService.class));
		} else if (serviceName.equals("CPUTuner")) {
			context.stopService(new Intent(context, DeviceCPUTunerService.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}	
	}

	
	
	
	
	
	
	
	
    
    public void setExtremeDevelopmentSystemDefaults(){
    	
    	context = getApplicationContext();
    	
    	String[] buildDotPropSettings = new String[] {
    			"service.adb.tcp.port=5555", // permanently turns on network adb access (for bluetooth/wifi administration)
    			"ro.com.android.dataroaming=true", // turns on data roaming
    			"ro.com.android.dateformat=yyyy-MM-dd", // sets the date format
    			"net.bt.name=rfcx-"+this.rfcxDeviceId.getDeviceGuid().substring(0,4) // sets the default bluetooth device name to 
    		};
    	
    	ShellCommands.executeCommand("mount -o rw,remount /dev/block/mmcblk0p1 /system", null, true, context);
    	
    	String writeToBuildDotProp = "";
    	for (int i = 0; i < buildDotPropSettings.length; i++) {
    		writeToBuildDotProp += " echo "+buildDotPropSettings[i]+" >> /system/build.prop; ";
    	}
    	
    	ShellCommands.executeCommand(writeToBuildDotProp, null, true, context);
    	
    }
    
    public void deleteExtraCyanogenModApps() {
            
        	context = getApplicationContext();
        	
        	String[] appsToDelete = new String[] {
        			"Camera", "Calculator", "Browser", "Pacman", "Email", "FM", 
        			"Calendar", "Gallery", "Music", "QuickSearchBox", "VoiceDialer", "RomManager"
        		};
        	
        	ShellCommands.executeCommand("mount -o rw,remount /dev/block/mmcblk0p1 /system", null, true, context);
        	
        	String executeAppDeletion = "";
        	for (int i = 0; i < appsToDelete.length; i++) {
        		executeAppDeletion += " rm -f /system/app/"+appsToDelete[i]+".apk; ";
        	}
        	
        	ShellCommands.executeCommand(executeAppDeletion, null, true, context);
    }
    
    private boolean installExternalExecutable(String binName, boolean forceOverWrite) {
    	try {
    		
    		String assetDirPath = Environment.getDownloadCacheDirectory().getAbsolutePath()+"/rfcx";
    		String binDirPath = assetDirPath+"/bin"; File binDir = new File(binDirPath);
    		String binFilePath = binDirPath+"/"+binName; File binFile = new File(binFilePath);
    		
    		if (!(new File(assetDirPath)).exists()) {
    			ShellCommands.executeCommand("mkdir "+assetDirPath+"; chmod a+rw "+assetDirPath+";", null, true, getApplicationContext());
    		}
    		
    		if (!binDir.exists()) {
	    		binDir.mkdirs();
	    		FileUtils.chmod(binDir, 0755);
    		}
    		
	     	if (forceOverWrite) { binFile.delete(); }
	     		
	        if (!binFile.exists()) {
	    		try {
	    			InputStream inputStream = getApplicationContext().getAssets().open(binName);
	    		    OutputStream outputStream = new FileOutputStream(binFilePath);
	    		    byte[] buf = new byte[1024];
	    		    int len;
	    		    while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
	    		    inputStream.close();
	    		    outputStream.close();
	    		    FileUtils.chmod(binFile, 0755);
	    		    return binFile.exists();
	    		} catch (IOException e) {
	    			RfcxLog.logExc(TAG, e);
	    			return false;
	    		}
	        } else {
	        	return true;
	        }
    	} catch (Exception e) {
    		RfcxLog.logExc(TAG, e);
    		return false;
    	}
    }
    
}
