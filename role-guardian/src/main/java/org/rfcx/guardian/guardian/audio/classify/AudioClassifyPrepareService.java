package org.rfcx.guardian.guardian.audio.classify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeUtils;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.File;
import java.util.List;

import static androidx.core.content.FileProvider.getUriForFile;

public class AudioClassifyPrepareService extends Service {

	public static final String SERVICE_NAME = "AudioClassifyPrepare";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyPrepareService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioClassifyPrepare audioClassifyPrepare;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioClassifyPrepare = new AudioClassifyPrepare();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioClassifyPrepare.start();
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
		this.audioClassifyPrepare.interrupt();
		this.audioClassifyPrepare = null;
	}
	
	private class AudioClassifyPrepare extends Thread {

		public AudioClassifyPrepare() {
			super("AudioClassifyPrepareService-AudioClassifyPrepare");
		}
		
		@Override
		public void run() {
			AudioClassifyPrepareService audioClassifyPrepareInstance = AudioClassifyPrepareService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			try {

				List<String[]> latestQueuedAudioFilesToClassify = app.audioClassifyDb.dbQueued.getAllRows();
				if (latestQueuedAudioFilesToClassify.size() == 0) { Log.d(logTag, "No classification jobs are currently queued."); }
				long audioCycleDuration = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 1000;
				AudioClassifyUtils.cleanupClassifyDirectory( context, latestQueuedAudioFilesToClassify, Math.round( 1.0 * audioCycleDuration ) );

				for (String[] latestQueuedAudioToClassify : latestQueuedAudioFilesToClassify) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					// only proceed with classify process if there is a valid queued audio file in the database
					if (latestQueuedAudioToClassify[0] != null) {

						String audioId = latestQueuedAudioToClassify[1];
						String classifierId = latestQueuedAudioToClassify[2];
						String classifierVersion = latestQueuedAudioToClassify[3];
						int captureSampleRate = Integer.parseInt(latestQueuedAudioToClassify[4]);
						int classifierSampleRate = Integer.parseInt(latestQueuedAudioToClassify[5]);
						String classifierWindowSize = latestQueuedAudioToClassify[8];
						String classifierStepSize = latestQueuedAudioToClassify[9];
						String classifierClasses = latestQueuedAudioToClassify[10];
						File classifierFile = new File(latestQueuedAudioToClassify[7]);
						File preClassifyAudioFile = new File(latestQueuedAudioToClassify[6]);


						if (!classifierFile.exists()) {

							Log.e(logTag, "Skipping Audio Classify Job because classifier file could not be found."+RfcxAssetCleanup.conciseFilePath(classifierFile.getAbsolutePath(), RfcxGuardian.APP_ROLE));
							app.audioClassifyDb.dbQueued.deleteSingleRow(audioId, classifierId);

						} else if (!preClassifyAudioFile.exists()) {

							Log.e(logTag, "Skipping Audio Classify Job because input audio file could not be found: "+RfcxAssetCleanup.conciseFilePath(preClassifyAudioFile.getAbsolutePath(), RfcxGuardian.APP_ROLE));
							app.audioClassifyDb.dbQueued.deleteSingleRow(audioId, classifierId);

						} else if (Integer.parseInt(latestQueuedAudioToClassify[11]) >= AudioClassifyUtils.CLASSIFY_FAILURE_SKIP_THRESHOLD) {

							Log.e(logTag, "Skipping Audio Classify Job for " + audioId + " after " + AudioEncodeUtils.ENCODE_FAILURE_SKIP_THRESHOLD + " failed attempts.");

							app.audioClassifyDb.dbQueued.deleteSingleRow(audioId, classifierId);

						} else {


							app.audioClassifyDb.dbQueued.incrementSingleRowAttempts(audioId, classifierId);


							preClassifyAudioFile = AudioCaptureUtils.checkOrCreateReSampledWav(context, "classify", preClassifyAudioFile.getAbsolutePath(), Long.parseLong(audioId), "wav", captureSampleRate, classifierSampleRate);

							Log.d(logTag, "Sending Classify Job to Classify Role...");

							app.audioClassifyUtils.queueClassifyJobAcrossRoles(
									audioId, classifierId, classifierVersion, classifierSampleRate,
									RfcxAssetCleanup.conciseFilePath(preClassifyAudioFile.getAbsolutePath(), RfcxGuardian.APP_ROLE),
									RfcxAssetCleanup.conciseFilePath(classifierFile.getAbsolutePath(), RfcxGuardian.APP_ROLE),
									classifierWindowSize, classifierStepSize, classifierClasses
							);

							app.audioClassifyDb.dbQueued.deleteSingleRow(audioId, classifierId);

						}

					} else {
						Log.d(logTag, "Queued classification job in database is invalid.");

					}
				}

					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioClassifyPrepareInstance.runFlag = false;
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			audioClassifyPrepareInstance.runFlag = false;

		}
	}
	

}
