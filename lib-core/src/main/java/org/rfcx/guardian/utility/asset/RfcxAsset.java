package org.rfcx.guardian.utility.asset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RfcxAsset {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "RfcxAsset");
    private static final Map<String, String> typePlural = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put(Type.AUDIO, "audio");
                put(Type.META, "meta");
                put(Type.SCREENSHOT, "screenshots");
                put(Type.LOG, "logs");
                put(Type.PHOTO, "photos");
                put(Type.VIDEO, "videos");
                put(Type.SMS, "messages");
                put(Type.APK, "apks");
                put(Type.DETECTION, "detections");
                put(Type.CLASSIFIER, "classifiers");
                put(Type.INSTRUCTION, "instructions");
                put(Type.PREF, "prefs");
                put(Type.SEGMENT, "segments");
                put(Type.SNIPPET, "snippets");
            }}
    );
    private static final Map<String, String> typeAbbrev = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put(Type.AUDIO, "aud");
                put(Type.META, "mta");
                put(Type.SCREENSHOT, "scn");
                put(Type.LOG, "log");
                put(Type.PHOTO, "pho");
                put(Type.VIDEO, "vid");
                put(Type.SMS, "sms");
                put(Type.APK, "apk");
                put(Type.DETECTION, "det");
                put(Type.CLASSIFIER, "cls");
                put(Type.INSTRUCTION, "ins");
                put(Type.PREF, "prf");
                put(Type.SEGMENT, "seg");
                put(Type.SNIPPET, "sni");
            }}
    );
    private static final Map<String, String> statusAbbrev = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put(Status.PURGED, "prg");
                put(Status.RECEIVED, "rec");
                put(Status.UNCONFIRMED, "unc");
            }}
    );

    public static JSONArray getJsonArrayForField(JSONObject jsonObj, String assetTypeOrStatus) throws JSONException {
        JSONArray jsonArr = new JSONArray();
        String assetType = getAssetTypeName(assetTypeOrStatus);
        if (assetType != null) {
            if (jsonObj.has(assetType)) {
                jsonArr = jsonObj.getJSONArray(assetType);
            } else if (jsonObj.has(getTypeAbbrev(assetType))) {
                jsonArr = jsonObj.getJSONArray(getTypeAbbrev(assetType));
            } else if (jsonObj.has(getTypePlural(assetType))) {
                jsonArr = jsonObj.getJSONArray(getTypePlural(assetType));
            }
        } else {
            String assetStatus = getStatus(assetTypeOrStatus);
            if (assetStatus != null) {
                if (jsonObj.has(assetStatus)) {
                    jsonArr = jsonObj.getJSONArray(assetStatus);
                } else if (jsonObj.has(getStatusAbbrev(assetStatus))) {
                    jsonArr = jsonObj.getJSONArray(getStatusAbbrev(assetStatus));
                }
            }
        }
        return jsonArr;
    }

    public static boolean isValidType(String assetType) {
        return typeAbbrev.containsKey(assetType);
    }


//	public static boolean doesJsonHaveField(JSONObject jsonObj, String assetTypeOrStatus) {
//		boolean hasField = jsonObj.has(assetTypeOrStatus);
//		if (!hasField && isValidType(assetTypeOrStatus)) {
//			hasField = jsonObj.has(getTypeAbbrev(assetTypeOrStatus)) || jsonObj.has(getTypePlural(assetTypeOrStatus));
//		} else if (!hasField && isValidStatus(assetTypeOrStatus)) {
//			hasField = jsonObj.has(getStatusAbbrev(assetTypeOrStatus));
//		}
//		return hasField;
//	}

    public static boolean isValidStatus(String assetStatus) {
        return statusAbbrev.containsKey(assetStatus);
    }

    public static String getTypePlural(String assetType) {
        return typePlural.get(assetType);
    }

    public static String getTypeAbbrev(String assetType) {
        return typeAbbrev.get(assetType);
    }

    public static String getStatusAbbrev(String assetStatus) {
        return statusAbbrev.get(assetStatus);
    }

    public static String getAssetTypeName(String label) {
        String labeledAs = label.toLowerCase(Locale.US);
        if (isValidType(labeledAs)) {
            return labeledAs;
        } else if (typeAbbrev.containsValue(labeledAs)) {
            return getHashMapKeyByValue(typeAbbrev, labeledAs);
        } else if (typePlural.containsValue(labeledAs)) {
            return getHashMapKeyByValue(typePlural, labeledAs);
        }
        return null;
    }

    public static String getStatus(String label) {
        String labeledAs = label.toLowerCase(Locale.US);
        if (isValidStatus(labeledAs)) {
            return labeledAs;
        } else if (statusAbbrev.containsValue(labeledAs)) {
            return getHashMapKeyByValue(statusAbbrev, labeledAs);
        }
        return null;
    }

    private static String getHashMapKeyByValue(Map<String, String> hashMap, String entryValue) {
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(entryValue)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static final class Type {
        public static final String AUDIO = "audio";
        public static final String META = "meta";
        public static final String SCREENSHOT = "screenshot";
        public static final String LOG = "log";
        public static final String PHOTO = "photo";
        public static final String VIDEO = "video";
        public static final String SMS = "sms";
        public static final String APK = "apk";
        public static final String DETECTION = "detection";
        public static final String CLASSIFIER = "classifier";
        public static final String INSTRUCTION = "instruction";
        public static final String PREF = "pref";
        public static final String SEGMENT = "segment";
        public static final String SNIPPET = "snippet";
    }

    public static final class Status {
        public static final String PURGED = "purged";
        public static final String RECEIVED = "received";
        public static final String UNCONFIRMED = "unconfirmed";
    }

}
