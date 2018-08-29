package guardian.api;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.StringUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiCheckInArchiveService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInArchiveService.class);
	
	private static final String SERVICE_NAME = "ApiCheckInArchive";
	
	private RfcxGuardian app;
	
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
		return START_STICKY;
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
			
			String rfcxDeviceId = app.rfcxDeviceGuid.getDeviceGuid();

			int archiveThreshold = app.rfcxPrefs.getPrefAsInt("checkin_archive_threshold");
			int stashCount = app.apiCheckInDb.dbStashed.getCount();
			
			String [] metaColumns = new String[] { "measured_at", "queued_at", "filename", "format", "sha1checksum", "samplerate", "bitrate", "encode_duration" };
			
			String sdCardArchiveDir = Environment.getExternalStorageDirectory().toString()+"/rfcx/archive";
			(new File(sdCardArchiveDir)).mkdirs(); FileUtils.chmod(sdCardArchiveDir, 0777);

			SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);
			SimpleDateFormat metaDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ", Locale.US);
			
			long archiveTimestamp = System.currentTimeMillis();
			String archiveTarFilePath = sdCardArchiveDir+"/"+rfcxDeviceId+"_"+fileDateTimeFormat.format(new Date(archiveTimestamp));
			(new File(archiveTarFilePath)).mkdirs(); FileUtils.chmod(archiveTarFilePath, 0777);
			(new File(archiveTarFilePath+"/audio")).mkdirs(); FileUtils.chmod(archiveTarFilePath+"/audio", 0777);
				
			if (!(new File(sdCardArchiveDir)).isDirectory()) {
				Log.e(logTag, "Archive job cancelled because SD card directory could not be located: "+sdCardArchiveDir);
				
			} else if (stashCount < archiveThreshold) {
				Log.e(logTag, "Archive job cancelled because archive threshold has not been reached: "+stashCount+" checkins are currently stashed ("+archiveThreshold+" required)");
				
			} else {
			
				try {
					
					Log.i(logTag, "Archiving "+archiveThreshold+" of "+stashCount+" stashed checkins");
					
					List<String[]> checkInsBeyondArchiveThreshold = app.apiCheckInDb.dbStashed.getRowsWithOffset((stashCount - archiveThreshold), archiveThreshold);

					// Create Archive File List
					List<String> preTarFileList = new ArrayList<String>();
					
					StringBuilder tsvRows = new StringBuilder();
					for (String columnName : metaColumns) { tsvRows.append(columnName).append("\t"); }
					tsvRows.append("\n");
					
					for (String[] checkInToArchive : checkInsBeyondArchiveThreshold) {
						
						String newFileName = checkInToArchive[4].substring(1+checkInToArchive[4].lastIndexOf("/"));

						// Create TSV contents row
						JSONObject audioJson = new JSONObject(checkInToArchive[2]);
						String[] audioMeta = audioJson.getString("audio").split("\\*");
						
						String tsvRow = (new StringBuilder())
							/* measured_at */	.append(metaDateTimeFormat.format(new Date((long) Long.parseLong(audioMeta[1])))).append("\t") 					
							/* queued_at */		.append(metaDateTimeFormat.format(new Date((long) Long.parseLong(audioJson.getString("queued_at"))))).append("\t")
							/* filename */		.append(newFileName).append("\t") 					
							/* format */			.append(audioMeta[2]).append("\t") 																		
							/* sha1checksum */	.append(audioMeta[3]).append("\t") 																			
							/* samplerate */		.append(audioMeta[4]).append("\t") 																		
							/* bitrate */		.append(audioMeta[5]).append("\t") 											
							/* encode_duration */.append(audioMeta[8]).append("\t") 	
							.append("\n").toString();
						
						// Copy audio file into position
						FileUtils.copy(checkInToArchive[4], archiveTarFilePath+"/audio/"+newFileName);
						
						if ((new File(archiveTarFilePath+"/audio/"+newFileName)).exists()) { 
							tsvRows.append(tsvRow);
							preTarFileList.add(archiveTarFilePath+"/audio/"+newFileName);
						}
					}
					
					StringUtils.saveStringToFile(tsvRows.toString(), archiveTarFilePath+"/_metadata.tsv");
					preTarFileList.add(archiveTarFilePath+"/_metadata.tsv");
					
					FileUtils.createTarArchiveFromFileList(preTarFileList, archiveTarFilePath+".tar");
					
					// Clean up and remove archived originals
					for (String[] checkInToArchive : checkInsBeyondArchiveThreshold) {
						File fileObj = new File(checkInToArchive[4]);
						if (fileObj.exists()) { fileObj.delete(); }
						app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(checkInToArchive[1]);
					}
					FileUtils.deleteDirectory(archiveTarFilePath);
				
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					
				} finally {
					apiCheckInArchiveInstance.runFlag = false;
					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					app.rfcxServiceHandler.stopService(SERVICE_NAME);
				}
			}
			
			apiCheckInArchiveInstance.runFlag = false;
			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
	}

}
