package guardian.audio.encode;

import java.io.File;
import java.util.List;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;

public class AudioEncodeJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioEncodeJobService.class);
	
	private static final String SERVICE_NAME = "AudioEncodeJob";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioEncodeJob audioEncodeJob;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioEncodeJob = new AudioEncodeJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioEncodeJob.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioEncodeJob.interrupt();
		this.audioEncodeJob = null;
	}
	
	private class AudioEncodeJob extends Thread {

		public AudioEncodeJob() {
			super("AudioEncodeJobService-AudioEncodeJob");
		}
		
		@Override
		public void run() {
			AudioEncodeJobService audioEncodeJobInstance = AudioEncodeJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			int prefsEncodeSkipThreshold = app.rfcxPrefs.getPrefAsInt("audio_encode_skip_threshold");
			int prefsAudioEncodeQuality = app.rfcxPrefs.getPrefAsInt("audio_encode_quality");
			
//			AudioEncodeUtils.cleanupEncodeDirectory(context, app.audioEncodeDb.dbEncodeQueue.getAllRows());
			
			try {
				
				List<String[]> latestQueuedAudioFilesToEncode = app.audioEncodeDb.dbEncodeQueue.getAllRows();
				if (latestQueuedAudioFilesToEncode.size() == 0) { Log.d(logTag, "No audio files are queued to be encoded."); }
				
				for (String[] latestQueuedAudioToEncode : latestQueuedAudioFilesToEncode) {
									
					// only proceed with encode process if there is a valid queued audio file in the database
					if (latestQueuedAudioToEncode[0] != null) {
						
						File preEncodeFile = new File(latestQueuedAudioToEncode[9]);
						
						if (!preEncodeFile.exists()) {
							
							app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(latestQueuedAudioToEncode[1]);
							
						} else if (((int) Integer.parseInt(latestQueuedAudioToEncode[10])) >= prefsEncodeSkipThreshold) {
							
							Log.d(logTag, (new StringBuilder()).append("Skipping AudioEncodeJob ").append(latestQueuedAudioToEncode[1]).append(" after ").append(prefsEncodeSkipThreshold).append(" failed attempts").toString());
							
							app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(latestQueuedAudioToEncode[1]);
							if (preEncodeFile.exists()) { preEncodeFile.delete(); }
							
						} else {
								
							Log.i(logTag, (new StringBuilder()).append("Beginning Encode: ").append(latestQueuedAudioToEncode[1]).append(" ").append(latestQueuedAudioToEncode[2]).append("=>").append(latestQueuedAudioToEncode[6]).toString());
						
							File postEncodeFile = new File(RfcxAudioUtils.getAudioFileLocation_PostEncode(context, (long) Long.parseLong(latestQueuedAudioToEncode[1]),latestQueuedAudioToEncode[6]));
							File gZippedFile = new File(RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(app.rfcxDeviceGuid.getDeviceGuid(), context, (long) Long.parseLong(latestQueuedAudioToEncode[1]),RfcxAudioUtils.getFileExtension(latestQueuedAudioToEncode[6])));

							// just in case there's already a post-encoded file, delete it first
							if (postEncodeFile.exists()) { postEncodeFile.delete(); }

							long encodeStartTime = System.currentTimeMillis();

							// perform audio encoding and set encoding eventual bit rate
							int encodeBitRate = 
								AudioEncodeUtils.encodeAudioFile(
									preEncodeFile, 											// source file
									postEncodeFile, 											// target file
									latestQueuedAudioToEncode[6], 							// encoding codec
									(int) Integer.parseInt(latestQueuedAudioToEncode[4]), 	// encoding sample rate
									(int) Integer.parseInt(latestQueuedAudioToEncode[5]), 	// encoding target bitrate
									prefsAudioEncodeQuality									// encoding quality
								);

							long encodeDuration = (System.currentTimeMillis() - encodeStartTime);

							if (encodeBitRate < 0) {

								app.audioEncodeDb.dbEncodeQueue.incrementSingleRowAttempts(latestQueuedAudioToEncode[1]);
								
							} else {

								// delete pre-encode file
								if (preEncodeFile.exists() && postEncodeFile.exists()) { preEncodeFile.delete(); }

								// generate file checksum of encoded file
								String preZipDigest = FileUtils.sha1Hash(postEncodeFile.getAbsolutePath());

								// GZIP encoded file into final location
								GZipUtils.gZipFile(postEncodeFile, gZippedFile);

								// If successful, cleanup pre-GZIP file and make sure final file is accessible by other roles (like 'api')
								if (gZippedFile.exists()) {

									FileUtils.chmod(gZippedFile, 0777);
									if (postEncodeFile.exists()) { postEncodeFile.delete(); }

									app.audioEncodeDb.dbEncoded
										.insert(
											latestQueuedAudioToEncode[1], 
											RfcxAudioUtils.getFileExtension(latestQueuedAudioToEncode[6]), 
											preZipDigest, 
											(int) Integer.parseInt(latestQueuedAudioToEncode[4]), 
											encodeBitRate, 
											latestQueuedAudioToEncode[6], 
											(long) Long.parseLong(latestQueuedAudioToEncode[7]),
											encodeDuration, 
											gZippedFile.getAbsolutePath()
										);

									app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(latestQueuedAudioToEncode[1]);

									app.rfcxServiceHandler.triggerService("ApiCheckInQueue", true);
								}
								
							}
						}		
					} else {
						Log.d(logTag, "Queued audio file entry in database is invalid.");
						
					}
				}
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			
			} finally { 
				audioEncodeJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}

		}
	}
	

}
