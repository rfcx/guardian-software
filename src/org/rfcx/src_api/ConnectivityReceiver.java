package org.rfcx.src_api;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onReceive()"); }
		final boolean isConnected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		Log.d(TAG, "are we connected: "+ isConnected);

	}

}
