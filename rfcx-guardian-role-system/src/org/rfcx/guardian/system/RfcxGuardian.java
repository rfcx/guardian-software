package org.rfcx.guardian.system;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.rfcx.guardian.system.database.DataTransferDb;
import org.rfcx.guardian.system.database.DeviceStateDb;
import org.rfcx.guardian.system.database.ScreenShotDb;
import org.rfcx.guardian.system.device.DeviceCpuUsage;
import org.rfcx.guardian.system.service.DeviceScreenShotService;
import org.rfcx.guardian.system.service.DeviceSensorService;
import org.rfcx.guardian.system.service.DeviceStateService;
import org.rfcx.guardian.system.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceNetworkStats;
import org.rfcx.guardian.utility.device.DeviceScreenLock;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RfcxGuardian extends Application {

	public String version;
	Context context;
	
	public static final String APP_ROLE = "System";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	
	// database access helpers
	public DeviceStateDb deviceStateDb = null;
	public DataTransferDb dataTransferDb = null;
	public ScreenShotDb screenShotDb = null;
	
	// for triggering and stopping services and intentservices
	public RfcxServiceHandler rfcxServiceHandler = new RfcxServiceHandler();
	
	public DeviceBattery deviceBattery = new DeviceBattery();
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	public DeviceNetworkStats deviceNetworkStats = new DeviceNetworkStats();

	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceId = (new RfcxDeviceId()).init(getApplicationContext());
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(getApplicationContext(), TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		setServiceHandlers();
				
		initializeRoleServices(getApplicationContext());
		
		List<String[]> svcToRun = new ArrayList<String[]>();
		svcToRun.add(new String[] { "DeviceState" });
		svcToRun.add(new String[] { "DeviceSensor" });
		svcToRun.add(new String[] { "ScreenShot" });
		this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceTrigger2", svcToRun);
		
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {
		
	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices(Context context) {
		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceTrigger")) {
		
			try {
				// Service Monitor
				this.rfcxServiceHandler.triggerIntentService("ServiceMonitor", 0, (3 * this.rfcxPrefs.getPrefAsInt("audio_cycle_duration")));
//				triggerIntentService("ServiceMonitor", 
//						System.currentTimeMillis(),
//						3 * this.rfcxPrefs.getPrefAsInt("audio_cycle_duration")
//						);
				
				// background service for gathering system stats
				this.rfcxServiceHandler.triggerService("DeviceState", true);
				
				// background service for gathering sensor stats
				this.rfcxServiceHandler.triggerService("DeviceSensor", true);
				
				// background service for taking screenshots
				this.rfcxServiceHandler.triggerService("ScreenShot", true);
				
				this.rfcxServiceHandler.setAbsoluteRunState("OnLaunchServiceTrigger", true);
				
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			}
		} else {
			Log.w(TAG, "Service Trigger has already run...");
		}
	}
	
	public void triggerIntentService(String intentServiceName, long startTimeMillis, int repeatIntervalSeconds) {
		Context context = getApplicationContext();
		if (startTimeMillis == 0) { startTimeMillis = System.currentTimeMillis(); }
		long repeatInterval = 300000;
		try { repeatInterval = repeatIntervalSeconds*1000; } catch (Exception e) { e.printStackTrace(); }
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if (intentServiceName.equals("ServiceMonitor")) {
			if (!this.rfcxServiceHandler.isRunning("ServiceMonitor")) {
				PendingIntent monitorServiceIntent = PendingIntent.getService(context, -1, new Intent(context, ServiceMonitorIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				if (repeatIntervalSeconds == 0) { alarmManager.set(AlarmManager.RTC, startTimeMillis, monitorServiceIntent);
				} else { alarmManager.setInexactRepeating(AlarmManager.RTC, startTimeMillis, repeatInterval, monitorServiceIntent); }
			} else { Log.w(TAG, "Repeating IntentService 'ServiceMonitor' is already running..."); }
		} else {
			Log.w(TAG, "No IntentService named '"+intentServiceName+"'.");
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version);
		this.deviceStateDb = new DeviceStateDb(this,versionNumber);
		this.dataTransferDb = new DataTransferDb(this,versionNumber);
		this.screenShotDb = new ScreenShotDb(this,versionNumber);
	}
	
	private void setServiceHandlers() {
		this.rfcxServiceHandler.init(getApplicationContext(), APP_ROLE);
		this.rfcxServiceHandler.addService("DeviceState", DeviceStateService.class);
		this.rfcxServiceHandler.addService("DeviceSensor", DeviceSensorService.class);
		this.rfcxServiceHandler.addService("ScreenShot", DeviceScreenShotService.class);
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitorIntentService.class);
	}
 

	
	
}
