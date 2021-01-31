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

import java.util.List;

public class AudioDetectionSendService extends Service {

	public static final String SERVICE_NAME = "AudioDetectionSend";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioDetectionSendService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioDetectionSendSvc audioDetectionSendSvc;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioDetectionSendSvc = new AudioDetectionSendSvc();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.audioDetectionSendSvc.start();
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
		this.audioDetectionSendSvc.interrupt();
		this.audioDetectionSendSvc = null;
	}
	
	private class AudioDetectionSendSvc extends Thread {

		public AudioDetectionSendSvc() {
			super("AudioDetectionSendService-AudioDetectionSend");
		}
		
		@Override
		public void run() {
			AudioDetectionSendService audioDetectionSendInstance = AudioDetectionSendService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			int detectionSendFailureSkipThreshold = 10;

			app.rfcxSvc.reportAsActive(SERVICE_NAME);

			try {

				List<String[]> latestQueuedAudioDetectionToSend = app.audioDetectionDb.dbQueued.getAllRows();
				if (latestQueuedAudioDetectionToSend.size() == 0) { Log.d(logTag, "No detections are currently queued."); }

				for (String[] latestQueuedDetectionToSend : latestQueuedAudioDetectionToSend) {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					// only proceed with audio detection send process if there is a valid queued json blob in the database
					if (latestQueuedDetectionToSend[0] != null) {

						String createdAt = latestQueuedDetectionToSend[0];
						String detectionJson = latestQueuedDetectionToSend[1];
						int previousAttempts = Integer.parseInt(latestQueuedDetectionToSend[3]);

						if (previousAttempts >= AudioClassifyUtils.DETECTION_SEND_FAILURE_SKIP_THRESHOLD) {

							Log.e(logTag, "Skipping Audio Detection Send Job for " + createdAt + " after " + AudioClassifyUtils.DETECTION_SEND_FAILURE_SKIP_THRESHOLD + " failed attempts.");

							app.audioDetectionDb.dbQueued.deleteSingleRow(createdAt);

						} else {

							app.audioDetectionDb.dbQueued.incrementSingleRowAttempts(createdAt);

							app.audioClassifyUtils.sendClassifyOutputToGuardianRole(detectionJson);

							app.audioDetectionDb.dbQueued.deleteSingleRow(createdAt);
						}

					} else {
						Log.d(logTag, "Queued audio detection send job definition from database is invalid.");

					}
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				audioDetectionSendInstance.runFlag = false;
			}
			
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			audioDetectionSendInstance.runFlag = false;

		}
	}
	

}
