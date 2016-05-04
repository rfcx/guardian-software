package org.rfcx.guardian.utility.device;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class DeviceScreenLock {

	private static final String TAG = "Rfcx-Utils-"+DeviceScreenLock.class.getSimpleName();
	
	private WakeLock wakeLock = null;
	private KeyguardManager.KeyguardLock keyguardLock = null;

	public void unLockScreen(Context context) {
		
		KeyguardManager keyguardManager = (KeyguardManager) context.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); 
		this.keyguardLock = keyguardManager.newKeyguardLock("RfcxKeyguardLock");
		this.keyguardLock.disableKeyguard();

		PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE); 
		this.wakeLock = powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
		        | PowerManager.ACQUIRE_CAUSES_WAKEUP
		        | PowerManager.ON_AFTER_RELEASE,
		        "RfcxWakeLock");
		this.wakeLock.acquire();
		Log.d(TAG,"KeyGuardLock disabled & WakeLock set.");
	}
	
	public void releaseWakeLock() {
		if (this.wakeLock != null) {
			this.wakeLock.release();
			keyguardLock.reenableKeyguard();
			Log.d(TAG,"WakeLock released & KeyGuardLock enabled.");
		}
	}
	
}
