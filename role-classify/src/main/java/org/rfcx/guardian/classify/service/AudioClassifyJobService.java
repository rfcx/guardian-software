package org.rfcx.guardian.classify.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.classify.utils.AudioClassifyUtils;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.List;

public class AudioClassifyJobService extends Service {

	public static final String SERVICE_NAME = "AudioClassifyJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioClassifyJobSvc audioClassifyJobSvc;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioClassifyJobSvc = new AudioClassifyJobSvc();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.audioClassifyJobSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxSvc.setRunState(SERVICE_NAME, false);
		this.audioClassifyJobSvc.interrupt();
		this.audioClassifyJobSvc = null;
	}
	
	private class AudioClassifyJobSvc extends Thread {

		public AudioClassifyJobSvc() {
			super("AudioClassifyJobService-AudioClassifyJob");
		}
		
		@Override
		public void run() {
			AudioClassifyJobService audioClassifyJobInstance = AudioClassifyJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxSvc.reportAsActive(SERVICE_NAME);

			try {

				List<String[]> latestQueuedAudioFilesToClassify = app.audioClassifyDb.dbQueued.getAllRows();
				if (latestQueuedAudioFilesToClassify.size() == 0) { Log.d(logTag, "No classification jobs are currently queued."); }
				long audioCycleDuration = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 1000;
				AudioClassifyUtils.cleanupClassifyDirectory( context, latestQueuedAudioFilesToClassify, Math.round( 3.0 * audioCycleDuration ) );

				for (String[] latestQueuedAudioToClassify : latestQueuedAudioFilesToClassify) {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					// only proceed with classify process if there is a valid queued audio file in the database
					if (latestQueuedAudioToClassify[0] != null) {

						String classifierId = latestQueuedAudioToClassify[2];
						String clsfrVersion = latestQueuedAudioToClassify[3];
						int clsfrSampleRate = Integer.parseInt(latestQueuedAudioToClassify[5]);
						float clsfrWindowSize = Float.parseFloat(latestQueuedAudioToClassify[8]);
						float clsfrStepSize = Float.parseFloat(latestQueuedAudioToClassify[9]);
						String clsfrClassifications = latestQueuedAudioToClassify[10];
						String clsfLoggingSummary = classifierId + ", v" + clsfrVersion + ", " + clsfrClassifications + ", " + Math.round(clsfrSampleRate/1000) + "kHz, " + clsfrWindowSize + ", " + clsfrStepSize;
						String audioId = latestQueuedAudioToClassify[1];
						long audioStartsAt = Long.parseLong(latestQueuedAudioToClassify[1]);
						String audioOrigRelativePath = latestQueuedAudioToClassify[6];

						if (Integer.parseInt(latestQueuedAudioToClassify[11]) >= AudioClassifyUtils.CLASSIFY_FAILURE_SKIP_THRESHOLD) {

							Log.e(logTag, "Skipping Audio Classify Job for " + audioId + " after " + AudioClassifyUtils.CLASSIFY_FAILURE_SKIP_THRESHOLD + " failed attempts.");

							app.audioClassifyDb.dbQueued.deleteSingleRow(audioId, classifierId);

						} else {

							app.audioClassifyDb.dbQueued.incrementSingleRowAttempts(audioId, classifierId);

							String clsfrFilePath = RfcxClassifierFileUtils.getClassifierFileLocation_Active(context, Long.parseLong(classifierId));
							String clsfrRelativeFilePath = RfcxAssetCleanup.conciseFilePath(clsfrFilePath, RfcxGuardian.APP_ROLE);
							Uri clsfrFileOriginUri = RfcxComm.getFileUri("guardian", clsfrRelativeFilePath);
							AudioClassifyUtils.cleanupClassifierDirectory( context, new String[] { clsfrFilePath }, Math.round( 24 * 60 * 60 * 1000 ) );

							if (!FileUtils.exists(clsfrFilePath) && !RfcxComm.getFileRequest( clsfrFileOriginUri, clsfrFilePath, app.getResolver())) {

								Log.e(logTag, "Classifier file could not be found or retrieved: " + clsfrRelativeFilePath);

							} else {

								boolean isClassifierInitialized = app.audioClassifyUtils.confirmOrLoadClassifier(classifierId, clsfrFilePath, clsfrSampleRate, clsfrWindowSize, clsfrStepSize, clsfrClassifications);

								if (!isClassifierInitialized) {

									Log.e(logTag, "Classifier could not be initialized: " + clsfLoggingSummary );

								} else {

									String audioFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreClassify(context, Long.parseLong(audioId), "wav", clsfrSampleRate, null);
									String audioRelativeFilePath = RfcxAssetCleanup.conciseFilePath(audioFilePath, RfcxGuardian.APP_ROLE);
									Uri audioFileOriginUri = RfcxComm.getFileUri("guardian", audioOrigRelativePath);

									if (!FileUtils.exists(audioFilePath) && !RfcxComm.getFileRequest( audioFileOriginUri, audioFilePath, app.getResolver())) {

										Log.e(logTag, "Audio file could not be found or retrieved: " + audioRelativeFilePath);

									} else {

										Log.i(logTag, "Beginning Audio Classify Job - Audio: " + audioId + " - Classifier: " + clsfLoggingSummary);

										long classifyStartTime = System.currentTimeMillis();
										List<float[]> classifyOutput = app.audioClassifyUtils.getClassifier(classifierId).classify(audioFilePath);
										Log.i(logTag, "Completed Audio Classify Job - " + DateTimeUtils.timeStampDifferenceFromNowAsReadableString(classifyStartTime) + " - Audio: " + audioId + " - Classifier: " + classifierId);

										JSONObject classifyOutputJson = app.audioClassifyUtils.classifyOutputAsJson(classifierId, audioId, audioStartsAt, classifyOutput);
										classifyOutputJson.put("classify_duration", (System.currentTimeMillis()-classifyStartTime)+"" );
										classifyOutputJson.put("audio_size", FileUtils.getFileSizeInBytes(audioFilePath)+"" );

										app.audioClassifyUtils.sendClassifyOutputToGuardianRole(classifyOutputJson);

										app.audioClassifyDb.dbQueued.deleteSingleRow(audioId, classifierId);
									}
								}
							}
						}

					} else {
						Log.d(logTag, "Queued classify job definitoin from database is invalid.");

					}
				}

// 		maybe some code to queue sending the detections back to the guardian role

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				audioClassifyJobInstance.runFlag = false;
			}
			
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			audioClassifyJobInstance.runFlag = false;

		}
	}
	

}
