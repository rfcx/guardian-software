package org.rfcx.src_api;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
        RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		final boolean isConnected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		rfcxSource.apiComm.setConnectivity(isConnected);
		if (RfcxSource.VERBOSE) Log.d(TAG, "BroadcastReceiver: "+TAG+" - Connectivity: "+isConnected);
		if (isConnected) {
			rfcxSource.apiComm.sendData(context);
		}
	}

}
