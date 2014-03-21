package audio;

import java.io.File;
import java.util.Calendar;

import javaFlacEncoder.FLAC_FileEncoder;

import org.rfcx.src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioProcessService extends Service {
	
	private static final String TAG = AudioProcessService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioProcess audioProcess;
	
	private static final int DELAY = 1000;
	
	private RfcxSource app = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioProcess = new AudioProcess();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		app = (RfcxSource) getApplication();
		if (app.audioCore.flacDir == null) { 
			app.audioCore.flacDir = app.getApplicationContext().getFilesDir().getPath()+"/flac";
			(new File(app.audioCore.flacDir)).mkdirs();
		}
		if (app.verboseLogging) Log.d(TAG, "Starting service: "+TAG);
		app.isServiceRunning_AudioProcess = true;
		this.audioProcess.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isServiceRunning_AudioProcess = false;
		this.audioProcess.interrupt();
		this.audioProcess = null;
	}	

	private class AudioProcess extends Thread {

		public AudioProcess() {
			super("AudioProcessService-AudioProcess");
		}
	
		@Override
		public void run() {
			AudioProcessService audioProcessService = AudioProcessService.this;
			app = (RfcxSource) getApplicationContext();
		//	AudioCore audioCore = app.audioCore;
			try {
				while (audioProcessService.runFlag) {
					File[] wavFiles = (new File(app.audioCore.wavDir)).listFiles();
					for (File wavFile : wavFiles) {
						if (app.verboseLogging) { Log.d(TAG,wavFile.getAbsolutePath()); }
						if (!wavFile.isDirectory()) {
							String wavPath = wavFile.getAbsolutePath().toString();
							String timeCode = wavPath.substring(1+wavPath.lastIndexOf("/"),wavPath.lastIndexOf("."));
							if (app.verboseLogging) { Log.d(TAG,"Processing Audio: "+timeCode); }
							FLAC_FileEncoder ffe = new FLAC_FileEncoder();								
							ffe.encode(wavFile, new File(app.audioCore.flacDir+"/"+timeCode+".flac"));
							wavFile.delete();
						}
					}
					
					Thread.sleep(DELAY);
				}
				if (app.verboseLogging) Log.d(TAG, "Stopping service: "+TAG);
			} catch (InterruptedException e) {
				Log.e(TAG, "InterruptedException");
				audioProcessService.runFlag = false;
				app.isServiceRunning_AudioProcess = false;
			} catch (Exception e) {
				Log.e(TAG, "Exception");
				audioProcessService.runFlag = false;
				app.isServiceRunning_AudioProcess = false;
			}
		}
		
	}
	
}
