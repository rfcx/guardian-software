package org.rfcx.guardian.audio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.ExtAudioRecorderModified;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class AudioCaptureService extends Service {

	private static final String TAG = AudioCaptureService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioCapture audioCapture;

	private RfcxGuardian app = null;
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
		app = (RfcxGuardian) getApplication();
		
		if (app.audioCore.cacheDir == null) {
			app.audioCore.wavDir = app.getApplicationContext().getFilesDir().getPath()+"/wav";
			app.audioCore.cacheDir = app.audioCore.wavDir+"/cache";
			(new File(app.audioCore.cacheDir)).mkdirs();
			app.audioCore.preEncodeWavDir = app.audioCore.wavDir+"/pre_encode";
			(new File(app.audioCore.preEncodeWavDir)).mkdirs();
			app.audioCore.postEncodeWavDir = app.audioCore.wavDir+"/post_encode";
			(new File(app.audioCore.postEncodeWavDir)).mkdirs();
		}
		
		File[] oldWavFiles;
		oldWavFiles = (new File(app.audioCore.cacheDir)).listFiles();
		for (File oldWavFile : oldWavFiles) { try { oldWavFile.delete(); } catch (Exception e) { Log.e(TAG,e.toString()); } }
		oldWavFiles = (new File(app.audioCore.preEncodeWavDir)).listFiles();
		for (File oldWavFile : oldWavFiles) { try { oldWavFile.delete(); } catch (Exception e) { Log.e(TAG,e.toString()); } }
		oldWavFiles = (new File(app.audioCore.postEncodeWavDir)).listFiles();
		for (File oldWavFile : oldWavFiles) { try { oldWavFile.delete(); } catch (Exception e) { Log.e(TAG,e.toString()); } }
		
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
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			AudioCore audioCore = app.audioCore;
			try {
				
				while (audioCaptureService.runFlag) {
					try {
						
						audioRecorder = ExtAudioRecorderModified.getInstance();
						String fileName = Calendar.getInstance().getTimeInMillis()+".wav";
				        audioRecorder.setOutputFile(app.audioCore.cacheDir+"/"+fileName);
				        audioRecorder.prepare();
				        audioRecorder.start();
						Thread.sleep(audioCore.CAPTURE_LOOP_PERIOD_MS);
						audioRecorder.stop();
						audioRecorder.release();
						(new File(app.audioCore.cacheDir+"/"+fileName)).renameTo(new File(app.audioCore.preEncodeWavDir+"/"+fileName));
					
					} catch (Exception e) {
						e.printStackTrace();
						audioCaptureService.runFlag = false;
						app.isServiceRunning_AudioCapture = false;
					}
				}
				if (app.verboseLogging) Log.d(TAG, "Stopping service: "+TAG);
				audioRecorder.stop();
				audioRecorder.release();
				
			} catch (Exception e) {
				e.printStackTrace();
				audioCaptureService.runFlag = false;
				app.isServiceRunning_AudioCapture = false;
			}
		}
	}

}
