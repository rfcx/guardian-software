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

	private String sdCardArchiveDir;
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
			
			setAndInitializeArchiveDirectories();
			
			int archiveThreshold = app.rfcxPrefs.getPrefAsInt("checkin_archive_threshold");
			int stashCount = app.apiCheckInDb.dbStashed.getCount();
						
			if (!(new File(sdCardArchiveDir)).isDirectory()) {
				Log.e(logTag, "Archive job cancelled because SD card directory could not be located: "+sdCardArchiveDir);
				
			} else if (stashCount < archiveThreshold) {
				Log.e(logTag, "Archive job cancelled because archive threshold has not been reached: "+stashCount+" checkins are currently stashed ("+archiveThreshold+" required)");
				
			} else {
			
				try {
					
					Log.i(logTag, "Archiving "+archiveThreshold+" of "+stashCount+" stashed checkins");
					
					List<String[]> checkInsBeyondArchiveThreshold = app.apiCheckInDb.dbStashed.getRowsWithOffset((stashCount - archiveThreshold), archiveThreshold);

					// Create Archive File List
					List<String> archiveFileList = new ArrayList<String>();
					
					StringBuilder tsvRows = new StringBuilder();
					tsvRows.append(TextUtils.join("\t", tsvMetaColumns)).append("\n");
					
					long oldestCheckInTimestamp = System.currentTimeMillis();
					long newestCheckInTimestamp = 0;
					
					for (String[] checkIn : checkInsBeyondArchiveThreshold) {
						
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
						FileUtils.gUnZipFile(checkIn[4], archiveTarFilePath+"/audio/"+unGzFileName);
						
						if ((new File(archiveTarFilePath+"/audio/"+unGzFileName)).exists()) { 
							tsvRows.append(tsvRow);
							archiveFileList.add(archiveTarFilePath+"/audio/"+unGzFileName);
						}
						
						if (measuredAt < oldestCheckInTimestamp) { oldestCheckInTimestamp = measuredAt; }
						if (measuredAt > newestCheckInTimestamp) { newestCheckInTimestamp = measuredAt; }
					}
					
					StringUtils.saveStringToFile(tsvRows.toString(), archiveTarFilePath+"/_metadata_audio.tsv");
					archiveFileList.add(archiveTarFilePath+"/_metadata_audio.tsv");

					Log.i(logTag, "Archiving: "+archiveTarFilePath);
					FileUtils.createTarArchiveFromFileList(archiveFileList, archiveTarFilePath+".tar");
					FileUtils.gZipFile(archiveTarFilePath+".tar", archiveTarFilePath+".tar.gz");
					
					int archiveFileSize = 0;
					app.archiveDb.dbCheckInArchive.insert(new Date(archiveTimestamp), new Date(oldestCheckInTimestamp), new Date(newestCheckInTimestamp), checkInsBeyondArchiveThreshold.size(), archiveFileSize, archiveTarFilePath);
					
					// Clean up and remove archived originals
					for (String[] checkIn : checkInsBeyondArchiveThreshold) {
						FileUtils.delete(checkIn[4]);
						app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(checkIn[1]);
					}
					FileUtils.delete(archiveTarFilePath);
					FileUtils.delete(archiveTarFilePath+".tar");
					
					Log.i(logTag, "Archive complete: "+archiveTarFilePath);
				
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					
				}
			}
			
			apiCheckInArchiveInstance.runFlag = false;
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
	}
	
	
	private void setAndInitializeArchiveDirectories() {

		sdCardArchiveDir = Environment.getExternalStorageDirectory().toString()+"/rfcx/archive";
		archiveTarFilePath = sdCardArchiveDir+"/archive_"+rfcxDeviceId+"_"+fileDateTimeFormat.format(new Date(archiveTimestamp));
		
		(new File(sdCardArchiveDir)).mkdirs(); FileUtils.chmod(sdCardArchiveDir, "rw", "rw");
		(new File(archiveTarFilePath)).mkdirs(); FileUtils.chmod(archiveTarFilePath, "rw", "rw");
		(new File(archiveTarFilePath+"/audio")).mkdirs(); FileUtils.chmod(archiveTarFilePath+"/audio", "rw", "rw");
		
	}

}
