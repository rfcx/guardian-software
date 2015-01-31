package org.rfcx.guardian.utility;

import org.rfcx.guardian.RfcxGuardian;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class DeviceScreenLock {

	private static final String TAG = "RfcxGuardian-"+DeviceScreenLock.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	private RfcxGuardian app = null;

	public void unLockScreen(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		
		KeyguardManager keyguardManager = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE); 
		final KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("RfcxKeyguardLock");
		keyguardLock.disableKeyguard(); 

		PowerManager powerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE); 
		WakeLock wakeLock = powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
		        | PowerManager.ACQUIRE_CAUSES_WAKEUP
		        | PowerManager.ON_AFTER_RELEASE,
		        "RfcxWakeLock");
		wakeLock.acquire();
	}
	
}
