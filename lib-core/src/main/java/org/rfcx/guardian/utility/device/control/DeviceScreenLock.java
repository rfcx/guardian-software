package org.rfcx.guardian.utility.device.control;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceScreenLock {
	
	public DeviceScreenLock(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceScreenLock");
	}
	
	private String logTag;
	
	private WakeLock wakeLock = null;
	private KeyguardManager.KeyguardLock keyguardLock = null;

	@SuppressLint("MissingPermission")
	public void unLockScreen(Context context) {
		
		KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); 
		this.keyguardLock = keyguardManager.newKeyguardLock("RfcxKeyguardLock");
		this.keyguardLock.disableKeyguard();

		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		this.wakeLock = powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
		        | PowerManager.ACQUIRE_CAUSES_WAKEUP
		        | PowerManager.ON_AFTER_RELEASE,
		        "Rfcx:DeviceWakeLock");
		this.wakeLock.acquire(60 * 1000L );
		Log.d(this.logTag,"KeyGuardLock disabled & WakeLock set.");
	}
	
	@SuppressLint("MissingPermission")
	public void releaseWakeLock() {
		if (this.wakeLock != null) {
			this.wakeLock.release();
			keyguardLock.reenableKeyguard();
			Log.d(this.logTag,"WakeLock released & KeyGuardLock enabled.");
		}
	}


	public static boolean isScreenOn(Context context) {
		return ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
	}
	
}
