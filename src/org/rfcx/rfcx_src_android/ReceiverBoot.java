package org.rfcx.rfcx_src_android;

import org.rfcx.src_state.ArduinoService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReceiverBoot extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		context.startService(new Intent(context, ArduinoService.class));
		context.startService(new Intent(context, ServiceAudioCapture.class));
		
		Log.d("BootReceiver", "onReceived()");
	}

}
