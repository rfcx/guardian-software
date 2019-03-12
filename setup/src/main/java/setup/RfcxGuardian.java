package setup;

import java.util.Map;

import org.rfcx.guardian.setup.R;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceBattery;
import rfcx.utility.device.DeviceConnectivity;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxDeviceGuid;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxPrefs;
import rfcx.utility.rfcx.RfcxRole;
import rfcx.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;
import setup.api.UpdateCheckInUtils;
import setup.device.android.control.RebootTriggerJobService;
import setup.receiver.ConnectivityReceiver;
import setup.service.ApiCheckVersionIntentService;
import setup.service.ApiCheckVersionService;
import setup.service.ApiRegisterService;
import setup.service.ApkDownloadService;
import setup.service.ApkInstallService;

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
	
	public UpdateCheckInUtils apiCore = new UpdateCheckInUtils();
	
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
		
		setDbHandlers();
		setServiceHandlers();
		
		ShellCommands.triggerNeedForRootAccess(this);
		
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
			
			this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
		}
	}
	
	private void setDbHandlers() {

	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ApiCheckVersion", ApiCheckVersionService.class);
		this.rfcxServiceHandler.addService("ApiCheckVersionIntentService", ApiCheckVersionIntentService.class);
		this.rfcxServiceHandler.addService("ApiRegister", ApiRegisterService.class);
		
		this.rfcxServiceHandler.addService("ApkInstall", ApkInstallService.class);
		this.rfcxServiceHandler.addService("ApkDownload", ApkDownloadService.class);
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
	
	    
}
