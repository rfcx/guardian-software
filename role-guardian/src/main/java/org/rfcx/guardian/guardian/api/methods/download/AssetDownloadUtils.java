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
				"tflite"
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

		}/* else if (assetType.equalsIgnoreCase("apk")) {

		}*/

		return null;
	}

	public String getFinalAssetFilePath(String assetType, String assetId, String fileType) {

		Context context = app.getApplicationContext();
		long numericAssetId = Long.parseLong(assetId);

		if (assetType.equalsIgnoreCase("classifier")) {
			return RfcxClassifierFileUtils.getClassifierFileLocation_Active(context, numericAssetId);

		} else if (assetType.equalsIgnoreCase("audio")) {
			return RfcxAudioFileUtils.getAudioFileLocation_Library(context, numericAssetId, fileType);

		}/* else if (assetType.equalsIgnoreCase("apk")) {

		}*/

		return null;
	}


	public void followUpOnSuccessfulDownload(String assetType, String assetId, String fileType) throws IOException {

		Log.i(logTag, "Following up on successful download...");

		String tmpPath = getPostDownloadAssetFilePath(assetType, assetId, fileType);
		String finalPath = getFinalAssetFilePath(assetType, assetId, fileType);
		FileUtils.initializeDirectoryRecursively(finalPath.substring(0, finalPath.lastIndexOf("/")), false);

		if (assetType.equalsIgnoreCase("classifier")) {

			FileUtils.copy(tmpPath, finalPath);

			// Using dummy data for development
			app.audioClassifierDb.dbActive.insert(
					assetId, "guid", "1", "tflite", "checksum",
					finalPath, 12000, "0.975", "1", "chainsaw,gunshot,vehicle"
			);

			FileUtils.delete(tmpPath);

		} else if (assetType.equalsIgnoreCase("audio")) {

//			FileUtils.copy(tmpPath, finalPath);

		}/* else if (assetType.equalsIgnoreCase("apk")) {

		}*/

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
