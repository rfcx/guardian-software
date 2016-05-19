package org.rfcx.guardian.api;

import org.rfcx.guardian.api.api.ApiWebCheckIn;
import org.rfcx.guardian.api.database.CheckInDb;
import org.rfcx.guardian.api.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.api.receiver.ConnectivityReceiver;
import org.rfcx.guardian.api.service.ApiCheckInService;
import org.rfcx.guardian.api.service.ApiCheckInTrigger;
import org.rfcx.guardian.api.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.device.DeviceAirplaneMode;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Api";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	// database access helpers
	public CheckInDb checkInDb = null;
	
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();

	// for transmitting api data
	public ApiWebCheckIn apiWebCheckIn = null;
	
	// for checking battery level
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);
	public DeviceAirplaneMode deviceAirplaneMode = new DeviceAirplaneMode(APP_ROLE);
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, TAG);
		this.rfcxPrefs.writeVersionToFile(this.version);

		this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		setDbHandlers();
		setServiceHandlers();
		
		initializeRoleServices();
		
		this.apiWebCheckIn = new ApiWebCheckIn(this);
	}
	
	public void onTerminate() {
		super.onTerminate();
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
						"ApiCheckInTrigger",
						"ServiceMonitor"+"|"+"0"+"|"+this.rfcxPrefs.getPrefAsString("service_monitor_cycle_duration")
					}, 
				true);
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version);
		this.checkInDb = new CheckInDb(this,versionNumber);
	}
	
	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ApiCheckInTrigger", ApiCheckInTrigger.class);
		this.rfcxServiceHandler.addService("ApiCheckIn", ApiCheckInService.class);
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitorIntentService.class);
	}
}
