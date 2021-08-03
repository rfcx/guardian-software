package org.rfcx.guardian.guardian.asset.detections;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AudioDetectionJsonUtils {

	public AudioDetectionJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioDetectionJsonUtils");

	private RfcxGuardian app;




	public JSONObject retrieveAndBundleDetectionJson(JSONObject inputDtcnJson, int maxDtcnRowsToBundle, boolean overrideFilterByLastAccessedAt) throws JSONException {

		JSONObject dtcnJsonBundledSnapshotsObj = inputDtcnJson;
		JSONArray dtcnJsonBundledSnapshotsIds = new JSONArray();

		List<String[]> dtcnRows = (overrideFilterByLastAccessedAt) ? app.metaDb.dbMeta.getLatestRowsWithLimit(maxDtcnRowsToBundle) :
				app.metaDb.dbMeta.getLatestRowsNotAccessedSinceWithLimit( (System.currentTimeMillis() - app.apiMqttUtils.getSetCheckInPublishTimeOutLength()), maxDtcnRowsToBundle);

		for (String[] dtcnRow : dtcnRows) {

			// add meta snapshot ID to array of IDs
			dtcnJsonBundledSnapshotsIds.put(dtcnRow[1]);

			// if this is the first row to be examined, initialize the bundled object with this JSON blob
			if (dtcnJsonBundledSnapshotsObj == null) {
				dtcnJsonBundledSnapshotsObj = new JSONObject(dtcnRow[2]);

			} else {
				JSONObject metaJsonObjToAppend = new JSONObject(dtcnRow[2]);

				Iterator<String> appendKeys = metaJsonObjToAppend.keys();
				Iterator<String> bundleKeys = dtcnJsonBundledSnapshotsObj.keys();
				List<String> allKeys = new ArrayList<>();
				while (bundleKeys.hasNext()) { String bndlKey = bundleKeys.next(); if (!ArrayUtils.doesStringListContainString(allKeys, bndlKey)) { allKeys.add(bndlKey); } }
				while (appendKeys.hasNext()) { String apnKey = appendKeys.next(); if (!ArrayUtils.doesStringListContainString(allKeys, apnKey)) { allKeys.add(apnKey); } }

				for (String jsonKey : allKeys) {

					if (	!dtcnJsonBundledSnapshotsObj.has(jsonKey)
							&&	metaJsonObjToAppend.has(jsonKey)
							&&	(metaJsonObjToAppend.get(jsonKey) instanceof String)
					) {
						String newStr = metaJsonObjToAppend.getString(jsonKey);
						dtcnJsonBundledSnapshotsObj.put(jsonKey, newStr);

					} else if (	dtcnJsonBundledSnapshotsObj.has(jsonKey)
							&&	(dtcnJsonBundledSnapshotsObj.get(jsonKey) instanceof String)
							&&	metaJsonObjToAppend.has(jsonKey)
							&&	(metaJsonObjToAppend.get(jsonKey) instanceof String)
					) {
						String origStr = dtcnJsonBundledSnapshotsObj.getString(jsonKey);
						String newStr = metaJsonObjToAppend.getString(jsonKey);
						if ( (origStr.length() > 0) && (newStr.length() > 0) ) {
							dtcnJsonBundledSnapshotsObj.put(jsonKey, origStr+"|"+newStr);
						} else {
							dtcnJsonBundledSnapshotsObj.put(jsonKey, origStr+newStr);
						}

					}
				}
			}

			// Overwrite meta_ids attribute with updated array of snapshot IDs
			dtcnJsonBundledSnapshotsObj.put("meta_ids", dtcnJsonBundledSnapshotsIds);

			// mark this row as accessed in the database
			app.metaDb.dbMeta.updateLastAccessedAtByTimestamp(dtcnRow[1]);

			// if the bundle already contains max number of snapshots, stop here
			if (dtcnJsonBundledSnapshotsIds.length() >= maxDtcnRowsToBundle) { break; }
		}

		// if no meta data was available to bundle, then we create an empty object
		if (dtcnJsonBundledSnapshotsObj == null) { dtcnJsonBundledSnapshotsObj = new JSONObject(); }


		return dtcnJsonBundledSnapshotsObj;
	}


}
