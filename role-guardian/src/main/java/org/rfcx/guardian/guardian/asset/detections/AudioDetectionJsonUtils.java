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


    public JSONObject retrieveAndBundleDetectionJson(JSONObject insertDetectionsInto, int maxDtcnRowsToBundle, boolean overrideFilterByLastAccessedAt) throws JSONException {

        if (insertDetectionsInto == null) {
            insertDetectionsInto = new JSONObject();
        }

        JSONArray dtcnIds = new JSONArray();
        ArrayList<String> dtcnList = new ArrayList<>();

        List<String[]> dtcnRows = (overrideFilterByLastAccessedAt) ? app.audioDetectionDb.dbFiltered.getLatestRowsWithLimit(maxDtcnRowsToBundle) :
                app.audioDetectionDb.dbFiltered.getLatestRowsNotAccessedSinceWithLimit((System.currentTimeMillis() - app.apiMqttUtils.getSetCheckInPublishTimeOutLength()), maxDtcnRowsToBundle);

        for (String[] dtcnRow : dtcnRows) {

            // add detection set ID to array of IDs
            dtcnIds.put(dtcnRow[0]);
            dtcnList.add(TextUtils.join("*", new String[]{dtcnRow[1], dtcnRow[3] + "-v" + dtcnRow[4], dtcnRow[7], "" + Math.round(Double.parseDouble(dtcnRow[8]) * 1000), dtcnRow[10]}));

            // mark this row as accessed in the database
            app.audioDetectionDb.dbFiltered.updateLastAccessedAtByCreatedAt(dtcnRow[0]);

            // if the bundle already contains max number of snapshots, stop here
            if (dtcnIds.length() >= maxDtcnRowsToBundle) {
                break;
            }
        }

        if (dtcnList.size() > 0) {
            insertDetectionsInto.put("detections", TextUtils.join("|", dtcnList));
            insertDetectionsInto.put("detection_ids", dtcnIds);
        }

        return insertDetectionsInto;
    }


}
