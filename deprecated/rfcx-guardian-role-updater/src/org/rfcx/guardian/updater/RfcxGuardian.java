package org.rfcx.guardian.updater;

import org.rfcx.guardian.updater.api.ApiCore;
import org.rfcx.guardian.updater.receiver.ConnectivityReceiver;
import org.rfcx.guardian.updater.service.ApiCheckVersionIntentService;
import org.rfcx.guardian.updater.service.ApiCheckVersionService;
import org.rfcx.guardian.updater.service.DownloadFileService;
import org.rfcx.guardian.updater.service.InstallAppService;
import org.rfcx.guardian.updater.service.RebootTriggerIntentService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Updater";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public static final String targetAppRoleApiEndpoint = "all";
	public String targetAppRole = "";
		
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	public ApiCore apiCore = new ApiCore();
	
	// for checking battery level
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);

	public long lastApiCheckTriggeredAt = System.currentTimeMillis();
	
	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, TAG);
		this.rfcxPrefs.writeVersionToFile(this.version);
		
		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		apiCore.setApiCheckVersionEndpoint(this.rfcxDeviceId.getDeviceGuid());
		
		setDbHandlers();
		setServiceHandlers();
		
		initializeRoleServices();
	}
	
	public void onTerminate() {
		super.onTerminate();
		this.unregisterReceiver(connectivityReceiver);
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices() {
		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {
			this.rfcxServiceHandler.triggerServiceSequence(
				"OnLaunchServiceSequence", 
					new String[] { 
						"ApiCheckVersionIntentService"
								+"|"+DateTimeUtils.nowPlusThisLong("00:05:00").getTimeInMillis() // waits five minutes before running
								+"|"+this.rfcxPrefs.getPrefAsString("install_cycle_duration")
						}, 
				true);
		}
	}
	
	private void setDbHandlers() {

	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ApiCheckVersion", ApiCheckVersionService.class);
		this.rfcxServiceHandler.addService("ApiCheckVersionIntentService", ApiCheckVersionIntentService.class);
		this.rfcxServiceHandler.addService("InstallApp", InstallAppService.class);
		this.rfcxServiceHandler.addService("DownloadFile", DownloadFileService.class);
		this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerIntentService.class);
	}


    
}
