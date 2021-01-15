package org.rfcx.guardian.guardian.asset;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.asset.RfcxLogcatFileUtils;
import org.rfcx.guardian.utility.asset.RfcxPhotoFileUtils;
import org.rfcx.guardian.utility.asset.RfcxScreenShotFileUtils;
import org.rfcx.guardian.utility.asset.RfcxVideoFileUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AssetUtils {

	public AssetUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetUtils");

	private RfcxGuardian app;


	public String[] getLatestExternalAssetMeta(String assetType, long excludeAssetsLastAccessWithinDuration) {

		String[] assetMeta = new String[] { null };
		try {
			JSONArray latestAssetMetaArr = RfcxComm.getQuery("admin", "database_get_latest_row", assetType, app.getResolver());
			for (int i = 0; i < latestAssetMetaArr.length(); i++) {
				JSONObject latestAssetMeta = latestAssetMetaArr.getJSONObject(i);
				long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(latestAssetMeta.getString("last_accessed_at"))));
				if (milliSecondsSinceAccessed > excludeAssetsLastAccessWithinDuration) {
					assetMeta = new String[] { latestAssetMeta.getString("filepath"),
							latestAssetMeta.getString("created_at"), latestAssetMeta.getString("timestamp"),
							latestAssetMeta.getString("format"), latestAssetMeta.getString("digest"),
							latestAssetMeta.getString("width"), latestAssetMeta.getString("height") };
					RfcxComm.updateQuery("admin", "database_set_last_accessed_at", assetType + "|" + latestAssetMeta.getString("timestamp"), app.getResolver());
					break;
				} else {
					Log.e(logTag,"Skipping asset attachment: "+assetType+", "+latestAssetMeta.getString("timestamp")+" was last sent only "+Math.round(milliSecondsSinceAccessed / 1000)+" seconds ago.");
				}
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return assetMeta;
	}

	public void purgeListOfAssets(String assetType, List<String> assetIdList) {
		String rfcxDeviceId = app.rfcxGuardianIdentity.getGuid();
		Context appContext = app.getApplicationContext();
		List<String> idList = new ArrayList<>();
		for (String assetId : assetIdList) {
			String[] purgeReport = purgeSingleAsset( assetType, rfcxDeviceId, appContext, assetId );
			if (purgeReport.length > 2) {
				Log.d(logTag, "Purged Asset: "+ TextUtils.join(", ", purgeReport));
			} else {
				idList.add(purgeReport[1]);
			}
		}
		if (idList.size() > 0) {
			Log.d(logTag, "Purged Asset" + ((idList.size() > 1) ? "s" : "") + ": " + assetType + ", " + TextUtils.join(" ", idList));
		}
	}

	public void purgeSingleAsset(String assetType, String assetId) {
		String[] purgeReport = purgeSingleAsset( assetType, app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), assetId);
		Log.d(logTag, "Purged Asset: "+ TextUtils.join(", ", purgeReport));
	}

	public String[] purgeSingleAsset(String assetType, String rfcxDeviceId, Context context, String assetId) {

		String[] purgedAssetReport = new String[] { assetType, assetId };;

		try {

			long numericAssetId = Long.parseLong(assetId);
			List<String> filePaths =  new ArrayList<String>();

			if (assetType.equals("audio")) {
				app.audioEncodeDb.dbEncoded.deleteSingleRow(assetId);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSkipped.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(assetId);
				for (String fileExtension : new String[] { "opus", "flac" }) {
					filePaths.add(RfcxAudioFileUtils.getAudioFileLocation_GZip(rfcxDeviceId, context, numericAssetId, fileExtension));
					filePaths.add(RfcxAudioFileUtils.getAudioFileLocation_Queue(rfcxDeviceId, context, numericAssetId, fileExtension));
					filePaths.add(RfcxAudioFileUtils.getAudioFileLocation_Stash(rfcxDeviceId, context, numericAssetId, fileExtension));
					filePaths.add(RfcxAudioFileUtils.getAudioFileLocation_ExternalStorage(rfcxDeviceId, numericAssetId, fileExtension));
				}

			} else if (assetType.equals("screenshot")) {
				RfcxComm.deleteQuery("admin", "database_delete_row", "screenshots|" + assetId, app.getResolver());
				filePaths.add(RfcxScreenShotFileUtils.getScreenShotFileLocation_Queue(rfcxDeviceId, context, numericAssetId));

			} else if (assetType.equals("log")) {
				RfcxComm.deleteQuery("admin", "database_delete_row", "logs|" + assetId, app.getResolver());
				filePaths.add(RfcxLogcatFileUtils.getLogcatFileLocation_Queue(rfcxDeviceId, context, numericAssetId));

			} else if (assetType.equals("photo")) {
				RfcxComm.deleteQuery("admin", "database_delete_row", "photos|" + assetId, app.getResolver());
				filePaths.add(RfcxPhotoFileUtils.getPhotoFileLocation_Queue(rfcxDeviceId, context, numericAssetId));

			} else if (assetType.equals("video")) {
				RfcxComm.deleteQuery("admin", "database_delete_row", "videos|" + assetId, app.getResolver());
				filePaths.add(RfcxVideoFileUtils.getVideoFileLocation_Queue(rfcxDeviceId, context, numericAssetId));
				filePaths.add(RfcxVideoFileUtils.getVideoFileLocation_ExternalStorage(rfcxDeviceId, numericAssetId));

			} else if (assetType.equals("classifier")) {
				app.audioClassifierDb.dbActive.deleteSingleRow(assetId);
				filePaths.add(RfcxClassifierFileUtils.getClassifierFileLocation_Active(context, numericAssetId));
				filePaths.add(RfcxClassifierFileUtils.getClassifierFileLocation_Cache(context, numericAssetId));

			} else if (assetType.equals("sms")) {
				RfcxComm.deleteQuery("admin", "database_delete_row", "sms|" + assetId, app.getResolver());

			} else if (assetType.equals("meta")) {
				app.metaDb.dbMeta.deleteSingleRowByTimestamp(assetId);
				app.assetExchangeLogDb.dbPurged.insert(assetType, assetId);

			} else if (assetType.equals("instruction")) {
				app.instructionsDb.dbExecuted.deleteSingleRowById(assetId);
				app.instructionsDb.dbQueued.deleteSingleRowById(assetId);

			} else if (assetType.equals("segment")) {
				app.apiSegmentUtils.deleteSegmentsById(assetId);

			} else if (assetType.equals("classification")) {
//				app.apiSegmentUtils.deleteSegmentsById(assetId);

			}

			// delete asset file after it has been purged from records
			for (String filePath : filePaths) {
				if ((filePath != null) && (new File(filePath)).exists()) {
					FileUtils.delete(filePath);
					app.assetExchangeLogDb.dbPurged.insert(assetType, assetId);
					purgedAssetReport = new String[] { assetType, assetId, RfcxAssetCleanup.conciseFilePath(filePath, RfcxGuardian.APP_ROLE) };
				}
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		return purgedAssetReport;
	}

	public String getAssetExchangeLogList(String assetStatus, int rowLimit) {

		List<String[]> assetRows = new ArrayList<String[]>();
		if (assetStatus.equalsIgnoreCase("purged")) {
			assetRows = app.assetExchangeLogDb.dbPurged.getLatestRowsWithLimitExcludeCreatedAt(rowLimit);
		}
		return DbUtils.getConcatRows(assetRows);
	}



	// Asset Cleanup

	public void runFileSystemAssetCleanup() {
		runFileSystemAssetCleanup(ScheduledAssetCleanupService.ASSET_CLEANUP_CYCLE_DURATION_MINUTES);
	}

	public void runFileSystemAssetCleanup(int checkFilesUnModifiedSinceThisManyMinutes) {

		String[] assetDirectoriesToScan = new String[] {
			// Misc Asset Directories
			RfcxScreenShotFileUtils.screenShotQueueDir(app.getApplicationContext()),
			RfcxLogcatFileUtils.logcatQueueDir(app.getApplicationContext()),
			RfcxPhotoFileUtils.photoQueueDir(app.getApplicationContext()),
			RfcxVideoFileUtils.videoQueueDir(app.getApplicationContext()),
			// Audio File Directories
			RfcxAudioFileUtils.audioStashDir(app.getApplicationContext()),
			RfcxAudioFileUtils.audioFinalDir(app.getApplicationContext()),
			RfcxAudioFileUtils.audioQueueDir(app.getApplicationContext()),
			RfcxAudioFileUtils.audioClassifyDir(app.getApplicationContext()),
			// Audio Library Directories
			RfcxAudioFileUtils.audioLibraryDir(app.getApplicationContext()),
			RfcxAudioFileUtils.audioCacheDir(app.getApplicationContext()),
			// Classifier Library Directories
			RfcxClassifierFileUtils.classifierLibraryDir(app.getApplicationContext()),
			RfcxClassifierFileUtils.classifierCacheDir(app.getApplicationContext()),
			RfcxClassifierFileUtils.classifierActiveDir(app.getApplicationContext())
		};

		List<String> assetFilePathsFromDatabase = new ArrayList<String>();
		// CheckIn Databases
		for (String[] row : app.apiCheckInDb.dbQueued.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		for (String[] row : app.apiCheckInDb.dbStashed.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		for (String[] row : app.apiCheckInDb.dbSent.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		for (String[] row : app.apiCheckInDb.dbSkipped.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		// Audio Encode Databases
		for (String[] row : app.audioEncodeDb.dbEncoded.getAllRows()) { assetFilePathsFromDatabase.add(row[10]); }
		for (String[] row : app.audioClassifyDb.dbQueued.getAllRows()) { assetFilePathsFromDatabase.add(row[6]); }
		// Asset Library Databases
		for (String[] row : app.assetLibraryDb.dbAudio.getAllRows()) { assetFilePathsFromDatabase.add(row[5]); }
		for (String[] row : app.assetLibraryDb.dbClassifier.getAllRows()) { assetFilePathsFromDatabase.add(row[5]); }
		// Classifier Databases
		for (String[] row : app.audioClassifierDb.dbActive.getAllRows()) { assetFilePathsFromDatabase.add(row[6]); }


		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(assetDirectoriesToScan, assetFilePathsFromDatabase, checkFilesUnModifiedSinceThisManyMinutes);

	}

}
