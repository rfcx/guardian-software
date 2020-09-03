package org.rfcx.guardian.guardian.archive;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiCheckInArchiveService extends Service {

	private static final String SERVICE_NAME = "ApiCheckInArchive";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInArchiveService");
	
	private RfcxGuardian app;

	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US);
	private static final SimpleDateFormat metaDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
	
	private String rfcxDeviceId;
	private long archiveTimestamp = System.currentTimeMillis();

	private String archiveSdCardDir;
	private String archiveTitle;
	private String archiveWorkDir;
	private String archiveTarFilePath;
	
	private static final String[] tsvMetaColumns = new String[] { "measured_at", "queued_at", "filename", "format", "sha1checksum", "samplerate", "bitrate", "encode_duration" };
	
	private boolean runFlag = false;
	private ApiCheckInArchive apiCheckInArchive;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInArchive = new ApiCheckInArchive();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckInArchive.start();
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
		this.apiCheckInArchive.interrupt();
		this.apiCheckInArchive = null;
//		Log.v(logTag, "Stopping service: "+logTag);
	}
	
	private class ApiCheckInArchive extends Thread {

		public ApiCheckInArchive() {
			super("ApiCheckInArchiveService-ApiCheckInArchive");
		}
		
		@Override
		public void run() {
			ApiCheckInArchiveService apiCheckInArchiveInstance = ApiCheckInArchiveService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			rfcxDeviceId = app.rfcxGuardianIdentity.getGuid();
			archiveTimestamp = System.currentTimeMillis();
			
			setAndInitializeArchiveDirectories(context);

			long archiveFileSizeTarget = app.rfcxPrefs.getPrefAsLong("checkin_archive_filesize_target");
			long archiveFileSizeTargetInBytes = archiveFileSizeTarget*1024*1024;

			long stashFileSizeBuffer = app.rfcxPrefs.getPrefAsLong("checkin_stash_filesize_buffer");
			long stashFileSizeBufferInBytes = stashFileSizeBuffer*1024*1024;

			long stashedCumulativeFileSizeInBytes = app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows();
						
			if (!(new File(archiveSdCardDir)).isDirectory()) {
				Log.e(logTag, "Archive job cancelled because SD card directory could not be located: "+ archiveSdCardDir);
				
			} else if (stashedCumulativeFileSizeInBytes < (stashFileSizeBufferInBytes + archiveFileSizeTargetInBytes)) {
				Log.e(logTag, "Archive job cancelled because archive threshold ("+archiveFileSizeTarget+" MB) has not been reached.");
				
			} else {
			
				try {

					List<String[]> stashedCheckInsBeyondBuffer = new ArrayList<String[]>();
					List<String[]> allStashedCheckIns = app.apiCheckInDb.dbStashed.getAllRows();

					long fileSizeBufferTracker = 0;

					for (int i = allStashedCheckIns.size() - 1; i >= 0; i--) {
						fileSizeBufferTracker += Long.parseLong(allStashedCheckIns.get(i)[6]);
						if (fileSizeBufferTracker > stashFileSizeBufferInBytes) {
							stashedCheckInsBeyondBuffer.add(allStashedCheckIns.get(i));
						}
					}

					Log.i(logTag, "Archiving "+stashedCheckInsBeyondBuffer.size()+" Stashed CheckIns.");

					// Create Archive File List
					List<String> archiveFileList = new ArrayList<String>();
					
					StringBuilder tsvRows = new StringBuilder();
					tsvRows.append(TextUtils.join("\t", tsvMetaColumns)).append("\n");
					
					long oldestCheckInTimestamp = System.currentTimeMillis();
					long newestCheckInTimestamp = 0;
					
					for (String[] checkIn : stashedCheckInsBeyondBuffer) {
						
						String gzFileName = "audio_"+checkIn[4].substring(1+checkIn[4].lastIndexOf("/"));
						String unGzFileName = gzFileName.substring(0, gzFileName.indexOf(".gz"));

						// Create TSV contents row
						JSONObject audioJson = new JSONObject(checkIn[2]);
						String[] audioMeta = audioJson.getString("audio").split("\\*");
						
						long measuredAt = Long.parseLong(audioMeta[1]);
						
						String tsvRow = ""
							/* measured_at */ 		+metaDateTimeFormat.format(new Date(measuredAt)) + "\t"
							/* queued_at */			+metaDateTimeFormat.format(new Date(Long.parseLong(audioJson.getString("queued_at")))) + "\t"
							/* filename */			+unGzFileName + "\t"
							/* format */			+audioMeta[2] + "\t"
							/* sha1checksum */		+audioMeta[3] + "\t"
							/* samplerate */		+audioMeta[4] + "\t"
							/* bitrate */			+audioMeta[5] + "\t"
							/* encode_duration */	+audioMeta[8]
													+"\n";
						
						// UnGZip audio files into position
						FileUtils.gUnZipFile(checkIn[4], archiveWorkDir+"/audio/"+unGzFileName);
						
						if (FileUtils.exists(archiveWorkDir+"/audio/"+unGzFileName)) {
							tsvRows.append(tsvRow);
							archiveFileList.add(archiveWorkDir+"/audio/"+unGzFileName);
						}
						
						if (measuredAt < oldestCheckInTimestamp) { oldestCheckInTimestamp = measuredAt; }
						if (measuredAt > newestCheckInTimestamp) { newestCheckInTimestamp = measuredAt; }
					}
					
					StringUtils.saveStringToFile(tsvRows.toString(), archiveWorkDir+"/_metadata_audio.tsv");
					archiveFileList.add(archiveWorkDir+"/_metadata_audio.tsv");

					Log.i(logTag, "Archiving '"+archiveTitle+"'...");
					FileUtils.createTarArchiveFromFileList(archiveFileList, archiveTarFilePath);
					long archiveFileSize = FileUtils.getFileSizeInBytes(archiveTarFilePath);

					app.archiveDb.dbCheckInArchive.insert(
							new Date(archiveTimestamp),			// archived_at
							new Date(oldestCheckInTimestamp),	// archive_begins_at
							new Date(newestCheckInTimestamp),	// archive_ends_at
							stashedCheckInsBeyondBuffer.size(),	// record_count
							archiveFileSize, 					// filesize in bytes
							archiveTarFilePath					// filepath
					);
					
					// Clean up and remove archived originals
					for (String[] checkIn : stashedCheckInsBeyondBuffer) {
						FileUtils.delete(checkIn[4]);
						app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(checkIn[1]);
					}
					FileUtils.delete(archiveWorkDir);
					
					Log.i(logTag, "Archive Complete: "
								+stashedCheckInsBeyondBuffer.size()+" audio files, "
								+Math.round(archiveFileSize/1024)+" kB, "
								+archiveTarFilePath);
				
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					
				}
			}
			
			apiCheckInArchiveInstance.runFlag = false;
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
	}
	
	
	private void setAndInitializeArchiveDirectories(Context context) {

		archiveTitle = "archive_"+rfcxDeviceId+"_"+fileDateTimeFormat.format(new Date(archiveTimestamp));
		archiveWorkDir = context.getFilesDir().toString() + "/archive/" + archiveTitle;
		archiveSdCardDir = Environment.getExternalStorageDirectory().toString() + "/rfcx/archive/" + dirDateFormat.format(new Date(archiveTimestamp));
		archiveTarFilePath = archiveSdCardDir + "/" + archiveTitle + ".tar";

		FileUtils.initializeDirectoryRecursively(archiveSdCardDir, true);
		FileUtils.initializeDirectoryRecursively(archiveWorkDir+"/audio", false);
	}

}
