package org.rfcx.guardian.utility.device;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class DeviceScreenLock {

	private String logTag = "Rfcx-Utils-"+DeviceScreenLock.class.getSimpleName();
	
	private WakeLock wakeLock = null;
	private KeyguardManager.KeyguardLock keyguardLock = null;
	private Context context = null;
	
	public DeviceScreenLock init(Context context, String roleName) {
		this.context = context;
		this.logTag = "Rfcx-"+roleName+"-"+DeviceScreenLock.class.getSimpleName();
		return this;
	}

	public void unLockScreen() {
		
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
