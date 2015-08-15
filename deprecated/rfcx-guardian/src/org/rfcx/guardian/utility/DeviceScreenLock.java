package org.rfcx.guardian.utility;

import org.rfcx.guardian.RfcxGuardian;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class DeviceScreenLock {

	private static final String TAG = "RfcxGuardian-"+DeviceScreenLock.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	private RfcxGuardian app = null;
	private WakeLock wakeLock = null;
	private /*final*/ KeyguardManager.KeyguardLock keyguardLock = null;

	public void unLockScreen(Context context) {
		app = (RfcxGuardian) context.getApplicationContext();
		
		KeyguardManager keyguardManager = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE); 
		this.keyguardLock = keyguardManager.newKeyguardLock("RfcxKeyguardLock");
		this.keyguardLock.disableKeyguard();
		Log.d(TAG,"Disabling KeyGuardLock");

		PowerManager powerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE); 
		this.wakeLock = powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
		        | PowerManager.ACQUIRE_CAUSES_WAKEUP
		        | PowerManager.ON_AFTER_RELEASE,
		        "RfcxWakeLock");
		Log.d(TAG,"Setting WakeLock");
		this.wakeLock.acquire();
	}
	
	public void releaseWakeLock() {
		if (this.wakeLock != null) {
			this.wakeLock.release();
			Log.d(TAG,"Releasing WakeLock");
			keyguardLock.reenableKeyguard();
			Log.d(TAG,"Re-Enabling KeyGuardLock");
		}
	}
	
}
