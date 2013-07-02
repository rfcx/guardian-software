package audio;

import javaFlacEncoder.FLAC_FileEncoder;

import org.rfcx.src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioEncodeService extends Service {

	private static final String TAG = AudioEncodeService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioEncode audioEncode;
	
	private static final int DELAY = 1000;
	
	private RfcxSource app = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioEncode = new AudioEncode();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		app = (RfcxSource) getApplication();
		if (app.verboseLogging) Log.d(TAG, "Starting service: "+TAG);
		app.isServiceRunning_AudioEncode = true;
		this.audioEncode.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isServiceRunning_AudioEncode = false;
		this.audioEncode.interrupt();
		this.audioEncode = null;
	}	
	
	private class AudioEncode extends Thread {

		public AudioEncode() {
			super("AudioEncodeService-AudioEncode");
		}
	
		@Override
		public void run() {
			AudioEncodeService audioEncodeService = AudioEncodeService.this;
			app = (RfcxSource) getApplicationContext();
			AudioCore audioCore = app.audioCore;
			try {
				while (audioEncodeService.runFlag) {
					while (audioCore.pcmBufferLength() > 2) {
						FLAC_FileEncoder flacEncoder = new FLAC_FileEncoder();
//						audioCore.addSpectrum();
					}
					Thread.sleep(DELAY);
				}
				if (app.verboseLogging) Log.d(TAG, "Stopping service: "+TAG);
			} catch (InterruptedException e) {
				Log.e(TAG, "InterruptedException");
				audioEncodeService.runFlag = false;
				app.isServiceRunning_AudioEncode = false;
			} catch (Exception e) {
				Log.e(TAG, "Exception");
				audioEncodeService.runFlag = false;
				app.isServiceRunning_AudioEncode = false;
			}
		}
		
	}
	
	
}
