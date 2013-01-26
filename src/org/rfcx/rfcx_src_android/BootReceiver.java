package org.rfcx.rfcx_src_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.rfcx.src_audio.AudioCaptureService;
import org.rfcx.src_state.ArduinoService;
import org.rfcx.src_state.DeviceCpuService;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String TAG = BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onReceive()"); }	
		context.startService(new Intent(context, ArduinoService.class));
		context.startService(new Intent(context, AudioCaptureService.class));
		context.startService(new Intent(context, DeviceCpuService.class));
	}

}
