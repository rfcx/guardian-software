package setup;

import java.util.Map;

import org.rfcx.guardian.setup.R;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceGuid;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;
import setup.api.ApiCore;
import setup.device.android.control.RebootTriggerJobService;
import setup.receiver.ConnectivityReceiver;
import setup.service.ApiCheckVersionIntentService;
import setup.service.ApiCheckVersionService;
import setup.service.ApiRegisterService;
import setup.service.DownloadFileService;
import setup.service.InstallAppService;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Setup";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);

	public RfcxDeviceGuid rfcxDeviceGuid = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;

	public SharedPreferences sharedPrefs = null;
	
	public static final String targetAppRoleApiEndpoint = "all";
	public String targetAppRole = "";
	
	public long lastApiCheckTriggeredAt = System.currentTimeMillis();
	
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	public ApiCore apiCore = new ApiCore();
	
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);

	public String[] RfcxCoreServices = 
		new String[] { 
		};
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceGuid = new RfcxDeviceGuid(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, logTag);
		
		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPrefs.registerOnSharedPreferenceChangeListener(this);
		this.syncSharedPrefs();
		
		if (this.sharedPrefs.getString("guardian_guid", "").trim().length() != 12) {
			this.setPref("guardian_guid", this.rfcxDeviceGuid.getDeviceGuid());
		}
		
		Log.d(logTag, "Guardian GUID: "+this.getDeviceGuid());
		this.rfcxPrefs.writeGuidToFile(this.getDeviceGuid());
		
		this.apiCore.targetAppRoleApiEndpoint = this.targetAppRoleApiEndpoint;
		this.apiCore.setApiCheckVersionEndpoint(this.getDeviceGuid());
		this.apiCore.setApiRegisterEndpoint();
		
		// install external binaries
//		this.installExternalExecutable("fb2png", false);
//		this.installExternalExecutable("logcat_capture", true);
		
		setDbHandlers();
		setServiceHandlers();
		
		initializeRoleServices();

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
	
	public void initializeRoleServices() {
		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {
			
			String[] onLaunchServices = new String[RfcxCoreServices.length+1];
			System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
			onLaunchServices[RfcxCoreServices.length] = 
					"ApiCheckVersionIntentService"
							+"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits 2 minutes before running
							+"|"+3600000 // repeats hourly
						;
			
			this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true);
		}
	}
	
	private void setDbHandlers() {

	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ApiCheckVersion", ApiCheckVersionService.class);
		this.rfcxServiceHandler.addService("ApiCheckVersionIntentService", ApiCheckVersionIntentService.class);
		this.rfcxServiceHandler.addService("ApiRegister", ApiRegisterService.class);
		this.rfcxServiceHandler.addService("InstallApp", InstallAppService.class);
		this.rfcxServiceHandler.addService("DownloadFile", DownloadFileService.class);
		this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerJobService.class);		
	}
	
	

	
	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {
		Log.d(logTag, "Pref changed: "+prefKey+" = "+this.sharedPrefs.getString(prefKey, null));
		syncSharedPrefs();
	}
	
	private void syncSharedPrefs() {
		for ( Map.Entry<String,?> pref : this.sharedPrefs.getAll().entrySet() ) {
//			this.rfcxPrefs.setPref(pref.getKey(), pref.getValue().toString());
		}
	}
	
	public boolean setPref(String prefKey, String prefValue) {
		return this.sharedPrefs.edit().putString(prefKey,prefValue).commit();
	}
	
	public String getPref(String prefKey) {
		return this.sharedPrefs.getString(prefKey, null);
	}
	
	public String getDeviceGuid() {
		if (this.sharedPrefs.getString("guardian_guid", "").length() == 12) {
			return this.sharedPrefs.getString("guardian_guid", null);
		} else {
			return this.rfcxDeviceGuid.getDeviceGuid();
		}
	}
	
	
	
	
	
	
//	
//	
//    
//    public void setExtremeDevelopmentSystemDefaults(){
//    	
//    	context = getApplicationContext();
//    	
//    	String[] buildDotPropSettings = new String[] {
//    			"service.adb.tcp.port=5555", // permanently turns on network adb access (for bluetooth/wifi administration)
//    			"ro.com.android.dataroaming=true", // turns on data roaming
//    			"ro.com.android.dateformat=yyyy-MM-dd", // sets the date format
//    			"net.bt.name=rfcx-"+this.rfcxDeviceId.getDeviceGuid().substring(0,4) // sets the default bluetooth device name to 
//    		};
//    	
//    	ShellCommands.executeCommand("mount -o rw,remount /dev/block/mmcblk0p1 /system", null, true, context);
//    	
//    	String writeToBuildDotProp = "";
//    	for (int i = 0; i < buildDotPropSettings.length; i++) {
//    		writeToBuildDotProp += " echo "+buildDotPropSettings[i]+" >> /system/build.prop; ";
//    	}
//    	
//    	ShellCommands.executeCommand(writeToBuildDotProp, null, true, context);
//    	
//    }
//    
//    public void deleteExtraCyanogenModApps() {
//            
//        	context = getApplicationContext();
//        	
//        	String[] appsToDelete = new String[] {
//        			"Camera", "Calculator", "Browser", "Pacman", "Email", "FM", 
//        			"Calendar", "Gallery", "Music", "QuickSearchBox", "VoiceDialer", "RomManager"
//        		};
//        	
//        	ShellCommands.executeCommand("mount -o rw,remount /dev/block/mmcblk0p1 /system", null, true, context);
//        	
//        	String executeAppDeletion = "";
//        	for (int i = 0; i < appsToDelete.length; i++) {
//        		executeAppDeletion += " rm -f /system/app/"+appsToDelete[i]+".apk; ";
//        	}
//        	
//        	ShellCommands.executeCommand(executeAppDeletion, null, true, context);
//    }
//    
//    private boolean installExternalExecutable(String binName, boolean forceOverWrite) {
//    	try {
//    		
//    		String assetDirPath = Environment.getDownloadCacheDirectory().getAbsolutePath()+"/rfcx";
//    		String binDirPath = assetDirPath+"/bin"; File binDir = new File(binDirPath);
//    		String binFilePath = binDirPath+"/"+binName; File binFile = new File(binFilePath);
//    		
//    		if (!(new File(assetDirPath)).exists()) {
//    			ShellCommands.executeCommand("mkdir "+assetDirPath+"; chmod a+rw "+assetDirPath+";", null, true, getApplicationContext());
//    		}
//    		
//    		if (!binDir.exists()) {
//	    		binDir.mkdirs();
//	    		FileUtils.chmod(binDir, 0755);
//    		}
//    		
//	     	if (forceOverWrite) { binFile.delete(); }
//	     		
//	        if (!binFile.exists()) {
//	    		try {
//	    			InputStream inputStream = getApplicationContext().getAssets().open(binName);
//	    		    OutputStream outputStream = new FileOutputStream(binFilePath);
//	    		    byte[] buf = new byte[1024];
//	    		    int len;
//	    		    while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
//	    		    inputStream.close();
//	    		    outputStream.close();
//	    		    FileUtils.chmod(binFile, 0755);
//	    		    return binFile.exists();
//	    		} catch (IOException e) {
//	    			RfcxLog.logExc(logTag, e);
//	    			return false;
//	    		}
//	        } else {
//	        	return true;
//	        }
//    	} catch (Exception e) {
//    		RfcxLog.logExc(logTag, e);
//    		return false;
//    	}
//    }
    
}
