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

	public void processApiCommandJson(String jsonStr) {

		if (!jsonStr.equalsIgnoreCase("{}")) {

			try {

				Log.i(logTag, "Command JSON: " + jsonStr);

				JSONObject jsonObj = new JSONObject(jsonStr);

				// parse audio info and use it to purge the data locally
				// this assumes that the audio array has only one item in it
				// multiple audio items returned in this array would cause an error
				if (RfcxAsset.doesJsonHaveIndex(jsonObj, "audio")) {
					JSONArray audJson = jsonObj.has("aud") ? jsonObj.getJSONArray("aud") : jsonObj.getJSONArray("audio");
					String audId = jsonObj.has("aud") ? audJson.getString(0) : audJson.getJSONObject(0).getString("id");
					app.assetUtils.purgeSingleAsset("audio", audId);

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
				if (jsonObj.has("screenshots") || jsonObj.has("scn")) {
					JSONArray scnJson = jsonObj.has("scn") ? jsonObj.getJSONArray("scn") : jsonObj.getJSONArray("screenshots");
					List<String> scnIds = new ArrayList<>();
					for (int i = 0; i < scnJson.length(); i++) {
						scnIds.add( jsonObj.has("scn") ? scnJson.getString(i) : scnJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("screenshot", scnIds);
				}

				// parse log info and use it to purge the data locally
				if (jsonObj.has("logs") || jsonObj.has("log")) {
					JSONArray logJson = jsonObj.has("log") ? jsonObj.getJSONArray("log") : jsonObj.getJSONArray("logs");
					List<String> logIds = new ArrayList<>();
					for (int i = 0; i < logJson.length(); i++) {
						logIds.add( jsonObj.has("log") ? logJson.getString(i) : logJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("log", logIds);
				}

				// parse sms info and use it to purge the data locally
				if (jsonObj.has("messages") || jsonObj.has("sms")) {
					JSONArray smsJson = jsonObj.has("sms") ? jsonObj.getJSONArray("sms") : jsonObj.getJSONArray("messages");
					List<String> smsIds = new ArrayList<>();
					for (int i = 0; i < smsJson.length(); i++) {
						smsIds.add( jsonObj.has("sms") ? smsJson.getString(i) : smsJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("sms", smsIds);
				}

				// parse 'meta' info and use it to purge the data locally
				if (jsonObj.has("meta") || jsonObj.has("mta")) {
					JSONArray mtaJson = jsonObj.has("mta") ? jsonObj.getJSONArray("mta") : jsonObj.getJSONArray("meta");
					List<String> mtaIds = new ArrayList<>();
					for (int i = 0; i < mtaJson.length(); i++) {
						mtaIds.add( jsonObj.has("mta") ? mtaJson.getString(i) : mtaJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("meta", mtaIds);
				}

				// parse photo info and use it to purge the data locally
				if (jsonObj.has("photos") || jsonObj.has("pho")) {
					JSONArray phoJson = jsonObj.has("pho") ? jsonObj.getJSONArray("pho") : jsonObj.getJSONArray("photos");
					List<String> phoIds = new ArrayList<>();
					for (int i = 0; i < phoJson.length(); i++) {
						phoIds.add( jsonObj.has("pho") ? phoJson.getString(i) : phoJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("photo", phoIds);
				}

				// parse video info and use it to purge the data locally
				if (jsonObj.has("videos") || jsonObj.has("vid")) {
					JSONArray vidJson = jsonObj.has("vid") ? jsonObj.getJSONArray("vid") : jsonObj.getJSONArray("videos");
					List<String> vidIds = new ArrayList<>();
					for (int i = 0; i < vidJson.length(); i++) {
						vidIds.add( jsonObj.has("vid") ? vidJson.getString(i) : vidJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("video", vidIds);
				}

				// parse segment info and use it to purge the data locally
				if (jsonObj.has("segments") || jsonObj.has("seg")) {
					JSONArray segJson = jsonObj.has("seg") ? jsonObj.getJSONArray("seg") : jsonObj.getJSONArray("segments");
					List<String> segIds = new ArrayList<>();
					for (int i = 0; i < segJson.length(); i++) {
						segIds.add( jsonObj.has("seg") ? segJson.getString(i) : segJson.getJSONObject(i).getString("id") );
					}
					app.assetUtils.purgeListOfAssets("segment", segIds);
				}

				// parse 'purged' confirmation array and delete entries from asset exchange log
				if (jsonObj.has("purged") || jsonObj.has("prg")) {
					JSONArray prgJson = jsonObj.has("prg") ? jsonObj.getJSONArray("prg") : jsonObj.getJSONArray("purged");
					List<String> prgIds = new ArrayList<>();
					for (int i = 0; i < prgJson.length(); i++) {
						JSONObject prgObj = prgJson.getJSONObject(i);
						if (	prgObj.has("type") && prgObj.has("id")
							&&	ArrayUtils.doesStringArrayContainString( RfcxAsset.TYPES, prgObj.getString("type") )
						) {
							prgIds.add(prgObj.getString("id"));
						} else {
							for (String typeAbbrev : RfcxAsset.TYPES_ABBREV) {
								if (prgObj.has(typeAbbrev)) {
									JSONArray prgArr = prgObj.getJSONArray(typeAbbrev);
									for (int j = 0; j < prgArr.length(); j++) {
										prgIds.add(prgArr.getString(j));
									}
								}
							}
						}
					}
					for (String prgId : prgIds) { app.assetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(prgId); }
				}

				// parse generic 'received' info and use it to purge the data locally
				if (jsonObj.has("received") || jsonObj.has("rec")) {
					JSONArray recJson = jsonObj.has("rec") ? jsonObj.getJSONArray("rec") : jsonObj.getJSONArray("received");
					for (int i = 0; i < recJson.length(); i++) {
						JSONObject recObj = recJson.getJSONObject(i);
						if (	recObj.has("type") && recObj.has("id")
							&&	ArrayUtils.doesStringArrayContainString( RfcxAsset.TYPES, recObj.getString("type") )
						) {
							app.assetUtils.purgeSingleAsset(recObj.getString("type"), recObj.getString("id"));
						} else {
							for (String typeAbbrev : RfcxAsset.TYPES_ABBREV) {
								if (recObj.has(typeAbbrev)) {
									JSONArray recArr = recObj.getJSONArray(typeAbbrev);
									for (int j = 0; j < recArr.length(); j++) {
										app.assetUtils.purgeSingleAsset(typeAbbrev, recArr.getString(j));
									}
								}
							}
						}
					}
				}

				// parse 'unconfirmed' array
				if (jsonObj.has("unconfirmed") || jsonObj.has("unc")) {
					JSONArray uncJson = jsonObj.has("unc") ? jsonObj.getJSONArray("unc") : jsonObj.getJSONArray("unconfirmed");
					for (int i = 0; i < uncJson.length(); i++) {
						JSONObject uncObj = uncJson.getJSONObject(i);
						if ( uncObj.has("id") && uncObj.has("type") && uncObj.getString("type").equalsIgnoreCase("audio") ) {
							app.apiCheckInUtils.reQueueAudioAssetForCheckIn("sent", uncObj.getString("id"));
						} else if (uncObj.has("aud")) {
							JSONArray uncArr = uncObj.getJSONArray("aud");
							for (int j = 0; j < uncArr.length(); j++) {
								app.apiCheckInUtils.reQueueAudioAssetForCheckIn("sent", uncArr.getString(j));
							}
						}
					}
				}

				// parse 'prefs' array
				if (jsonObj.has("prefs") || jsonObj.has("prf")) {
					JSONArray prfJson = jsonObj.has("prf") ? jsonObj.getJSONArray("prf") : jsonObj.getJSONArray("prefs");
					for (int i = 0; i < prfJson.length(); i++) {
						JSONObject prfObj = prfJson.getJSONObject(i);
						if (prfObj.has("sha1")) {
							app.rfcxPrefs.prefsSha1FullApiSync = prfObj.getString("sha1").toLowerCase();
						}
					}
				}

				// parse 'instructions' array
				if (jsonObj.has("instructions") || jsonObj.has("ins")) {
					app.instructionsUtils.processReceivedInstructionJson((new JSONObject()).put("instructions", jsonObj.has("ins") ? jsonObj.getJSONArray("ins") : jsonObj.getJSONArray("instructions")));
					//	app.rfcxServiceHandler.triggerService("InstructionsExecution", false);
				}

			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);

			}
		}
	}





}
