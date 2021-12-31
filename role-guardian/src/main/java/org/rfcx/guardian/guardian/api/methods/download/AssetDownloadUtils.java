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

//				app.assetDownloadDb.dbQueued.insert(
//				"classifier",
//				"1637739334295",
//				"331e7a8c2453c9ca27c61be6018d9f4098d9c366",
//				"http",
//				"http://192.168.1.102:8080/cdn/tflite/asianelephant_v5.tflite.gz",
//				13635432,
//				"tflite",
//				"{"
//						+"\"classifier_name\":\"asianelephant\","
//						+"\"classifier_version\":\"5\","
//						+"\"sample_rate\":\"8000\","
//						+"\"input_gain\":\"1.0\","
//						+"\"window_size\":\"3.5000\","
//						+"\"step_size\":\"1\","
//						+"\"classifications\":\"asianelephant,environment\","
//						+"\"classifications_filter_threshold\":\"0.99,1.00\""
//						+"}"
//		);

//		app.assetDownloadDb.dbQueued.insert(
//				"classifier",
//				"1637901623151",
//				"69482d8b65083e2fabcf1096033c863409cc50f7",
//				"http",
//				"http://192.168.43.107:8080/cdn/tflite/asia-elephant-edge_v2.tflite.gz",
//				12469754,
//				"tflite",
//				"{"
//						+"\"classifier_name\":\"asia-elephant-edge\","
//						+"\"classifier_version\":\"2\","
//						+"\"sample_rate\":\"8000\","
//						+"\"input_gain\":\"1.0\","
//						+"\"window_size\":\"2.5000\","
//						+"\"step_size\":\"2.0000\","
//						+"\"classifications\":\"elephas_maximus,environment\","
//						+"\"classifications_filter_threshold\":\"0.98,1.00\""
//						+"}"
//		);

		app.assetDownloadDb.dbQueued.insert(
				"classifier",
				"1617208867756",
				"accfb018701e52696835c9d1c02600a67a228db1",
				"http",
				"https://rfcx-install.s3.eu-west-1.amazonaws.com/rfcx-guardian/guardian-asset-classifier/1617208867756.tflite.gz",
				12465841,
				"tflite",
				"{"
						+"\"classifier_name\":\"chainsaw\","
						+"\"classifier_version\":\"5\","
						+"\"sample_rate\":\"12000\","
						+"\"input_gain\":\"1.0\","
						+"\"window_size\":\"0.9750\","
						+"\"step_size\":\"1\","
						+"\"classifications\":\"chainsaw,environment\","
						+"\"classifications_filter_threshold\":\"0.95,1.00\""
						+"}"
				);


//		app.assetDownloadDb.dbQueued.insert(
//				"classifier",
//				"1617208867757",
//				"5ad4aafdf92cbb4c2fc795962548a711581273aa",
//				"http",
//				"http://192.168.1.102:8080/cdn/tflite/chainsaw_v2.tflite.gz",
//				12500443,
//				"tflite",
//				"{"
//						+"\"classifier_name\":\"chainsaw\","
//						+"\"classifier_version\":\"2\","
//						+"\"sample_rate\":\"12000\","
//						+"\"input_gain\":\"1.0\","
//						+"\"window_size\":\"0.9750\","
//						+"\"step_size\":\"1\","
//						+"\"classifications\":\"chainsaw,environment\","
//						+"\"classifications_filter_threshold\":\"0.90,1.00\""
//						+"}"
//		);

	}




	public String getTmpAssetFilePath(String assetType, String assetId) {

		return this.downloadDirectoryPath + "/" + assetType + "_" + assetId+".download";
	}

	public String getPostDownloadAssetFilePath(String assetType, String assetId, String fileType) {

		Context context = app.getApplicationContext();
		long numericAssetId = Long.parseLong(assetId);

		if (assetType.equalsIgnoreCase("classifier")) {
			return RfcxClassifierFileUtils.getClassifierFileLocation_Cache(context, numericAssetId);

		} else if (assetType.equalsIgnoreCase("audio")) {
			return RfcxAudioFileUtils.getAudioFileLocation_Cache(context, numericAssetId, fileType);

		}

		return null;
	}


	public void followUpOnSuccessfulDownload(String assetType, String assetId, String fileType, String checksum, String metaJsonBlob) throws IOException {

		Log.i(logTag, "Following up on successful download...");

		String tmpPath = getPostDownloadAssetFilePath(assetType, assetId, fileType);
		String libraryPath = app.assetLibraryUtils.getLibraryAssetFilePath(assetType, assetId, fileType);
		FileUtils.initializeDirectoryRecursively(libraryPath.substring(0, libraryPath.lastIndexOf("/")), false);

		FileUtils.delete(libraryPath);
		FileUtils.copy(tmpPath, libraryPath);

		if (assetType.equalsIgnoreCase("classifier") && (app.assetLibraryDb.dbClassifier.getCountByAssetId(assetId) == 0)) {

			app.assetLibraryDb.dbClassifier.insert( assetId, fileType, checksum, libraryPath,
					FileUtils.getFileSizeInBytes(libraryPath), metaJsonBlob,0,0);

			app.audioClassifyUtils.activateClassifier(assetId);

		} else if (assetType.equalsIgnoreCase("audio") && (app.assetLibraryDb.dbAudio.getCountByAssetId(assetId) == 0)) {

			app.assetLibraryDb.dbAudio.insert( assetId, fileType, checksum, libraryPath,
					FileUtils.getFileSizeInBytes(libraryPath), metaJsonBlob, 0, 0);

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
