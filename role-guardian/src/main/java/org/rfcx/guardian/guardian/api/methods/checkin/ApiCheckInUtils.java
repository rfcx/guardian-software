package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApiCheckInUtils {

	public ApiCheckInUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInUtils");

	private RfcxGuardian app;

	public boolean addCheckInToQueue(String[] audioInfo, String filePath) {

		// serialize audio info into JSON for checkin queue insertion
		String queueJson = app.apiCheckInJsonUtils.buildCheckInQueueJson(audioInfo);

		long audioFileSize = FileUtils.getFileSizeInBytes(filePath);

		// add audio info to checkin queue
		int queuedCount = app.apiCheckInDb.dbQueued.insert(audioInfo[1] + "." + audioInfo[2], queueJson, "0", filePath, audioInfo[10], audioFileSize+"");

		long queuedLimitMb = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_QUEUE_FILESIZE_LIMIT);
		long queuedLimitPct = Math.round(Math.floor(100*(Double.parseDouble(app.apiCheckInDb.dbQueued.getCumulativeFileSizeForAllRows()+"")/(queuedLimitMb*1024*1024))));

		Log.d(logTag, "Audio added to Queue: " + audioInfo[1]
						+ ", " + FileUtils.bytesAsReadableString(audioFileSize) + " (" + queuedCount + " in queue, "+queuedLimitPct+"% of "+queuedLimitMb+" MB limit)"
						+ ", " + RfcxAssetCleanup.conciseFilePath(filePath, RfcxGuardian.APP_ROLE));

		// once queued, remove database reference from encode role
		app.audioEncodeDb.dbEncoded.deleteSingleRow(audioInfo[1], "stream");

		return true;
	}

	public boolean sendEncodedAudioToQueue(String audioTimestamp, File preStreamFile, File streamFile) throws IOException {

		FileUtils.copy(preStreamFile, streamFile);
		FileUtils.delete(preStreamFile);
		if (FileUtils.exists(streamFile)) {
			app.audioEncodeDb.dbEncoded.deleteSingleRow(audioTimestamp, "stream");
			return true;
		} else {
			Log.e(logTag, "Final encoded file not found: "+streamFile.getAbsolutePath());
		}
		return false;
	}

	public void stashOrArchiveOldestCheckIns() {

		long queueFileSizeLimit = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_QUEUE_FILESIZE_LIMIT);
		long queueFileSizeLimitInBytes = queueFileSizeLimit*1024*1024;

		long stashFileSizeBuffer = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_STASH_FILESIZE_BUFFER);
		long archiveFileSizeTarget = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_ARCHIVE_FILESIZE_TARGET);

		if (app.apiCheckInDb.dbQueued.getCumulativeFileSizeForAllRows() >= queueFileSizeLimitInBytes) {

			long queuedFileSizeSumBeforeLimit = 0;
			int queuedCountBeforeLimit = 0;

			for (String[] checkInCycle : app.apiCheckInDb.dbQueued.getRowsWithOffset(0,5000)) {
				queuedFileSizeSumBeforeLimit += Long.parseLong(checkInCycle[6]);
				if (queuedFileSizeSumBeforeLimit >= queueFileSizeLimitInBytes) { break; }
				queuedCountBeforeLimit++;
			}

			// string list for reporting stashed checkins to the log
			List<String> stashSuccessList = new ArrayList<String>();
			List<String> stashFailureList = new ArrayList<String>();
			int stashCount = 0;

			// cycle through stashable checkins and move them to the new table/database
			for (String[] checkInsToStash : app.apiCheckInDb.dbQueued.getRowsWithOffset(queuedCountBeforeLimit, 16)) {

				String stashFilePath = RfcxAudioFileUtils.getAudioFileLocation_Stash(
						app.rfcxGuardianIdentity.getGuid(),
						app.getApplicationContext(),
						Long.parseLong(checkInsToStash[1].substring(0, checkInsToStash[1].lastIndexOf("."))),
						checkInsToStash[1].substring(checkInsToStash[1].lastIndexOf(".") + 1));
				try {
					FileUtils.copy(checkInsToStash[4], stashFilePath);
				} catch (IOException e) {
					RfcxLog.logExc(logTag, e);
				}

				if (FileUtils.exists(stashFilePath) && (FileUtils.getFileSizeInBytes(stashFilePath) == Long.parseLong(checkInsToStash[6]))) {
					stashCount = app.apiCheckInDb.dbStashed.insert(checkInsToStash[1], checkInsToStash[2], checkInsToStash[3], stashFilePath, checkInsToStash[5], checkInsToStash[6]);
					stashSuccessList.add(checkInsToStash[1].substring(0, checkInsToStash[1].indexOf(".")));
				} else {
					stashFailureList.add(checkInsToStash[1].substring(0, checkInsToStash[1].indexOf(".")));
				}

				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInsToStash[1]);
				FileUtils.delete(checkInsToStash[4]);
			}

			if (stashFailureList.size() > 0) {
				Log.e(logTag, "Failed to Stash " + stashFailureList.size() + " CheckIn" + ((stashFailureList.size() == 1) ? "" : "s") + " (" + TextUtils.join(" ", stashFailureList) + ").");
			}

			if (stashSuccessList.size() > 0) {

				long stashedSize = app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows();
				long stashedLimitPct = Math.round(Math.floor(100*(Double.parseDouble(stashedSize+"")/((stashFileSizeBuffer+archiveFileSizeTarget)*1024*1024))));

				Log.i(logTag, "Transferred " + stashSuccessList.size() + " CheckIn" + ((stashSuccessList.size() == 1) ? "" : "s") + " to Stash: "
						+ TextUtils.join(" ", stashSuccessList)
						+" (" + stashCount + " in stash"
						+", "+FileUtils.bytesAsReadableString(stashedSize)
						+", " + stashedLimitPct +"% of "+(stashFileSizeBuffer+archiveFileSizeTarget)+" MB limit)");
			}
		}

		if (app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows() >= ((stashFileSizeBuffer+archiveFileSizeTarget)*1024*1024)) {
			app.rfcxSvc.triggerService( ApiCheckInArchiveService.SERVICE_NAME, false);
		}
	}

	public void skipSingleCheckIn(String[] checkInToSkip) {

		if (DeviceStorage.isExternalStorageWritable()) {

			String skipFilePath = RfcxAudioFileUtils.getAudioFileLocation_ExternalStorage(
					app.rfcxGuardianIdentity.getGuid(),
					Long.parseLong(checkInToSkip[1].substring(0, checkInToSkip[1].lastIndexOf("."))),
					checkInToSkip[1].substring(checkInToSkip[1].lastIndexOf(".") + 1));

			try {
				FileUtils.copy(checkInToSkip[4], skipFilePath);
			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}

			if (FileUtils.exists(skipFilePath) && (FileUtils.getFileSizeInBytes(skipFilePath) == Long.parseLong(checkInToSkip[6]))) {
				app.apiCheckInDb.dbSkipped.insert(checkInToSkip[0], checkInToSkip[1], checkInToSkip[2], checkInToSkip[3], skipFilePath, checkInToSkip[5], checkInToSkip[6]);
			}
		}

		app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInToSkip[1]);
		FileUtils.delete(checkInToSkip[4]);

	}

	public void reQueueAudioAssetForCheckIn(String checkInStatus, String audioId) {

		boolean isReQueued = false;
		String[] checkInToReQueue = new String[] {};

		// fetch check-in entry from relevant table, if it exists...
		if (checkInStatus.equalsIgnoreCase("sent")) {
			checkInToReQueue = app.apiCheckInDb.dbSent.getSingleRowByAudioAttachmentId(audioId);
		} else if (checkInStatus.equalsIgnoreCase("stashed")) {
			checkInToReQueue = app.apiCheckInDb.dbStashed.getSingleRowByAudioAttachmentId(audioId);
		} else if (checkInStatus.equalsIgnoreCase("skipped")) {
			checkInToReQueue = app.apiCheckInDb.dbSkipped.getSingleRowByAudioAttachmentId(audioId);
		}

		// if this array has been populated, indicating that the source row exists, then add entry to checkin table
		if ((checkInToReQueue.length > 0) && (checkInToReQueue[0] != null)) {


			int queuedCount = app.apiCheckInDb.dbQueued.insert(checkInToReQueue[1], checkInToReQueue[2], checkInToReQueue[3], checkInToReQueue[4], checkInToReQueue[5], checkInToReQueue[6]);
			String[] reQueuedCheckIn = app.apiCheckInDb.dbQueued.getSingleRowByAudioAttachmentId(audioId);

			// if successfully inserted into queue table (and verified), delete from original table
			if (reQueuedCheckIn[0] != null) {
				if (checkInStatus.equalsIgnoreCase("sent")) {
					app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(audioId);
					checkInToReQueue = app.apiCheckInDb.dbSent.getSingleRowByAudioAttachmentId(audioId);
				} else if (checkInStatus.equalsIgnoreCase("stashed")) {
					app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(audioId);
					checkInToReQueue = app.apiCheckInDb.dbStashed.getSingleRowByAudioAttachmentId(audioId);
				} else if (checkInStatus.equalsIgnoreCase("skipped")) {
					app.apiCheckInDb.dbSkipped.deleteSingleRowByAudioAttachmentId(audioId);
					checkInToReQueue = app.apiCheckInDb.dbSkipped.getSingleRowByAudioAttachmentId(audioId);
				}
				isReQueued = (checkInToReQueue[0] == null);
			}
		}

		if (isReQueued) {
			Log.i(logTag, "CheckIn Successfully ReQueued (Stream): "+checkInStatus+", "+audioId);
		} else {
			Log.e(logTag, "CheckIn Failed to ReQueue (Stream): "+checkInStatus+", "+audioId);
		}
	}

	public void reQueueStashedCheckInIfAllowedByHealthCheck(long[] currentCheckInStats) {

		if (	app.apiCheckInHealthUtils.validateRecentCheckInHealthCheck(
					app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION),
					app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.CHECKIN_REQUEUE_BOUNDS_HOURS),
					currentCheckInStats
				)
			&& 	(app.apiCheckInDb.dbStashed.getCount() > 0)
		) {
			reQueueAudioAssetForCheckIn("stashed", app.apiCheckInDb.dbStashed.getLatestRow()[1]);
		}
	}

	public void moveCheckInEntryToSentDatabase(String inFlightCheckInAudioId) {

		if ((app.apiCheckInHealthUtils.getInFlightCheckInEntry(inFlightCheckInAudioId) != null) && (app.apiCheckInHealthUtils.getInFlightCheckInEntry(inFlightCheckInAudioId)[0] != null)) {
			String[] checkInEntry = app.apiCheckInHealthUtils.getInFlightCheckInEntry(inFlightCheckInAudioId);
			// delete latest instead to keep present info
			if (app.apiCheckInHealthUtils.getLatestCheckInAudioId() != null) {
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(app.apiCheckInHealthUtils.getLatestCheckInAudioId());
			}
			if ((checkInEntry != null) && (checkInEntry[0] != null)) {
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
				app.apiCheckInDb.dbSent.insert(checkInEntry[1], checkInEntry[2], checkInEntry[3], checkInEntry[4], checkInEntry[5], checkInEntry[6]);
				app.apiCheckInDb.dbSent.incrementSingleRowAttempts(checkInEntry[1]);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
			}
		}


		long sentFileSizeBufferInBytes = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_SENT_FILESIZE_BUFFER)*1024*1024;

		if (app.apiCheckInDb.dbSent.getCumulativeFileSizeForAllRows() >= sentFileSizeBufferInBytes) {

			long sentFileSizeSumBeforeLimit = 0;
			int sentCountBeforeLimit = 0;

			for (String[] checkInCycle : app.apiCheckInDb.dbSent.getRowsWithOffset(0,5000)) {
				sentFileSizeSumBeforeLimit += Long.parseLong(checkInCycle[6]);
				if (sentFileSizeSumBeforeLimit >= sentFileSizeBufferInBytes) { break; }
				sentCountBeforeLimit++;
			}

			for (String[] sentCheckInsToMove : app.apiCheckInDb.dbSent.getRowsWithOffset(sentCountBeforeLimit, 16)) {

				if (!DeviceStorage.isExternalStorageWritable()) {
					app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(sentCheckInsToMove[1]);

				} else {
					String sentFilePath_BufferOverrun = RfcxAudioFileUtils.getAudioFileLocation_ExternalStorage(
							app.rfcxGuardianIdentity.getGuid(),
							Long.parseLong(sentCheckInsToMove[1].substring(0, sentCheckInsToMove[1].lastIndexOf("."))),
							sentCheckInsToMove[1].substring(sentCheckInsToMove[1].lastIndexOf(".") + 1));
					try {
						FileUtils.copy(sentCheckInsToMove[4], sentFilePath_BufferOverrun);
					} catch (IOException e) {
						RfcxLog.logExc(logTag, e);
					}

					if (FileUtils.exists(sentFilePath_BufferOverrun) && (FileUtils.getFileSizeInBytes(sentFilePath_BufferOverrun) == Long.parseLong(sentCheckInsToMove[6]))) {
						app.apiCheckInDb.dbSent.updateFilePathByAudioAttachmentId(sentCheckInsToMove[1], sentFilePath_BufferOverrun);
					} else {
						app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(sentCheckInsToMove[1]);
					}
				}

				FileUtils.delete(sentCheckInsToMove[4]);
			}
		}
	}



}
