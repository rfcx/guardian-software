package org.rfcx.guardian.guardian.api.methods.command;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAsset;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ApiCommandUtils {

	public ApiCommandUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCommandUtils");

	private final RfcxGuardian app;

	public void processApiCommandJson(String jsonStr, String originProtocol) {

		if (!jsonStr.equalsIgnoreCase("{}")) {

			try {

				Log.i(logTag, "Command JSON: " + jsonStr);

				JSONObject jsonObj = new JSONObject(jsonStr);

				// parse audio info and use it to purge the data locally
				// this assumes that the audio array has only one item in it
				// multiple audio items returned in this array would cause an error
				if (RfcxAsset.doesJsonHaveField(jsonObj, RfcxAsset.Type.AUDIO)) {
					JSONArray audJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.AUDIO);
					String audId = jsonObj.has("aud") ? audJson.getString(0) : audJson.getJSONObject(0).getString("id");
					app.assetUtils.purgeSingleAsset(RfcxAsset.Type.AUDIO, audId);

					if (jsonObj.has("checkin_id") || jsonObj.has("chk")) {
						String chkId = jsonObj.has("chk") ? jsonObj.getString("chk") : jsonObj.getString("checkin_id");
						if (chkId.length() > 0) {
							long[] checkInStats = app.apiCheckInHealthUtils.getInFlightCheckInStatsEntry(audId);
							if (checkInStats != null) {
								app.latencyStatsDb.dbCheckInLatency.insert(chkId, checkInStats[1], checkInStats[2]);
								Calendar rightNow = GregorianCalendar.getInstance();
								rightNow.setTime(new Date());

								app.apiCheckInUtils.reQueueStashedCheckInIfAllowedByHealthCheck(new long[]{
										/* latency */        checkInStats[1],
										/* queued */        (long) app.apiCheckInDb.dbQueued.getCount(),
										/* recent */        checkInStats[0]
								});
							}
						}
					}
					app.apiCheckInHealthUtils.updateInFlightCheckInOnReceive(audId);
				}

				// parse screenshot info and use it to purge the data locally
				JSONArray scnJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.SCREENSHOT);
				List<String> scnIds = new ArrayList<>();
				for (int i = 0; i < scnJson.length(); i++) {
					scnIds.add( jsonObj.has("scn") ? scnJson.getString(i) : scnJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.SCREENSHOT, scnIds);


				// parse log info and use it to purge the data locally
				JSONArray logJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.LOG);
				List<String> logIds = new ArrayList<>();
				for (int i = 0; i < logJson.length(); i++) {
					logIds.add( jsonObj.has("log") ? logJson.getString(i) : logJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.LOG, logIds);


				// parse sms info and use it to purge the data locally
				JSONArray smsJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.SMS);
				List<String> smsIds = new ArrayList<>();
				for (int i = 0; i < smsJson.length(); i++) {
					smsIds.add( jsonObj.has("sms") ? smsJson.getString(i) : smsJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.SMS, smsIds);


				// parse 'meta' info and use it to purge the data locally
				JSONArray mtaJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.META);
				List<String> mtaIds = new ArrayList<>();
				for (int i = 0; i < mtaJson.length(); i++) {
					mtaIds.add( jsonObj.has("mta") ? mtaJson.getString(i) : mtaJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.META, mtaIds);


				// parse photo info and use it to purge the data locally
				JSONArray phoJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.PHOTO);
				List<String> phoIds = new ArrayList<>();
				for (int i = 0; i < phoJson.length(); i++) {
					phoIds.add( jsonObj.has("pho") ? phoJson.getString(i) : phoJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.PHOTO, phoIds);


				// parse video info and use it to purge the data locally
				JSONArray vidJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.VIDEO);
				List<String> vidIds = new ArrayList<>();
				for (int i = 0; i < vidJson.length(); i++) {
					vidIds.add( jsonObj.has("vid") ? vidJson.getString(i) : vidJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.VIDEO, vidIds);


				// parse segment info and use it to purge the data locally
				JSONArray segJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.SEGMENT);
				List<String> segIds = new ArrayList<>();
				for (int i = 0; i < segJson.length(); i++) {
					segIds.add( jsonObj.has("seg") ? segJson.getString(i) : segJson.getJSONObject(i).getString("id") );
				}
				app.assetUtils.purgeListOfAssets(RfcxAsset.Type.SEGMENT, segIds);


				// parse 'purged' confirmation array and delete entries from asset exchange log
				JSONArray prgJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Status.PURGED);
				List<String> prgIds = new ArrayList<>();
				for (int i = 0; i < prgJson.length(); i++) {
					JSONObject prgObj = prgJson.getJSONObject(i);
					if ( prgObj.has("type") && prgObj.has("id") ) {
						String assetType = RfcxAsset.getType(prgObj.getString("type"));
						if (assetType != null) {
							prgIds.add(prgObj.getString("id"));
						}
					}
				}
				for (String prgId : prgIds) { app.assetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(prgId); }


				// parse generic 'received' info and use it to purge the data locally
				JSONArray recJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Status.RECEIVED);
				for (int i = 0; i < recJson.length(); i++) {
					JSONObject recObj = recJson.getJSONObject(i);
					if ( recObj.has("type") && recObj.has("id") ) {
						String assetType = RfcxAsset.getType(recObj.getString("type"));
						if (assetType != null) {
							app.assetUtils.purgeSingleAsset(assetType, recObj.getString("id"));
						}
					}
				}

				// parse 'unconfirmed' array
				JSONArray uncJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Status.UNCONFIRMED);
				for (int i = 0; i < uncJson.length(); i++) {
					JSONObject uncObj = uncJson.getJSONObject(i);
					if ( uncObj.has("type") && uncObj.has("id") ) {
						String assetType = RfcxAsset.getType(uncObj.getString("type"));
						if ((assetType != null) && assetType.equalsIgnoreCase(RfcxAsset.Type.AUDIO)){
							app.apiCheckInUtils.reQueueAudioAssetForCheckIn("sent", uncObj.getString("id"));
						}
					}
				}

				// parse 'prefs' array
				JSONArray prfJson = RfcxAsset.getJsonArrayForField(jsonObj, RfcxAsset.Type.PREF);
				for (int i = 0; i < prfJson.length(); i++) {
					JSONObject prfObj = prfJson.getJSONObject(i);
					if (prfObj.has("sha1")) {
						if (!originProtocol.equalsIgnoreCase("socket")) {
							app.rfcxPrefs.prefsSha1FullApiSync = prfObj.getString("sha1").toLowerCase();
						}
					}
				}

				// parse 'instructions' array
				if (RfcxAsset.doesJsonHaveField(jsonObj, RfcxAsset.Type.INSTRUCTION)) {
					app.instructionsUtils.processReceivedInstructionJson((new JSONObject()).put("instructions", jsonObj.has("ins") ? jsonObj.getJSONArray("ins") : jsonObj.getJSONArray("instructions")), originProtocol);
				}

			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);

			}
		}
	}



//	private static JSONArray getJsonArrForType(String assetTypeOrStatus) {
//
//
//
//	}


}
