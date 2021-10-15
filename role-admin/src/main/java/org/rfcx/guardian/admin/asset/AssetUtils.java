package org.rfcx.guardian.admin.asset;

import android.content.Context;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxLogcatFileUtils;
import org.rfcx.guardian.utility.asset.RfcxPhotoFileUtils;
import org.rfcx.guardian.utility.asset.RfcxScreenShotFileUtils;
import org.rfcx.guardian.utility.asset.RfcxVideoFileUtils;
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


    public void runFileSystemAssetCleanup() {
        runFileSystemAssetCleanup(ScheduledAssetCleanupService.ASSET_CLEANUP_CYCLE_DURATION_MINUTES);
    }

    public void runFileSystemAssetCleanup(int checkFilesUnModifiedSinceThisManyMinutes) {

        String[] assetDirectoriesToScan = new String[]{
                RfcxPhotoFileUtils.photoCaptureDir(app.getApplicationContext()),
                RfcxPhotoFileUtils.photoQueueDir(app.getApplicationContext()),
                RfcxVideoFileUtils.videoCaptureDir(app.getApplicationContext()),
                RfcxVideoFileUtils.videoQueueDir(app.getApplicationContext()),
                RfcxLogcatFileUtils.logcatCaptureDir(app.getApplicationContext()),
                RfcxLogcatFileUtils.logcatQueueDir(app.getApplicationContext()),
                RfcxLogcatFileUtils.logcatPostCaptureDir(app.getApplicationContext()),
                RfcxScreenShotFileUtils.screenShotCaptureDir(app.getApplicationContext()),
                RfcxScreenShotFileUtils.screenShotQueueDir(app.getApplicationContext())
        };

        List<String> assetFilePathsFromDatabase = new ArrayList<String>();
        // Admin Asset Databases
        for (String[] row : app.screenShotDb.dbCaptured.getAllRows()) {
            assetFilePathsFromDatabase.add(row[4]);
        }
        for (String[] row : app.cameraCaptureDb.dbPhotos.getAllRows()) {
            assetFilePathsFromDatabase.add(row[4]);
        }
        for (String[] row : app.cameraCaptureDb.dbVideos.getAllRows()) {
            assetFilePathsFromDatabase.add(row[4]);
        }
        for (String[] row : app.logcatDb.dbCaptured.getAllRows()) {
            assetFilePathsFromDatabase.add(row[4]);
        }


        (new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(assetDirectoriesToScan, assetFilePathsFromDatabase, checkFilesUnModifiedSinceThisManyMinutes);

    }

}
