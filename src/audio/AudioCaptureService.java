package audio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import org.rfcx.src_android.RfcxSource;

import utility.ExtAudioRecorderModified;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class AudioCaptureService extends Service {

	private static final String TAG = AudioCaptureService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioCapture audioCapture;

	private RfcxSource app = null;
    ExtAudioRecorderModified audioRecorder = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCapture = new AudioCapture();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		app = (RfcxSource) getApplication();
		if (app.audioCore.cacheDir == null) {
			app.audioCore.wavDir = app.getApplicationContext().getFilesDir().getPath()+"/wav";
			app.audioCore.cacheDir = app.audioCore.wavDir+"/cache";
			(new File(app.audioCore.cacheDir)).mkdirs();
		}
		if (app.verboseLogging) Log.d(TAG, "Starting service: "+TAG);
		app.isServiceRunning_AudioCapture = true;
		this.audioCapture.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isServiceRunning_AudioCapture = false;
		this.audioCapture.interrupt();
		this.audioCapture = null;
	}

	private class AudioCapture extends Thread {

		public AudioCapture() {
			super("AudioCaptureService-AudioCapture");
		}

		@Override
		public void run() {
			AudioCaptureService audioCaptureService = AudioCaptureService.this;
			app = (RfcxSource) getApplicationContext();
			AudioCore audioCore = app.audioCore;
			try {
				
				while (audioCaptureService.runFlag) {
					try {
						
						audioRecorder = ExtAudioRecorderModified.getInstance();
						String fileName = Calendar.getInstance().getTimeInMillis()+".wav";
				        audioRecorder.setOutputFile(app.audioCore.cacheDir+"/"+fileName);
				        audioRecorder.prepare();
				        audioRecorder.start();
						Thread.sleep(60000);
						audioRecorder.stop();
						audioRecorder.release();
						(new File(app.audioCore.cacheDir+"/"+fileName)).renameTo(new File(app.audioCore.wavDir+"/"+fileName));
						
					} catch (Exception e) {
						e.printStackTrace();
						audioCaptureService.runFlag = false;
						app.isServiceRunning_AudioCapture = false;
					}
				}
				if (app.verboseLogging) Log.d(TAG, "Stopping service: "+TAG);
				audioRecorder.stop();
				audioRecorder.release();
//			} catch (InterruptedException e) {
//				Log.e(TAG, "InterruptedException");
//				audioCaptureService.runFlag = false;
//				app.isServiceRunning_AudioCapture = false;
			} catch (Exception e) {
				e.printStackTrace();
				audioCaptureService.runFlag = false;
				app.isServiceRunning_AudioCapture = false;
			}
		}
	}

}
