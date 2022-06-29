package org.rfcx.guardian.guardian.api.methods.command;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAsset;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.List;

public class ApiCommandUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCommandUtils");
    private final RfcxGuardian app;

    public ApiCommandUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
    }

    private static String extractAssetIdFromJsonArrayAtIndex(JSONArray jsonArr, int ind) throws JSONException {
        if ((jsonArr.get(ind) instanceof JSONObject) && jsonArr.getJSONObject(ind).has("id")) {
            return jsonArr.getJSONObject(ind).getString("id");
        } else if (jsonArr.get(ind) instanceof String) {
            return jsonArr.getString(ind);
        }
        return null;
    }

    private static String getAssetTypeFromStatusJson(JSONObject jsonObj) {
        if (jsonObj.has("type") && jsonObj.has("id")) {
            try {
                return RfcxAsset.getAssetTypeName(jsonObj.getString("type"));
            } catch (JSONException e) {
                RfcxLog.logExc(logTag, e);
            }
        }
        return null;
    }

    public void processApiCommandJson(String jsonStr, String originProtocol) {

        if (!jsonStr.equalsIgnoreCase("{}")) {

            try {

                Log.i(logTag, "Command JSON: " + jsonStr);

                JSONObject jsonObj = new JSONObject(jsonStr);


                // parse audio info and use it to purge the data locally
                JSONArray audioArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.AUDIO);
                List<String> audioIds = new ArrayList<>();
                for (int i = 0; i < audioArr.length(); i++) {
                    String audioId = extractAssetIdFromJsonArrayAtIndex(audioArr, i);
                    audioIds.add(audioId);
                    processCheckInId(jsonObj, audioId);
                    app.apiCheckInHealthUtils.updateInFlightCheckInOnReceive(audioId);
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.AUDIO, audioIds);


                // parse screenshot info and use it to purge the data locally
                JSONArray screenshotArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.SCREENSHOT);
                List<String> screenshotIds = new ArrayList<>();
                for (int i = 0; i < screenshotArr.length(); i++) {
                    screenshotIds.add(extractAssetIdFromJsonArrayAtIndex(screenshotArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.SCREENSHOT, screenshotIds);


                // parse log info and use it to purge the data locally
                JSONArray logArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.LOG);
                List<String> logIds = new ArrayList<>();
                for (int i = 0; i < logArr.length(); i++) {
                    logIds.add(extractAssetIdFromJsonArrayAtIndex(logArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.LOG, logIds);


                // parse sms info and use it to purge the data locally
                JSONArray smsArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.SMS);
                List<String> smsIds = new ArrayList<>();
                for (int i = 0; i < smsArr.length(); i++) {
                    smsIds.add(extractAssetIdFromJsonArrayAtIndex(smsArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.SMS, smsIds);


                // parse 'meta' info and use it to purge the data locally
                JSONArray metaArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.META);
                List<String> metaIds = new ArrayList<>();
                for (int i = 0; i < metaArr.length(); i++) {
                    metaIds.add(extractAssetIdFromJsonArrayAtIndex(metaArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.META, metaIds);


                // parse photo info and use it to purge the data locally
                JSONArray photoArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.PHOTO);
                List<String> photoIds = new ArrayList<>();
                for (int i = 0; i < photoArr.length(); i++) {
                    photoIds.add(extractAssetIdFromJsonArrayAtIndex(photoArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.PHOTO, photoIds);


                // parse video info and use it to purge the data locally
                JSONArray videoArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.VIDEO);
                List<String> videoIds = new ArrayList<>();
                for (int i = 0; i < videoArr.length(); i++) {
                    videoIds.add(extractAssetIdFromJsonArrayAtIndex(videoArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.VIDEO, videoIds);

                // parse detections info and use it to purge the data locally
                JSONArray detectionsArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.DETECTION);
                List<String> detectionsIds = new ArrayList<>();
                for (int i = 0; i < detectionsArr.length(); i++) {
                    detectionsIds.add( extractAssetIdFromJsonArrayAtIndex(detectionsArr, i) );
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.DETECTION, detectionsIds);


                // parse segment info and use it to purge the data locally
                JSONArray segmentArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.SEGMENT);
                List<String> segmentIds = new ArrayList<>();
                for (int i = 0; i < segmentArr.length(); i++) {
                    segmentIds.add(extractAssetIdFromJsonArrayAtIndex(segmentArr, i));
                }
                app.assetUtils.purgeListOfAssets(RfcxAsset.Type.SEGMENT, segmentIds);


                // parse 'instructions' array
                app.instructionsUtils.processReceivedInstructionJson(RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.INSTRUCTION), originProtocol);


                // parse 'purged' confirmation array and delete entries from asset exchange log
                JSONArray purgedArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Status.PURGED);
                List<String> purgedIds = new ArrayList<>();
                for (int i = 0; i < purgedArr.length(); i++) {
                    JSONObject purgedObj = purgedArr.getJSONObject(i);
                    String assetType = getAssetTypeFromStatusJson(purgedObj);
                    if (assetType != null) {
                        purgedIds.add(purgedObj.getString("id"));
                    }
                }
                for (String purgedId : purgedIds) {
                    app.assetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(purgedId);
                }


                // parse generic 'received' info and use it to purge the data locally
                JSONArray receivedArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Status.RECEIVED);
                for (int i = 0; i < receivedArr.length(); i++) {
                    JSONObject receivedObj = receivedArr.getJSONObject(i);
                    String assetType = getAssetTypeFromStatusJson(receivedObj);
                    if (assetType != null) {
                        app.assetUtils.purgeSingleAsset(assetType, receivedObj.getString("id"));
                    }
                }


                // parse 'unconfirmed' array
                JSONArray unconfirmedArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Status.UNCONFIRMED);
                for (int i = 0; i < unconfirmedArr.length(); i++) {
                    JSONObject unconfirmedObj = unconfirmedArr.getJSONObject(i);
                    String assetType = getAssetTypeFromStatusJson(unconfirmedObj);
                    if ((assetType != null) && assetType.equalsIgnoreCase(RfcxAsset.Type.AUDIO)) {
                        app.apiCheckInUtils.reQueueAudioAssetForCheckIn("sent", unconfirmedObj.getString("id"));
                    }
                }


                // parse 'prefs' array
                JSONArray prefArr = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.PREF);
                for (int i = 0; i < prefArr.length(); i++) {
                    JSONObject prefObj = prefArr.getJSONObject(i);
                    if (prefObj.has("sha1")) {
                        if (!originProtocol.equalsIgnoreCase("socket")) {
                            app.rfcxPrefs.prefsSync_Sha1Value = prefObj.getString("sha1").toLowerCase().substring(0, RfcxPrefs.prefsSync_Sha1CharLimit);
                        }
                    }
                }


            } catch (JSONException e) {
                RfcxLog.logExc(logTag, e);

            }
        }
    }

    private void processCheckInId(JSONObject jsonObj, String audId) throws JSONException {
        if (jsonObj.has("checkin_id")) {
            String chkId = jsonObj.getString("checkin_id");
            if (chkId.length() > 0) {
                long[] checkInStats = app.apiCheckInHealthUtils.getInFlightCheckInStatsEntry(audId);
                if (checkInStats != null) {
                    app.latencyStatsDb.dbCheckInLatency.insert(chkId, checkInStats[1], checkInStats[2]);
                    app.apiCheckInUtils.reQueueStashedCheckInIfAllowedByHealthCheck(new long[]{
                            /* latency */       checkInStats[1],
                            /* queued */        (long) app.apiCheckInDb.dbQueued.getCount(),
                            /* recent */        checkInStats[0]
                    });
                }
            }
        }

    }


}
