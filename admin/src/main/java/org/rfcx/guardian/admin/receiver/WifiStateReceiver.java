package org.rfcx.guardian.admin.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class WifiStateReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, WifiStateReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		final String intentAction = intent.getAction();

       if (intentAction.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

       		final int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
			boolean prefsAdminEnableWifi = app.rfcxPrefs.getPrefAsBoolean("admin_enable_wifi");

            if (		(prefsAdminEnableWifi
						&&	(	(wifiState == WifiManager.WIFI_STATE_DISABLING)
						//	||	(wifiState == WifiManager.WIFI_STATE_DISABLED)
						))
				||		(!prefsAdminEnableWifi
						&&	(	(wifiState == WifiManager.WIFI_STATE_ENABLING)
						//	||	(wifiState == WifiManager.WIFI_STATE_ENABLED)
						))
				) {
					app.rfcxServiceHandler.triggerService("WifiStateSet", false);
            }
        }
		
	}

}
