package org.rfcx.guardian.guardian.asset;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.camera.RfcxCameraUtils;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceLogCat;
import org.rfcx.guardian.utility.device.capture.DeviceScreenShot;
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
			JSONArray latestAssetMetaArray = RfcxComm.getQueryContentProvider("admin", "database_get_latest_row",
					assetType, app.getApplicationContext().getContentResolver());
			for (int i = 0; i < latestAssetMetaArray.length(); i++) {
				JSONObject latestAssetMeta = latestAssetMetaArray.getJSONObject(i);
				long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(latestAssetMeta.getString("last_accessed_at"))));
				if (milliSecondsSinceAccessed > excludeAssetsLastAccessWithinDuration) {
					assetMeta = new String[] { latestAssetMeta.getString("filepath"),
							latestAssetMeta.getString("created_at"), latestAssetMeta.getString("timestamp"),
							latestAssetMeta.getString("format"), latestAssetMeta.getString("digest"),
							latestAssetMeta.getString("width"), latestAssetMeta.getString("height") };
					RfcxComm.updateQueryContentProvider("admin", "database_set_last_accessed_at", assetType + "|" + latestAssetMeta.getString("timestamp"),
							app.getApplicationContext().getContentResolver());
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

	public void purgeSingleAsset(String assetType, String rfcxDeviceId, Context context, String assetId) {

		try {
			List<String> filePaths =  new ArrayList<String>();

			if (assetType.equals("audio")) {
				app.audioEncodeDb.dbEncoded.deleteSingleRow(assetId);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSkipped.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(assetId);
				for (String fileExtension : new String[] { "opus", "flac" }) {
					filePaths.add(RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(rfcxDeviceId, context, Long.parseLong(assetId), fileExtension));
					filePaths.add(RfcxAudioUtils.getAudioFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId), fileExtension));
				}

			} else if (assetType.equals("screenshot")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(DeviceScreenShot.getScreenShotFileLocation_Complete(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(DeviceScreenShot.getScreenShotFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("photo")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "photos|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(RfcxCameraUtils.getPhotoFileLocation_Complete_PostGZip(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(RfcxCameraUtils.getPhotoFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("video")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "videos|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(RfcxCameraUtils.getVideoFileLocation_Complete_PostGZip(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(RfcxCameraUtils.getVideoFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("log")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(DeviceLogCat.getLogFileLocation_Complete_PostZip(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(DeviceLogCat.getLogFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("sms")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "sms|" + assetId,
						app.getApplicationContext().getContentResolver());

			} else if (assetType.equals("meta")) {
				app.metaDb.dbMeta.deleteSingleRowByTimestamp(assetId);
				app.assetExchangeLogDb.dbPurged.insert(assetType, assetId);

			} else if (assetType.equals("instruction")) {
				app.instructionsDb.dbExecutedInstructions.deleteSingleRowById(assetId);
				app.instructionsDb.dbQueuedInstructions.deleteSingleRowById(assetId);

			}

			boolean isPurgeReported = false;
			// delete asset file after it has been purged from records
			for (String filePath : filePaths) {
				if ((filePath != null) && (new File(filePath)).exists()) {
					FileUtils.delete(filePath);
					app.assetExchangeLogDb.dbPurged.insert(assetType, assetId);
					Log.d(logTag, "Purging asset: " + assetType + ", " + assetId + ", " + filePath.substring(1 + filePath.lastIndexOf("/")));
					isPurgeReported = true;
				}
			}
			if (!isPurgeReported) { Log.d(logTag, "Purging asset: " + assetType + ", " + assetId); }

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}



	public String getAssetExchangeLogList(String assetStatus, int rowLimit) {

		List<String[]> assetRows = new ArrayList<String[]>();
		if (assetStatus.equalsIgnoreCase("purged")) {
			assetRows = app.assetExchangeLogDb.dbPurged.getLatestRowsWithLimitExcludeCreatedAt(rowLimit);
		}
		return DbUtils.getConcatRows(assetRows);
	}




}
