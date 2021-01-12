package org.rfcx.guardian.guardian.api.methods.download;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssetDownloadUtils {

	public AssetDownloadUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		this.downloadDirectoryPath = context.getFilesDir().getAbsolutePath()+"/downloads";
		FileUtils.initializeDirectoryRecursively(this.downloadDirectoryPath, false);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiDownloadUtils");

	private RfcxGuardian app;
	public String downloadDirectoryPath;

	public static final int DOWNLOAD_FAILURE_SKIP_THRESHOLD = 5;

	public void createDummyRow() {

		long classifierId = Long.parseLong("1610252810821");

		app.assetDownloadDb.dbQueued.insert(
				"classifier",
				""+classifierId,
				"b905c4091870453d19591af400b02f161552fad0",
				"http",
				"http://install.rfcx.org/rfcx-guardian/guardian-asset-classifier/1610252810820.tflite.gz",
				12476227,
				"tflite",
				"{"
						+"\"classifier_name\":\"threat\","
						+"\"classifier_version\":\"1\","
						+"\"sample_rate\":\"12000\","
						+"\"window_size\":\"0.9750\","
						+"\"step_size\":\"1\","
						+"\"classifications\":\"chainsaw,gunshot,vehicle\""
						+"}"
				);

	}




	public String getTmpAssetFilePath(String assetType, String assetId) {

		return this.downloadDirectoryPath + "/" + assetType + "_" + assetId+".download";
	}

	public String getPostDownloadAssetFilePath(String assetType, String assetId, String fileType) {

		Context context = app.getApplicationContext();
		long numericAssetId = Long.parseLong(assetId);

		if (assetType.equalsIgnoreCase("classifier")) {
			return RfcxClassifierFileUtils.getClassifierFileLocation_Download(context, numericAssetId);

		} else if (assetType.equalsIgnoreCase("audio")) {
			return RfcxAudioFileUtils.getAudioFileLocation_Download(context, numericAssetId, fileType);

		}

		return null;
	}


	public void followUpOnSuccessfulDownload(String assetType, String assetId, String fileType, String checksum, String metaJsonBlob) throws IOException {

		Log.i(logTag, "Following up on successful download...");

		String tmpPath = getPostDownloadAssetFilePath(assetType, assetId, fileType);
		String galleryPath = app.assetGalleryUtils.getGalleryAssetFilePath(assetType, assetId, fileType);
		FileUtils.initializeDirectoryRecursively(galleryPath.substring(0, galleryPath.lastIndexOf("/")), false);

		FileUtils.delete(galleryPath);
		FileUtils.copy(tmpPath, galleryPath);

		if (assetType.equalsIgnoreCase("classifier") && (app.assetGalleryDb.dbClassifier.getCountByAssetId(assetId) == 0)) {

			app.assetGalleryDb.dbClassifier.insert( assetId, "classifier", fileType, checksum, galleryPath,
					FileUtils.getFileSizeInBytes(galleryPath), metaJsonBlob,0,0);

			app.audioClassifyUtils.activateClassifier(assetId);

		} else if (assetType.equalsIgnoreCase("audio") && (app.assetGalleryDb.dbAudio.getCountByAssetId(assetId) == 0)) {

			app.assetGalleryDb.dbAudio.insert( assetId, "audio", fileType, checksum, galleryPath,
					FileUtils.getFileSizeInBytes(galleryPath), metaJsonBlob, 0, 0);

		}

		FileUtils.delete(tmpPath);
	}



	public void cleanupDownloadDirectory(List<String[]> queuedForDownload, long maxAgeInMilliseconds) {

		ArrayList<String> assetsQueuedForDownload = new ArrayList<String>();
		for (String[] queuedRow : queuedForDownload) {
	//		assetsQueuedForDownload.add(queuedRow[6]);
		}

		String[] dirsToScan = new String[]{ this.downloadDirectoryPath };

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( dirsToScan, assetsQueuedForDownload, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
