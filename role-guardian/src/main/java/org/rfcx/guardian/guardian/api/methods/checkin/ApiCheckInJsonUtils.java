package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ApiCheckInJsonUtils {

	public ApiCheckInJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInJsonUtils");

	private RfcxGuardian app;


	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta, String[] photoFileMeta, String[] videoFileMeta) throws JSONException, IOException {

		JSONObject jsonObj = app.metaJsonUtils.retrieveAndBundleMetaJson(null, app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT), false);

		// Adding Audio JSON fields from checkin table
		JSONObject checkInJsonObj = new JSONObject(checkInJsonString);
		jsonObj.put("queued_at", checkInJsonObj.getLong("queued_at"));
		jsonObj.put("audio", checkInJsonObj.getString("audio"));

		// Recording number of currently queued/skipped/stashed checkins
		jsonObj.put("checkins", app.metaJsonUtils.getCheckInStatusInfoForJson( new String[] { "sent" } ));

		jsonObj.put("purged", app.assetUtils.getAssetExchangeLogList("purged", 4 * app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT)));

		// Adding software role versions
		jsonObj.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));

		// Adding checksum of current prefs values
		jsonObj.put("prefs", app.metaJsonUtils.buildPrefsJsonObj(false));

		// Adding instructions, if there are any
		if (app.instructionsUtils.getInstructionsCount() > 0) {
			jsonObj.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		// Adding library assets, if there are any
		if (app.assetLibraryUtils.getLibraryAssetCount() > 0) {
			jsonObj.put("library", app.assetLibraryUtils.getLibraryInfoAsJson());
		}

		// Adding messages to JSON blob
		JSONArray smsArr = RfcxComm.getQuery("admin", "database_get_all_rows", "sms", app.getResolver());
		if (smsArr.length() > 0) { jsonObj.put("messages", smsArr); }

		// Adding screenshot meta to JSON blob
		if (screenShotMeta[0] != null) {
			jsonObj.put("screenshots", TextUtils.join("*", new String[]{screenShotMeta[1], screenShotMeta[2], screenShotMeta[3], screenShotMeta[4], screenShotMeta[5], screenShotMeta[6]}));
		}

		// Adding logs meta to JSON blob
		if (logFileMeta[0] != null) {
			jsonObj.put("logs", TextUtils.join("*", new String[]{logFileMeta[1], logFileMeta[2], logFileMeta[3], logFileMeta[4]}));
		}

		// Adding photos meta to JSON blob
		if (photoFileMeta[0] != null) {
			jsonObj.put("photos", TextUtils.join("*", new String[]{photoFileMeta[1], photoFileMeta[2], photoFileMeta[3], photoFileMeta[4], photoFileMeta[5], photoFileMeta[6]}));
		}

		// Adding videos meta to JSON blob
		if (videoFileMeta[0] != null) {
			jsonObj.put("videos", TextUtils.join("*", new String[]{videoFileMeta[1], videoFileMeta[2], videoFileMeta[3], videoFileMeta[4], videoFileMeta[5], videoFileMeta[6]}));
		}

		int limitLogsTo = 1500;
		String strLogs = jsonObj.toString();
		Log.d(logTag, (strLogs.length() <= limitLogsTo) ? strLogs : strLogs.substring(0, limitLogsTo) + "...");

		return jsonObj.toString();

	}

	public String buildCheckInQueueJson(String[] audioFileInfo) {

		try {
			JSONObject queueJson = new JSONObject();

			// Recording the moment the check in was queued
			queueJson.put("queued_at", System.currentTimeMillis());

			// Adding audio file metadata
			List<String> audioFiles = new ArrayList<String>();
			audioFiles.add(TextUtils.join("*", audioFileInfo));
			queueJson.put("audio", TextUtils.join("|", audioFiles));

			return queueJson.toString();

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
			return "{}";
		}
	}






}
