package org.rfcx.guardian.api;

import java.util.Calendar;

import org.rfcx.guardian.api.api.ApiWebCheckIn;
import org.rfcx.guardian.api.database.CheckInDb;
import org.rfcx.guardian.utility.device.DeviceAirplaneMode;
import org.rfcx.guardian.api.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.api.receiver.ConnectivityReceiver;
import org.rfcx.guardian.api.service.ApiCheckInService;
import org.rfcx.guardian.api.service.ApiCheckInTrigger;
import org.rfcx.guardian.api.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxConstants;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.device.DeviceBattery;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Api";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	
	// database access helpers
	public CheckInDb checkInDb = null;
	
	public boolean isConnected = false;
	public long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	public long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	
	// for viewing and controlling airplane mode
	public DeviceAirplaneMode airplaneMode = new DeviceAirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();

	// for transmitting api data
	public ApiWebCheckIn apiWebCheckIn = new ApiWebCheckIn();
	
	// for checking battery level
	public DeviceBattery deviceBattery = new DeviceBattery();

	public boolean isRunning_ApiCheckIn = false;
	public boolean isRunning_ApiCheckInTrigger = false;
	
	public boolean isRunning_ServiceMonitor = false;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {
		
		super.onCreate();
		
		this.rfcxDeviceId = (new RfcxDeviceId()).init(getApplicationContext());
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(getApplicationContext(), TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		
		apiWebCheckIn.init(this);
		
		this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {
		
	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				
				// start the api triggers
				triggerService("ApiCheckInTrigger", true);
				// Service Monitor
				triggerIntentService("ServiceMonitor", 
						System.currentTimeMillis(),
						3 * Math.round( this.rfcxPrefs.getPrefAsInt("audio_cycle_duration") / 1000 )
						);

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
		if (serviceName.equals("ApiCheckIn")) {
			if (!this.isRunning_ApiCheckIn || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckInService.class));
				context.startService(new Intent(context, ApiCheckInService.class));
			}
		} else if (serviceName.equals("ApiCheckInTrigger")) {
			if (!this.isRunning_ApiCheckInTrigger || forceReTrigger) {
				context.stopService(new Intent(context, ApiCheckInTrigger.class));
				context.startService(new Intent(context, ApiCheckInTrigger.class));
			} else { Log.w(TAG, "Service '"+serviceName+"' is already running..."); }
		} else {
			Log.w(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	public void stopService(String serviceName) {
		context = getApplicationContext();		
		if (serviceName.equals("ApiCheckIn")) {
			context.stopService(new Intent(context, ApiCheckInService.class));
		} else if (serviceName.equals("ApiCheckInTrigger")) {
			context.stopService(new Intent(context, ApiCheckInTrigger.class));
		} else {
			Log.e(TAG, "There is no service named '"+serviceName+"'.");
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version, TAG);
		this.checkInDb = new CheckInDb(this,versionNumber);
	}
}
