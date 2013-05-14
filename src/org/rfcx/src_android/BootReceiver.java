package org.rfcx.src_android;

import org.rfcx.src_api.ApiCommService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String TAG = BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent callingIntent) {
		if (RfcxSource.VERBOSE) Log.d(TAG, "BroadcastReceiver: "+TAG);
		((RfcxSource) context.getApplicationContext()).launchAllServices(context);
		
		Intent intent = new Intent(context, ApiCommService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), 300000, pendingIntent);
	}

}
