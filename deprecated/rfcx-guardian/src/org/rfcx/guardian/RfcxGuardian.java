package org.rfcx.guardian;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;

import org.rfcx.guardian.api.ApiCore;
import org.rfcx.guardian.audio.AudioCore;
import org.rfcx.guardian.carrier.CarrierInteraction;
import org.rfcx.guardian.database.AudioDb;
import org.rfcx.guardian.database.CheckInDb;
import org.rfcx.guardian.database.DataTransferDb;
import org.rfcx.guardian.database.DeviceStateDb;
import org.rfcx.guardian.database.ScreenShotDb;
import org.rfcx.guardian.database.SmsDb;
import org.rfcx.guardian.device.DeviceCpuUsage;
import org.rfcx.guardian.device.DeviceState;
import org.rfcx.guardian.intentservice.CarrierCodeBalance;
import org.rfcx.guardian.intentservice.CarrierCodeTopUp;
import org.rfcx.guardian.intentservice.ServiceMonitor;
import org.rfcx.guardian.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.receiver.ConnectivityReceiver;
import org.rfcx.guardian.service.ApiCheckInService;
import org.rfcx.guardian.service.ApiCheckInTrigger;
import org.rfcx.guardian.service.AudioCaptureService;
import org.rfcx.guardian.service.CarrierCodeService;
import org.rfcx.guardian.service.DeviceCPUTunerService;
import org.rfcx.guardian.service.DeviceStateService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.DeviceAirplaneMode;
import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceScreenLock;
import org.rfcx.guardian.utility.DeviceScreenShot;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.FileUtils;
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
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = "RfcxGuardian-"+RfcxGuardian.class.getSimpleName();
	
	// for interacting with telecom carriers
	public CarrierInteraction carrierInteraction = new CarrierInteraction();
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {
	}
	
	public void appPause() {
	}

	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.i(TAG, "Preference changed: "+key);
	}

}
