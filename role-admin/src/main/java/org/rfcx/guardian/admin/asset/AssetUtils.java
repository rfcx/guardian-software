package org.rfcx.guardian.admin.asset;

import android.content.Context;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxLogcatUtils;
import org.rfcx.guardian.utility.asset.RfcxPhotoUtils;
import org.rfcx.guardian.utility.asset.RfcxScreenShotUtils;
import org.rfcx.guardian.utility.asset.RfcxVideoUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class AssetUtils {

	public AssetUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetUtils");

	private RfcxGuardian app;



	// Asset Cleanup


	private int checkFilesUnModifiedSinceThisManyMinutes = 120;

	public void runFileSystemAssetCleanup(int checkFilesUnModifiedSinceThisManyMinutes) {
		this.checkFilesUnModifiedSinceThisManyMinutes = checkFilesUnModifiedSinceThisManyMinutes;
		runFileSystemAssetCleanup();
	}

	public void runFileSystemAssetCleanup() {

		String[] assetDirectoriesToScan = new String[] {
				RfcxPhotoUtils.photoCaptureDir(app.getApplicationContext()),
				RfcxPhotoUtils.photoQueueDir(app.getApplicationContext()),
				RfcxVideoUtils.videoCaptureDir(app.getApplicationContext()),
				RfcxVideoUtils.videoQueueDir(app.getApplicationContext()),
				RfcxLogcatUtils.logCaptureDir(app.getApplicationContext()),
				RfcxLogcatUtils.logQueueDir(app.getApplicationContext()),
				RfcxLogcatUtils.logPostCaptureDir(app.getApplicationContext()),
				RfcxScreenShotUtils.screenShotCaptureDir(app.getApplicationContext()),
				RfcxScreenShotUtils.screenShotQueueDir(app.getApplicationContext())
		};

		List<String> assetFilePathsFromDatabase = new ArrayList<String>();
		// Admin Asset Databases
		for (String[] row : app.screenShotDb.dbCaptured.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		for (String[] row : app.cameraCaptureDb.dbPhotos.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		for (String[] row : app.cameraCaptureDb.dbVideos.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }
		for (String[] row : app.logcatDb.dbCaptured.getAllRows()) { assetFilePathsFromDatabase.add(row[4]); }


		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(assetDirectoriesToScan, assetFilePathsFromDatabase, checkFilesUnModifiedSinceThisManyMinutes);

	}

}
