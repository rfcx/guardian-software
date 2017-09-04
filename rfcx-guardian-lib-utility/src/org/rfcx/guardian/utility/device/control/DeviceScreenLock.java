package org.rfcx.guardian.utility.device.control;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class DeviceScreenLock {
	
	public DeviceScreenLock(String appRole) {
		this.logTag = this.logTag = (new StringBuilder()).append("Rfcx-").append(appRole).append("-").append(DeviceScreenLock.class.getSimpleName()).toString();
	}
	
	private String logTag = (new StringBuilder()).append("Rfcx-Utils-").append(DeviceScreenLock.class.getSimpleName()).toString();
	
	private WakeLock wakeLock = null;
	private KeyguardManager.KeyguardLock keyguardLock = null;

	public void unLockScreen(Context context) {
		
		KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); 
		this.keyguardLock = keyguardManager.newKeyguardLock("RfcxKeyguardLock");
		this.keyguardLock.disableKeyguard();

		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE); 
		this.wakeLock = powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
		        | PowerManager.ACQUIRE_CAUSES_WAKEUP
		        | PowerManager.ON_AFTER_RELEASE,
		        "RfcxWakeLock");
		this.wakeLock.acquire();
		Log.d(this.logTag,"KeyGuardLock disabled & WakeLock set.");
	}
	
	public void releaseWakeLock() {
		if (this.wakeLock != null) {
			this.wakeLock.release();
			keyguardLock.reenableKeyguard();
			Log.d(this.logTag,"WakeLock released & KeyGuardLock enabled.");
		}
	}
	
}
