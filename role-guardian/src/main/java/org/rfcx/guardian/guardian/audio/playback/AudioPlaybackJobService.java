package org.rfcx.guardian.guardian.audio.playback;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.misc.FileUtils;

import java.util.List;

public class AudioPlaybackJobService extends Service {

	public static final String SERVICE_NAME = "AudioPlaybackJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioPlaybackJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioPlaybackJob audioPlaybackJob;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioPlaybackJob = new AudioPlaybackJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.audioPlaybackJob.start();
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
		this.audioPlaybackJob.interrupt();
		this.audioPlaybackJob = null;
	}
	
	private class AudioPlaybackJob extends Thread {

		public AudioPlaybackJob() {
			super("AudioPlaybackJobService-AudioPlaybackJob");
		}
		
		@Override
		public void run() {
			AudioPlaybackJobService audioPlaybackJobInstance = AudioPlaybackJobService.this;
			
			app = (RfcxGuardian) getApplication();

			app.rfcxSvc.reportAsActive(SERVICE_NAME);
			
			try {

				List<String[]> latestQueuedAudioFilesForPlayback = app.audioPlaybackDb.dbQueued.getAllRows();

				for (String[] latestQueuedAudio : latestQueuedAudioFilesForPlayback) {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					if (latestQueuedAudio[0] != null) {

						String queuedAt = latestQueuedAudio[0];
						String assetId = latestQueuedAudio[1];
						String audioFormat = latestQueuedAudio[2];
						int audioSampleRate = Integer.parseInt(latestQueuedAudio[3]);
						String audioFilePath = latestQueuedAudio[4];
						long intendedDuration = Long.parseLong(latestQueuedAudio[5]);
						int playbackAttempts = Integer.parseInt(latestQueuedAudio[6]);

						if (!FileUtils.exists(audioFilePath)) {

							Log.d(logTag, "Skipping Audio Playback Job for " + assetId + " because input audio file could not be found.");

							app.audioPlaybackDb.dbQueued.deleteSingleRowByCreatedAt(queuedAt);

						} else if (playbackAttempts >= AudioPlaybackUtils.PLAYBACK_FAILURE_SKIP_THRESHOLD) {

							Log.d(logTag, "Skipping Audio Playback Job for " + assetId + " after " + AudioPlaybackUtils.PLAYBACK_FAILURE_SKIP_THRESHOLD + " failed attempts.");

							app.audioPlaybackDb.dbQueued.deleteSingleRowByCreatedAt(queuedAt);
							FileUtils.delete(audioFilePath);

						} else {


							Log.i(logTag, "Beginning Audio Playback Job: " + assetId + ", " + audioFormat);

							app.audioPlaybackDb.dbQueued.incrementSingleRowAttempts(queuedAt);

							long playbackStartTime = System.currentTimeMillis();

							MediaPlayer mediaPlayer = new MediaPlayer();
							mediaPlayer.setDataSource(audioFilePath);
							mediaPlayer.prepare();
							mediaPlayer.start();

							long playbackDuration = (System.currentTimeMillis() - playbackStartTime);

							app.audioPlaybackDb.dbQueued.deleteSingleRowByCreatedAt(queuedAt);

						}

					} else {
						Log.d(logTag, "Queued audio file entry in database is invalid.");

					}

				}


					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				audioPlaybackJobInstance.runFlag = false;
			}
			
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			audioPlaybackJobInstance.runFlag = false;

		}
	}
	

}
