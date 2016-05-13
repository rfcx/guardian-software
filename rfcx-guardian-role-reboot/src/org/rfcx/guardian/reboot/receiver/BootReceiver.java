package org.rfcx.guardian.reboot.receiver;

import java.util.Calendar;

import org.rfcx.guardian.reboot.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardian) context.getApplicationContext()).rebootDb.dbReboot.insert(Calendar.getInstance().getTimeInMillis());
//		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices();
	}

}

