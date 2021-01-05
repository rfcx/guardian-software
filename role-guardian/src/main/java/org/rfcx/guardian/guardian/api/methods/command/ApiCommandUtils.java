package org.rfcx.guardian.guardian.api.methods.command;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

	public void processApiCommandJson(String jsonStr) {

		if (!jsonStr.equalsIgnoreCase("{}")) {

			try {

				Log.i(logTag, "Command JSON: " + jsonStr);

				JSONObject jsonObj = new JSONObject(jsonStr);

				// parse audio info and use it to purge the data locally
				// this assumes that the audio array has only one item in it
				// multiple audio items returned in this array would cause an error
				if (jsonObj.has("audio")) {
					JSONArray audioJson = jsonObj.getJSONArray("audio");
					String audioId = audioJson.getJSONObject(0).getString("id");
					app.assetUtils.purgeSingleAsset("audio", audioId);

					if (jsonObj.has("checkin_id")) {
						String checkInId = jsonObj.getString("checkin_id");
						if (checkInId.length() > 0) {
							long[] checkInStats = app.apiCheckInHealthUtils.getInFlightCheckInStatsEntry(audioId);
							if (checkInStats != null) {
								app.apiCheckInStatsDb.dbStats.insert(checkInId, checkInStats[1], checkInStats[2]);
								Calendar rightNow = GregorianCalendar.getInstance();
								rightNow.setTime(new Date());

								app.apiCheckInUtils.reQueueStashedCheckInIfAllowedByHealthCheck(new long[]{
										/* latency */        checkInStats[1],
										/* queued */        (long) app.apiCheckInDb.dbQueued.getCount(),
										/* recent */        checkInStats[0],
										/* time-of-day */   (long) rightNow.get(Calendar.HOUR_OF_DAY)
								});
							}
						}
					}
					app.apiCheckInHealthUtils.updateInFlightCheckInOnReceive(audioId);
				}

				// parse screenshot info and use it to purge the data locally
				if (jsonObj.has("screenshots")) {
					JSONArray screenShotJson = jsonObj.getJSONArray("screenshots");
					List<String> screenshotIds = new ArrayList<>();
					for (int i = 0; i < screenShotJson.length(); i++) {
						screenshotIds.add(screenShotJson.getJSONObject(i).getString("id"));
					}
					app.assetUtils.purgeListOfAssets("screenshot", screenshotIds);
				}

				// parse log info and use it to purge the data locally
				if (jsonObj.has("logs")) {
					JSONArray logsJson = jsonObj.getJSONArray("logs");
					List<String> logsIds = new ArrayList<>();
					for (int i = 0; i < logsJson.length(); i++) {
						logsIds.add(logsJson.getJSONObject(i).getString("id"));
					}
					app.assetUtils.purgeListOfAssets("log", logsIds);
				}

				// parse sms info and use it to purge the data locally
				if (jsonObj.has("messages")) {
					JSONArray smsJson = jsonObj.getJSONArray("messages");
					List<String> smsIds = new ArrayList<>();
					for (int i = 0; i < smsJson.length(); i++) {
						smsIds.add(smsJson.getJSONObject(i).getString("id"));
					}
					app.assetUtils.purgeListOfAssets("sms", smsIds);
				}

				// parse 'meta' info and use it to purge the data locally
				if (jsonObj.has("meta")) {
					JSONArray metaJson = jsonObj.getJSONArray("meta");
					List<String> metaIds = new ArrayList<>();
					for (int i = 0; i < metaJson.length(); i++) {
						metaIds.add(metaJson.getJSONObject(i).getString("id"));
					}
					app.assetUtils.purgeListOfAssets("meta", metaIds);
				}

				// parse photo info and use it to purge the data locally
				if (jsonObj.has("photos")) {
					JSONArray photoJson = jsonObj.getJSONArray("photos");
					List<String> photoIds = new ArrayList<>();
					for (int i = 0; i < photoJson.length(); i++) {
						photoIds.add(photoJson.getJSONObject(i).getString("id"));
					}
					app.assetUtils.purgeListOfAssets("photo", photoIds);
				}

				// parse video info and use it to purge the data locally
				if (jsonObj.has("videos")) {
					JSONArray videoJson = jsonObj.getJSONArray("videos");
					List<String> videoIds = new ArrayList<>();
					for (int i = 0; i < videoJson.length(); i++) {
						videoIds.add(videoJson.getJSONObject(i).getString("id"));
					}
					app.assetUtils.purgeListOfAssets("video", videoIds);
				}

				// parse 'purged' confirmation array and delete entries from asset exchange log
				if (jsonObj.has("purged")) {
					JSONArray purgedJson = jsonObj.getJSONArray("purged");
					for (int i = 0; i < purgedJson.length(); i++) {
						JSONObject purgedObj = purgedJson.getJSONObject(i);
						if (purgedObj.has("type") && purgedObj.has("id")) {
							String assetId = purgedObj.getString("id");
							String assetType = purgedObj.getString("type");
							if (assetType.equalsIgnoreCase("meta")
									|| assetType.equalsIgnoreCase("audio")
									|| assetType.equalsIgnoreCase("screenshot")
									|| assetType.equalsIgnoreCase("log")
									|| assetType.equalsIgnoreCase("photo")
									|| assetType.equalsIgnoreCase("video")
							) {
								app.assetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(assetId);
							}
						}
					}
				}

				// parse generic 'received' info and use it to purge the data locally
				if (jsonObj.has("received")) {
					JSONArray receivedJson = jsonObj.getJSONArray("received");
					for (int i = 0; i < receivedJson.length(); i++) {
						JSONObject receivedObj = receivedJson.getJSONObject(i);
						if (receivedObj.has("type") && receivedObj.has("id")) {
							String assetId = receivedObj.getString("id");
							String assetType = receivedObj.getString("type");
							app.assetUtils.purgeSingleAsset(assetType, assetId);
						}
					}
				}

				// parse 'unconfirmed' array
				if (jsonObj.has("unconfirmed")) {
					JSONArray unconfirmedJson = jsonObj.getJSONArray("unconfirmed");
					for (int i = 0; i < unconfirmedJson.length(); i++) {
						String assetId = unconfirmedJson.getJSONObject(i).getString("id");
						String assetType = unconfirmedJson.getJSONObject(i).getString("type");
						if (assetType.equalsIgnoreCase("audio")) {
							app.apiCheckInUtils.reQueueAudioAssetForCheckIn("sent", assetId);
						}
					}
				}

				// parse 'prefs' array
				if (jsonObj.has("prefs")) {
					JSONArray prefsJson = jsonObj.getJSONArray("prefs");
					for (int i = 0; i < prefsJson.length(); i++) {
						JSONObject prefsObj = prefsJson.getJSONObject(i);
						if (prefsObj.has("sha1")) {
							app.rfcxPrefs.prefsSha1FullApiSync = prefsObj.getString("sha1").toLowerCase();
						}
					}
				}

				// parse 'instructions' array
				if (jsonObj.has("instructions")) {
					app.instructionsUtils.processReceivedInstructionJson((new JSONObject()).put("instructions", jsonObj.getJSONArray("instructions")));
					//	app.rfcxServiceHandler.triggerService("InstructionsExecution", false);
				}

				// parse segment info and use it to purge the data locally
				if (jsonObj.has("segment")) {
					JSONArray segJson = jsonObj.getJSONArray("segment");
					List<String> segIds = new ArrayList<>();
					for (int i = 0; i < segJson.length(); i++) {
						segIds.add( segJson.getString(i) );
					}
					app.assetUtils.purgeListOfAssets("segment", segIds);
				}

			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);

			}
		}
	}





}
