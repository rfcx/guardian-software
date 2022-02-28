package org.rfcx.guardian.guardian.asset.library;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAsset;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AssetLibraryUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetLibraryUtils");
    private RfcxGuardian app;

    public AssetLibraryUtils(Context context) {

        this.app = (RfcxGuardian) context.getApplicationContext();

    }

    public String getLibraryAssetFilePath(String assetType, String assetId, String fileType) {

        Context context = app.getApplicationContext();
        long numericAssetId = Long.parseLong(assetId);

        if (assetType.equalsIgnoreCase("classifier")) {
            return RfcxClassifierFileUtils.getClassifierFileLocation_Library(context, numericAssetId);

        } else if (assetType.equalsIgnoreCase("audio")) {
            return RfcxAudioFileUtils.getAudioFileLocation_Library(context, numericAssetId, fileType);

        }/* else if (assetType.equalsIgnoreCase("apk")) {

		}*/

        return null;
    }


    public JSONObject getLibraryInfoAsJson() {

        JSONObject libObj = new JSONObject();
        try {

            JSONArray classifierLibArr = new JSONArray();
            for (String[] classifierRow : app.assetLibraryDb.dbClassifier.getAllRows()) {
                if (classifierRow[0] != null) {
                    String classifierId = classifierRow[1];
                    JSONObject classifierJsonMeta = new JSONObject(classifierRow[7]);
                    String classifierName = classifierJsonMeta.getString("classifier_name");
                    String classifierVersion = classifierJsonMeta.getString("classifier_version");

                    JSONObject classifierObj = new JSONObject();
                    classifierObj.put("id", classifierId);
                    classifierObj.put("guid", classifierName + "-v" + classifierVersion);
                    classifierLibArr.put(classifierObj);

                    app.assetLibraryDb.dbClassifier.updateLastAccessedAtById(classifierId);
                }
            }
            libObj.put(RfcxAsset.getTypePlural("classifier"), classifierLibArr);

            JSONArray audioLibArr = new JSONArray();
            for (String[] audioRow : app.assetLibraryDb.dbAudio.getAllRows()) {
                if (audioRow[0] != null) {
                    String audioId = audioRow[1];
                    JSONObject audioJsonMeta = new JSONObject(audioRow[7]);

                    JSONObject audioObj = new JSONObject();
                    audioObj.put("id", audioId);
                    audioLibArr.put(audioObj);

                    app.assetLibraryDb.dbAudio.updateLastAccessedAtById(audioId);
                }
            }
            libObj.put(RfcxAsset.getTypePlural("audio"), audioLibArr);

        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);
        }
        return libObj;
    }

    public int getLibraryAssetCount() {
        return app.assetLibraryDb.dbAudio.getCount() + app.assetLibraryDb.dbClassifier.getCount();
    }

}
