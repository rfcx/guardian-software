package org.rfcx.rfcx_src_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReceiverBoot extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		context.startService(new Intent(context, org.rfcx.src_state.ArduinoService.class));
		context.startService(new Intent(context, org.rfcx.src_audio.ServiceAudioCapture.class));
		
		Log.d("BootReceiver", "onReceived()");
	}

}
