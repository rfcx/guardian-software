package org.rfcx.guardian.utility.rfcx;

import android.content.ContentResolver;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RfcxStatus {

	public RfcxStatus(String appRole, String fetchTargetRole, RfcxGuardianIdentity rfcxGuardianIdentity, ContentResolver contentResolver) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxStatus");
		this.fetchTargetRole = ArrayUtils.doesStringArrayContainString(validFetchTargetRoles, fetchTargetRole) ? fetchTargetRole.toLowerCase(Locale.US) : null;
		this.rfcxGuardianIdentity = rfcxGuardianIdentity;
		this.contentResolver = contentResolver;
		initializeCaches();
	}

	protected String logTag;
	private RfcxGuardianIdentity rfcxGuardianIdentity;
	private ContentResolver contentResolver;

	protected static final String[] activityTypes =	new String[] { "audio_capture", "api_checkin" };
	protected static final String[] statusTypes = 	new String[] { "allowed", "enabled" };

	protected static final String[] validFetchTargetRoles = new String[] { "guardian", "admin" };
	private String fetchTargetRole;

	private static final boolean[] statusValueLocalFallbacks = new boolean[] { true, true };
	private static final boolean[] statusValueFetchedFallbacks = new boolean[] { true, true };

	private final boolean[] hasLocalCache = new boolean[] { false, false };
	private final Map<Integer, boolean[]> lastLocalValue = new HashMap<>();
	private final Map<Integer, long[]> lastLocalValueSetAt = new HashMap<>();

	private boolean hasFetchedCache = false;
	private long lastFetchedValueSetAt = 0;
	private Map<Integer, boolean[]> lastFetchedValue = new HashMap<>();

	private long localValueCacheExpiresAfter = 10000;
	private long fetchedValueCacheExpiresAfter = 20000;



	// This should be over-ridden within each role that makes use of this class
	protected boolean getStatusBasedOnRoleSpecificLogic(int activityType, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {

		boolean statusValue = fallbackValue;

		// This is where the role specific logic would be.
		// Just in case this function is not over-ridden, we report an error below, so that we'll be informed.
		Log.e(logTag, "Using Generic 'getStatusBasedOnRoleSpecificLogic' function. No role-specific functionality could be included.");

		return statusValue;
	}


	//
	//	Everything below this part should be generic, and usable across all roles that make use of status information
	//

	private void updateLocalStatus(int activityType, boolean printFeedbackInLog) {
		boolean[] updatedStatus = lastLocalValue.containsKey(activityType) ? lastLocalValue.get(activityType) : statusValueLocalFallbacks;
		for (int statusType = 0; statusType < statusTypes.length; statusType++) {
			updatedStatus[statusType] = getStatusBasedOnRoleSpecificLogic(activityType, statusType, updatedStatus[statusType], printFeedbackInLog);
		}
		lastLocalValue.put(activityType, updatedStatus);
		lastLocalValueSetAt.put(activityType, new long[] { System.currentTimeMillis(), System.currentTimeMillis() } );
		hasLocalCache[activityType] = true;
	}

	private void updateFetchedStatus() {
		Map<Integer, boolean[]> _lastFetchedValue = lastFetchedValue;
		try {
			if ((fetchTargetRole != null) && isGuardianRegistered()) {
				Log.w(logTag, "Refreshing fetched status values via Content Provider...");
				JSONArray jsonArray = RfcxComm.getQuery(fetchTargetRole, "status", "*", contentResolver);
				if (jsonArray.length() > 0) {
					JSONObject jsonObj = jsonArray.getJSONObject(0);
					for (int activityType = 0; activityType < activityTypes.length; activityType++) {
						boolean[] updatedStatus = _lastFetchedValue.containsKey(activityType) ? _lastFetchedValue.get(activityType) : statusValueFetchedFallbacks;
						if (jsonObj.has(activityTypes[activityType])) {
							JSONObject activityStatusObj = jsonObj.getJSONObject(activityTypes[activityType]);
							for (int statusType = 0; statusType < statusTypes.length; statusType++) {
								if (activityStatusObj.has("is_"+statusTypes[statusType])) {
									updatedStatus[statusType] = activityStatusObj.getBoolean("is_" + statusTypes[statusType]);
								}
							}
							_lastFetchedValue.put(activityType, updatedStatus);
						}
					}
				}
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		lastFetchedValue = _lastFetchedValue;
		lastFetchedValueSetAt = System.currentTimeMillis();
		hasFetchedCache = true;
	}

	private boolean getFetchedStatus(int activityType, int statusType) {
		if  ( hasFetchedCache && (Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(lastFetchedValueSetAt)) <= fetchedValueCacheExpiresAfter) ) {
			return lastFetchedValue.get(activityType)[statusType];
		} else {
			updateFetchedStatus();
			return lastFetchedValue.containsKey(activityType) ? lastFetchedValue.get(activityType)[statusType] : statusValueFetchedFallbacks[statusType];
		}
	}

	public boolean getFetchedStatus(String activityType, String statusType) {
		return getFetchedStatus(getActivityType(activityType), getStatusType(statusType));
	}

	private boolean getLocalStatus(int activityType, int statusType, boolean printFeedbackInLog) {

		if  ( 	hasLocalCache[activityType] && lastLocalValueSetAt.containsKey(activityType)
			&&	(Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(lastLocalValueSetAt.get(activityType)[statusType])) <= localValueCacheExpiresAfter) ) {
			return lastLocalValue.get(activityType)[statusType];
		} else {
			updateLocalStatus(activityType, printFeedbackInLog);
			Log.w(logTag, "Refreshed local status cache: "+activityTypes[activityType]);
			return lastLocalValue.containsKey(activityType) ? lastLocalValue.get(activityType)[statusType] : statusValueLocalFallbacks[statusType];
		}
	}

	public boolean getLocalStatus(String activityType, String statusType, boolean printFeedbackInLog) {
		return getLocalStatus(getActivityType(activityType), getStatusType(statusType), printFeedbackInLog);
	}

	protected static int getActivityType(String activityType) {
		return ArrayUtils.indexOfStringInStringArray(activityTypes, activityType);
	}

	protected static int getStatusType(String statusType) {
		return ArrayUtils.indexOfStringInStringArray(statusTypes, statusType);
	}

	private JSONObject getLocalStatusAsJsonObj(int activityType) throws JSONException {
		JSONObject statusObj = new JSONObject();
		for (int statusType = 0; statusType < statusTypes.length; statusType++) {
			statusObj.put("is_"+statusTypes[statusType], getLocalStatus(activityType, statusType, false));
		}
		return statusObj;
	}

	private JSONObject getLocalStatusAsJsonObj(String activityType) throws JSONException {
		return getLocalStatusAsJsonObj(getActivityType(activityType));
	}

	public JSONArray getCompositeLocalStatusAsJsonArr() {
		JSONArray compositeStatusArr = new JSONArray();
		JSONObject compositeStatusObj = new JSONObject();
		try {
			for (int activityType = 0; activityType < activityTypes.length; activityType++) {
				compositeStatusObj.put(activityTypes[activityType], getLocalStatusAsJsonObj(activityType));
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		compositeStatusArr.put(compositeStatusObj);
		return compositeStatusArr;
	}

	private void initializeCaches() {
		for (int activityType = 0; activityType < activityTypes.length; activityType++) {
			boolean[] valueArr = new boolean[statusTypes.length];
			long[] valueSetAtArr = new long[statusTypes.length];
			for (int statusType = 0; statusType < statusTypes.length; statusType++) {
				valueArr[statusType] = true;
				valueSetAtArr[statusType] = 0;
			}

			lastLocalValue.put(activityType, valueArr);
			lastLocalValueSetAt.put(activityType, valueSetAtArr);
			hasLocalCache[activityType] = false;

			lastFetchedValue.put(activityType, valueArr);
			lastFetchedValueSetAt = 0;
			hasFetchedCache = false;
		}
	}

	public void setOrResetCacheExpirations(int audioCycleDurationInSeconds) {
		long audioCycleDuration =  audioCycleDurationInSeconds * 1000;
		localValueCacheExpiresAfter = Math.min( Math.max( Math.round( audioCycleDuration / 13.5 ), 3333 ), 10000 );
		fetchedValueCacheExpiresAfter = Math.min( Math.max( Math.round( audioCycleDuration / 8.0 ), 5000 ), 15000 );
		Log.w(logTag, "Updated Status Cache Timeouts "
				+"- Local: "+DateTimeUtils.milliSecondDurationAsReadableString(localValueCacheExpiresAfter)
				+", Fetched: "+DateTimeUtils.milliSecondDurationAsReadableString(fetchedValueCacheExpiresAfter));
	}

	private boolean isGuardianRegistered() {
		return (rfcxGuardianIdentity.getAuthToken() != null);
	}

}
