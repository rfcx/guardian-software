package org.rfcx.src_audio;

import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_api.ApiComm;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


public class AudioProcessServiceInt extends IntentService {

	private static final String TAG = AudioProcessServiceInt.class.getSimpleName();
	
	public static final String SRC_AUDIO_PROCESS = "org.rfcx.src_android.SRC_AUDIO_PROCESS";
	public static final String RECEIVE_AUDIO_PROCESS_NOTIFICATIONS = "org.rfcx.src_android.RECEIVE_AUDIO_PROCESS_NOTIFICATIONS";
	
	public AudioProcessServiceInt() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Log.d(TAG, "ApiCommIntentService started");
		RfcxSource rfcxSource = (RfcxSource) getApplication();
		AudioState audioState = rfcxSource.audioState;

		Intent intent = new Intent(SRC_AUDIO_PROCESS);
		sendBroadcast(intent, RECEIVE_AUDIO_PROCESS_NOTIFICATIONS);
		
		try {
			Log.d(TAG, "ProcessAudio is a go: "+audioState.pcmBufferLength());
			while (audioState.pcmBufferLength() > 2) {
				audioState.addSpectrum();
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		
	}
	
	
	
}
