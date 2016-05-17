package org.rfcx.guardian.system;

import java.util.Calendar;

import org.rfcx.guardian.system.database.DataTransferDb;
import org.rfcx.guardian.system.database.DeviceStateDb;
import org.rfcx.guardian.system.database.RebootDb;
import org.rfcx.guardian.system.database.ScreenShotDb;
import org.rfcx.guardian.system.device.DeviceCpuUsage;
import org.rfcx.guardian.system.service.DeviceScreenShotService;
import org.rfcx.guardian.system.service.DeviceSensorService;
import org.rfcx.guardian.system.service.DeviceStateService;
import org.rfcx.guardian.system.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.device.DeviceNetworkStats;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.Context;

public class RfcxGuardian extends Application {

	public String version;
	Context context;
	
	public static final String APP_ROLE = "System";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = null;
	public DataTransferDb dataTransferDb = null;
	public ScreenShotDb screenShotDb = null;
	public RebootDb rebootDb = null;
	
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	public DeviceNetworkStats deviceNetworkStats = new DeviceNetworkStats(APP_ROLE);
	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		setServiceHandlers();
		
		initializeRoleServices();
		
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
						"DeviceState", 
						"DeviceSensor", 
						"ScreenShot",
						"ServiceMonitor"+"|"+"0"+"|"+(3*this.rfcxPrefs.getPrefAsInt("audio_cycle_duration"))
					}, 
				true);
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version);
		this.deviceStateDb = new DeviceStateDb(this,versionNumber);
		this.dataTransferDb = new DataTransferDb(this,versionNumber);
		this.screenShotDb = new ScreenShotDb(this,versionNumber);
		this.rebootDb = new RebootDb(this,versionNumber);
	}
	
	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("DeviceState", DeviceStateService.class);
		this.rfcxServiceHandler.addService("DeviceSensor", DeviceSensorService.class);
		this.rfcxServiceHandler.addService("ScreenShot", DeviceScreenShotService.class);
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitorIntentService.class);
	}
 
}
