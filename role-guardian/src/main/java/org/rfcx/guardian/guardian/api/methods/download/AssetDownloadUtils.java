package org.rfcx.guardian.guardian.api.methods.download;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssetDownloadUtils {

    public static final int DOWNLOAD_FAILURE_SKIP_THRESHOLD = 5;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiDownloadUtils");
    public String downloadDirectoryPath;
    private final RfcxGuardian app;

    public AssetDownloadUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.downloadDirectoryPath = context.getFilesDir().getAbsolutePath() + "/downloads";
        FileUtils.initializeDirectoryRecursively(this.downloadDirectoryPath, false);
    }

    public void createDummyRow() {

        app.assetDownloadDb.dbQueued.insert(
                "classifier",
                "1617208867756",
                "accfb018701e52696835c9d1c02600a67a228db1",
                "http",
                "https://rfcx-install.s3.eu-west-1.amazonaws.com/rfcx-guardian/guardian-asset-classifier/1617208867756.tflite.gz",
                12465841,
                "tflite",
                "{"
                        + "\"classifier_name\":\"chainsaw\","
                        + "\"classifier_version\":\"5\","
                        + "\"sample_rate\":\"12000\","
                        + "\"input_gain\":\"1.0\","
                        + "\"window_size\":\"0.9750\","
                        + "\"step_size\":\"1\","
                        + "\"classifications\":\"chainsaw,environment\","
                        + "\"classifications_filter_threshold\":\"0.95,1.00\""
                        + "}"
        );

    }

    public void createPreClassifierValues(Context context) {
        String assetId = "1617208867756";
        String filePath = RfcxClassifierFileUtils.getClassifierFileLocation_Active(context, Long.parseLong(assetId));
        Uri fileOriginUri = RfcxComm.getFileUri("classify", RfcxAssetCleanup.conciseFilePath(filePath, RfcxGuardian.APP_ROLE));
        String fileChecksum = FileUtils.sha1Hash(filePath);
        if (new File(filePath).exists()) {
            return;
        }
        RfcxComm.getFileRequest(fileOriginUri, filePath, app.getResolver());

        String fileType = "tflite";
        String metaJsonBlob = "{"
                + "\"classifier_name\":\"chainsaw\","
                + "\"classifier_version\":\"5\","
                + "\"sample_rate\":\"12000\","
                + "\"input_gain\":\"1.0\","
                + "\"window_size\":\"0.9750\","
                + "\"step_size\":\"1\","
                + "\"classifications\":\"chainsaw,environment\","
                + "\"classifications_filter_threshold\":\"0.95,1.00\""
                + "}";

        String[] existClassifier = app.assetLibraryDb.dbClassifier.getSingleRowById(assetId);
        if (existClassifier[1] != null && existClassifier[1].equals(assetId)) {
            return;
        }
        app.assetLibraryDb.dbClassifier.insert(
                assetId,
                fileType,
                fileChecksum,
                filePath,
                FileUtils.getFileSizeInBytes(filePath),
                metaJsonBlob,
                0,
                0
        );
        app.audioClassifyUtils.activateClassifier(assetId);
    }

    public String getTmpAssetFilePath(String assetType, String assetId) {

        return this.downloadDirectoryPath + "/" + assetType + "_" + assetId + ".download";
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

            app.assetLibraryDb.dbClassifier.insert(assetId, fileType, checksum, libraryPath,
                    FileUtils.getFileSizeInBytes(libraryPath), metaJsonBlob, 0, 0);

            app.audioClassifyUtils.activateClassifier(assetId);

        } else if (assetType.equalsIgnoreCase("audio") && (app.assetLibraryDb.dbAudio.getCountByAssetId(assetId) == 0)) {

            app.assetLibraryDb.dbAudio.insert(assetId, fileType, checksum, libraryPath,
                    FileUtils.getFileSizeInBytes(libraryPath), metaJsonBlob, 0, 0);

        }

        FileUtils.delete(tmpPath);
    }


    public void cleanupDownloadDirectory(List<String[]> queuedForDownload, long maxAgeInMilliseconds) {

        ArrayList<String> assetsQueuedForDownload = new ArrayList<String>();
        for (String[] queuedRow : queuedForDownload) {
            //		assetsQueuedForDownload.add(queuedRow[6]);
        }

        String[] dirsToScan = new String[]{this.downloadDirectoryPath};

        (new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(dirsToScan, assetsQueuedForDownload, Math.round(maxAgeInMilliseconds / 60000), false, false);
    }

}
