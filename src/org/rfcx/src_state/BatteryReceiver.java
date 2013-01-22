package org.rfcx.src_state;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {

	private static final String TAG = BatteryReceiver.class.getSimpleName();
		
	@Override
	public void onReceive(Context context, Intent intent) {
        setBatteryState(context, intent);
        Log.d(TAG,"onReceive()");
	}
	
	private void setBatteryState(Context context, Intent intent) {
		RfcxSource app = (RfcxSource) context.getApplicationContext();
		app.batteryState.setLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		app.batteryState.setScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
        Log.d(TAG,"battery percentage: "+app.batteryState.getPercent() + "%");
	}
}
