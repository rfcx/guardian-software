package org.rfcx.guardian.classify.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
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
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
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
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
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

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			try {

				String clsfrId = "1234567890";
				String clsfrFilePath = RfcxClassifierFileUtils.classifierActiveDir(context)+"/multiclass-model.tflite";
				int clsfrSampleRate = 12000;
				float clsfrWindowSize = app.rfcxPrefs.getPrefAsFloat(RfcxPrefs.Pref.PREDICTION_WINDOW_SIZE);
				float clsfrStepSize = app.rfcxPrefs.getPrefAsFloat(RfcxPrefs.Pref.PREDICTION_STEP_SIZE);
				String clsfrModelOutput = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.PREDICTION_MODEL_OUTPUT);

				String audioId = "9876543210";
				long audioStartsAt = Long.parseLong(audioId);
				String audioFilePath = RfcxAudioFileUtils.audioClassifyDir(app.getApplicationContext()) + "/chainsaw12000.wav";

				boolean isClassifierLoaded = app.audioClassifyClassicUtils.confirmOrLoadClassifier(clsfrId, clsfrFilePath, clsfrSampleRate, clsfrWindowSize, clsfrStepSize, clsfrModelOutput);

				if (isClassifierLoaded) {
					long classifyStartTime = System.currentTimeMillis();
					List<float[]> classifyOutput = app.audioClassifyClassicUtils.getClassifier(clsfrId).classify(audioFilePath);
					Log.e(logTag, "Classify Job Time Elapsed: "+ DateTimeUtils.timeStampDifferenceFromNowAsReadableString(classifyStartTime));
					Log.d(logTag, app.audioClassifyClassicUtils.classifierOutputAsJson(clsfrId, audioId, audioStartsAt, classifyOutput).toString());
				}



//				List<String[]> latestQueuedAudioFilesToClassify = app.audioClassifyDb.dbQueued.getAllRows();
//				if (latestQueuedAudioFilesToClassify.size() == 0) { Log.d(logTag, "No audio files are queued to be classified."); }
//				long audioCycleDuration = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 1000;
//				AudioClassifyClassicUtils.cleanupClassifyDirectory( context, latestQueuedAudioFilesToClassify, Math.round( 1.0 * audioCycleDuration ) );
//
//				for (String[] latestQueuedAudioToClassify : latestQueuedAudioFilesToClassify) {
//
//					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
//
//					// only proceed with classify job if there is a valid queued audio file in the database
//					if (latestQueuedAudioToClassify[0] != null) {
////
////						String encodePurpose = latestQueuedAudioToEncode[9];
////						String timestamp = latestQueuedAudioToEncode[1];
////						String inputFileExt = latestQueuedAudioToEncode[2];
////						long audioDuration = Long.parseLong(latestQueuedAudioToEncode[7]);
////						String codec = latestQueuedAudioToEncode[6];
////						int bitRate = Integer.parseInt(latestQueuedAudioToEncode[5]);
////						int inputSampleRate = Integer.parseInt(latestQueuedAudioToEncode[11]);
////						int outputSampleRate = Integer.parseInt(latestQueuedAudioToEncode[4]);
////
////						File preEncodeFile = new File(latestQueuedAudioToEncode[10]);
////						File finalDestinationFile = null;
////
////						if (!preEncodeFile.exists()) {
////
////							Log.e(logTag, "Skipping Audio Encode Job ("+StringUtils.capitalizeFirstChar(encodePurpose)+") for " + timestamp + " because input audio file could not be found.");
////
////							app.audioEncodeDb.dbQueued.deleteSingleRow(timestamp);
////
////						} else if (Integer.parseInt(latestQueuedAudioToEncode[12]) >= AudioEncodeUtils.ENCODE_FAILURE_SKIP_THRESHOLD) {
////
////							Log.e(logTag, "Skipping Audio Encode Job ("+StringUtils.capitalizeFirstChar(encodePurpose)+") for " + timestamp + " after " + AudioEncodeUtils.ENCODE_FAILURE_SKIP_THRESHOLD + " failed attempts.");
////
////							app.audioEncodeDb.dbQueued.deleteSingleRow(timestamp);
////							FileUtils.delete(preEncodeFile);
////
////						} else {
////
////							Log.i(logTag, "Beginning Audio Encode Job ("+ StringUtils.capitalizeFirstChar(encodePurpose) +"): "
////												+ timestamp + ", "
////												+ inputFileExt.toUpperCase(Locale.US) + " ("+Math.round(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CAPTURE_SAMPLE_RATE)/1000)+" kHz) "
////												+"to " + codec.toUpperCase(Locale.US)+" ("+Math.round(outputSampleRate/1000)+" kHz"+ ((codec.equalsIgnoreCase("opus")) ? (", "+Math.round(bitRate/1024)+" kbps") : "")+")"
////							);
////
////							app.audioEncodeDb.dbQueued.incrementSingleRowAttempts(timestamp);
////
////							// if needed, re-sample wav file prior to encoding
////							preEncodeFile = AudioCaptureUtils.checkOrCreateReSampledWav(context, encodePurpose, preEncodeFile.getAbsolutePath(), Long.parseLong(timestamp), inputFileExt, outputSampleRate);
////
////							File postEncodeFile = new File(RfcxAudioFileUtils.getAudioFileLocation_PostEncode(context, Long.parseLong(timestamp), codec, outputSampleRate, encodePurpose));
////
////							// just in case there's already a post-encoded file, delete it first
////							FileUtils.delete(postEncodeFile);
////
////							long encodeStartTime = System.currentTimeMillis();
////
////							// perform audio encoding and return encoding true bit rate
////							int measuredBitRate = AudioEncodeUtils.encodeAudioFile( preEncodeFile, postEncodeFile, codec, outputSampleRate, bitRate, AudioEncodeUtils.ENCODE_QUALITY );
////
////							long encodeDuration = (System.currentTimeMillis() - encodeStartTime);
////
////							if (measuredBitRate >= 0) {
////
////								// generate file checksum of encoded file
////								String encodedFileDigest = FileUtils.sha1Hash(postEncodeFile.getAbsolutePath());
////
////								if (encodePurpose.equalsIgnoreCase("stream")) {
////
////									File gZippedFile = new File(RfcxAudioFileUtils.getAudioFileLocation_GZip(app.rfcxGuardianIdentity.getGuid(), context, Long.parseLong(timestamp), RfcxAudioFileUtils.getFileExt(codec)));
////
////									// GZIP encoded file into final location
////									FileUtils.gZipFile(postEncodeFile, gZippedFile);
////
////									// If successful, cleanup pre-GZIP file and make sure final file is accessible by other roles (like 'api')
////									if (gZippedFile.exists()) {
////
////										FileUtils.delete(postEncodeFile);
////
////										finalDestinationFile = new File(RfcxAudioFileUtils.getAudioFileLocation_Queue(app.rfcxGuardianIdentity.getGuid(), context, Long.parseLong(timestamp), RfcxAudioFileUtils.getFileExt(codec)));
////
////										if (app.apiCheckInUtils.sendEncodedAudioToQueue(timestamp, gZippedFile, finalDestinationFile)) {
////
////											app.audioEncodeDb.dbEncoded.insert(
////													timestamp, RfcxAudioFileUtils.getFileExt(codec), encodedFileDigest, outputSampleRate, measuredBitRate,
////													codec, audioDuration, encodeDuration, encodePurpose, finalDestinationFile.getAbsolutePath(), inputSampleRate
////											);
////										}
////
////									}
////
////								} else if (encodePurpose.equalsIgnoreCase("vault")) {
////
////									finalDestinationFile = new File(RfcxAudioFileUtils.getAudioFileLocation_Vault(app.rfcxGuardianIdentity.getGuid(), Long.parseLong(timestamp), RfcxAudioFileUtils.getFileExt(codec), outputSampleRate));
////
////									if (AudioEncodeUtils.sendEncodedAudioToVault(timestamp, postEncodeFile, finalDestinationFile)) {
////
////										String vaultRowId = AudioEncodeUtils.vaultStatsDayId.format(new Date(Long.parseLong(timestamp)));
////
////										if (app.audioVaultDb.dbVault.getCountById(vaultRowId) > 0) {
////											app.audioVaultDb.dbVault.incrementSingleRowDuration(vaultRowId, Math.round(audioDuration/1000));
////											app.audioVaultDb.dbVault.incrementSingleRowRecordCount(vaultRowId, 1);
////											app.audioVaultDb.dbVault.incrementSingleRowFileSize(vaultRowId, FileUtils.getFileSizeInBytes(finalDestinationFile));
////										} else {
////											app.audioVaultDb.dbVault.insert(vaultRowId, Math.round(audioDuration/1000), 1, FileUtils.getFileSizeInBytes(finalDestinationFile));
////										}
////
////									}
////								}
////
////								app.audioEncodeDb.dbQueued.deleteSingleRow(timestamp, encodePurpose);
////
////							}
////						}
//					} else {
//						Log.e(logTag, "Queued audio file entry in database is invalid.");
//
//					}
//				}
//
//				app.rfcxServiceHandler.triggerIntentServiceImmediately("ApiCheckInQueue");

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
