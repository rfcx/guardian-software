package org.rfcx.guardian.guardian.audio.classify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioClassifyJobService extends Service {

	private static final String SERVICE_NAME = "AudioClassifyJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioClassifyJob audioClassifyJob;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioClassifyJob = new AudioClassifyJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioClassifyJob.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioClassifyJob.interrupt();
		this.audioClassifyJob = null;
	}
	
	private class AudioClassifyJob extends Thread {

		public AudioClassifyJob() {
			super("AudioClassifyJobService-AudioClassifyJob");
		}
		
		@Override
		public void run() {
			AudioClassifyJobService audioClassifyJobInstance = AudioClassifyJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			try {

				List<String[]> latestQueuedAudioFilesToClassify = app.audioClassifyDb.dbQueued.getAllRows();
				if (latestQueuedAudioFilesToClassify.size() == 0) { Log.d(logTag, "No audio files are queued to be classified."); }
				long audioCycleDuration = app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
				AudioClassify2Utils.cleanupClassifyDirectory( context, latestQueuedAudioFilesToClassify, audioCycleDuration );

				for (String[] latestQueuedAudioToClassify : latestQueuedAudioFilesToClassify) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					// only proceed with classify process if there is a valid queued audio file in the database
					if (latestQueuedAudioToClassify[0] != null) {

						String timestamp = latestQueuedAudioToClassify[1];
						String captureFileExt = latestQueuedAudioToClassify[2];
						int captureSampleRate = Integer.parseInt(latestQueuedAudioToClassify[11]);
						int classifierSampleRate = Integer.parseInt(latestQueuedAudioToClassify[4]);
						File preClassifyFile = new File(latestQueuedAudioToClassify[10]);


						preClassifyFile = AudioCaptureUtils.checkOrCreateReSampledWav(context, "classify", preClassifyFile.getAbsolutePath(), Long.parseLong(timestamp), captureFileExt, classifierSampleRate);


//						AudioClassifyUtils audioClassifyUtils = new AudioClassifyUtils(context);
//
//
//						audioClassifyUtils.classifyAudio(audioFile);



						app.audioClassifyDb.dbQueued.deleteSingleRow(timestamp);


					} else {
						Log.d(logTag, "Queued audio file entry in database is invalid.");

					}
				}

					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioClassifyJobInstance.runFlag = false;
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			audioClassifyJobInstance.runFlag = false;

		}
	}
	

}
