package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ApiCheckInHealthUtils {

	public ApiCheckInHealthUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInHealthUtils");

	private RfcxGuardian app;

	private Map<String, long[]> healthCheckMonitors = new HashMap<String, long[]>();
	private static final String[] healthCheckCategories = new String[] { "latency", "queued", "recent", "time-of-day" };
	private long[] healthCheckTargetLowerBounds = new long[healthCheckCategories.length];
	private long[] healthCheckTargetUpperBounds = new long[healthCheckCategories.length];
	private static final int healthCheckMeasurementCount = 6;
	private long[] healthCheckInitValues = new long[healthCheckMeasurementCount];
	private boolean doCheckInConditionsAllowCheckInRequeuing = false;

	private long lastKnownAudioCycleDuration = 0;
	private long lastKnownTimeOfDayLowerBound = 0;
	private long lastKnownTimeOfDayUpperBound = 0;

	private String inFlightCheckInAudioId = null;
	private String latestCheckInAudioId = null;
	private Map<String, String[]> inFlightCheckInEntries = new HashMap<String, String[]>();
	private Map<String, long[]> inFlightCheckInStats = new HashMap<String, long[]>();

	public void updateInFlightCheckInOnSend(String audioId, String[] checkInDbEntry) {
		this.inFlightCheckInAudioId = audioId;
		this.inFlightCheckInEntries.remove(audioId);
		this.inFlightCheckInEntries.put(audioId, checkInDbEntry);
	}

	public void updateInFlightCheckInOnReceive(String audioId) {
		this.latestCheckInAudioId = audioId;
		this.inFlightCheckInEntries.remove(audioId);
		this.inFlightCheckInStats.remove(audioId);
	}

	public String getInFlightCheckInAudioId() {
		return this.inFlightCheckInAudioId;
	}

	public String getLatestCheckInAudioId() {
		return this.latestCheckInAudioId;
	}

	public long[] getInFlightCheckInStatsEntry(String audioId) {
		return this.inFlightCheckInStats.get(audioId);
	}

	public long[] getCurrentInFlightCheckInStatsEntry() {
		return getInFlightCheckInStatsEntry(this.inFlightCheckInAudioId);
	}

	public String[] getInFlightCheckInEntry(String audioId) {
		return this.inFlightCheckInEntries.get(audioId);
	}

	public void setInFlightCheckInStats(String keyId, long msgSendStart, long msgSendDuration, long msgPayloadSize) {
		long[] stats = this.inFlightCheckInStats.get(keyId);
		if (stats == null) { stats = new long[] { 0, 0, 0 }; }
		if (msgSendStart != 0) { stats[0] = msgSendStart; }
		if (msgSendDuration != 0) { stats[1] = msgSendDuration; }
		if (msgPayloadSize != 0) { stats[2] = msgPayloadSize; }
		this.inFlightCheckInStats.remove(keyId);
		this.inFlightCheckInStats.put(keyId, stats);
	}

	private void resetRecentCheckInHealthMonitors() {
		healthCheckMonitors = new HashMap<String, long[]>();
		healthCheckTargetLowerBounds = new long[healthCheckCategories.length];
		healthCheckTargetUpperBounds = new long[healthCheckCategories.length];
		healthCheckInitValues = new long[healthCheckMeasurementCount];
		doCheckInConditionsAllowCheckInRequeuing = false;
	}

	private void setOrResetRecentCheckInHealthCheck(long prefsAudioCycleDuration, long prefsTimeOfDayLowerBound, long prefsTimeOfDayUpperBound) {

		if (	!healthCheckMonitors.containsKey(healthCheckCategories[0])
			|| 	(lastKnownAudioCycleDuration != prefsAudioCycleDuration)
			|| 	(lastKnownTimeOfDayLowerBound != prefsTimeOfDayLowerBound)
			|| 	(lastKnownTimeOfDayUpperBound != prefsTimeOfDayUpperBound)
		) {

			Log.v(logTag, "Resetting RecentCheckInHealthCheck metrics...");

			lastKnownAudioCycleDuration = prefsAudioCycleDuration;
			lastKnownTimeOfDayLowerBound = prefsTimeOfDayLowerBound;
			lastKnownTimeOfDayUpperBound = prefsTimeOfDayUpperBound;

			resetRecentCheckInHealthMonitors();

			// fill initial array with garbage (very high) values to ensure that checks will fail until we have the required number of checkins to compare
			Arrays.fill(healthCheckInitValues, Math.round(Long.MAX_VALUE / healthCheckMeasurementCount));

			// initialize categories with initial arrays (to be filled incrementally with real data)
			for (String healthCheckCategory : healthCheckCategories) {
				if (!healthCheckMonitors.containsKey(healthCheckCategory)) { healthCheckMonitors.put(healthCheckCategory, healthCheckInitValues); }
			}

			// set parameters (bounds) for health check pass or fail

			/* latency */		healthCheckTargetLowerBounds[0] = 0;
								healthCheckTargetUpperBounds[0] = Math.round( 0.4 * lastKnownAudioCycleDuration * 1000);

			/* queued */		healthCheckTargetLowerBounds[1] = 0;
								healthCheckTargetUpperBounds[1] = 1;

			/* recent */		healthCheckTargetLowerBounds[2] = 0;
								healthCheckTargetUpperBounds[2] = ( healthCheckMeasurementCount / 2 ) * (lastKnownAudioCycleDuration * 1000);

			/* time-of-day */	healthCheckTargetLowerBounds[3] = lastKnownTimeOfDayLowerBound;
								healthCheckTargetUpperBounds[3] = lastKnownTimeOfDayUpperBound;
		}
	}

	public boolean validateRecentCheckInHealthCheck(long prefsAudioCycleDuration, String prefsTimeOfDayBounds, long[] currentCheckInStats) {

		setOrResetRecentCheckInHealthCheck(	prefsAudioCycleDuration,
										(prefsTimeOfDayBounds.contains("-") ? Long.parseLong(prefsTimeOfDayBounds.split("-")[0]) : 11),
										(prefsTimeOfDayBounds.contains("-") ? Long.parseLong(prefsTimeOfDayBounds.split("-")[1]) : 13)
									);

		long[] currAvgVals = new long[healthCheckCategories.length]; Arrays.fill(currAvgVals, 0);

		for (int j = 0; j < healthCheckCategories.length; j++) {
			String categ = healthCheckCategories[j];
			long[] arraySnapshot = new long[healthCheckMeasurementCount];
			arraySnapshot[0] = currentCheckInStats[j];
			for (int i = (healthCheckMeasurementCount-1); i > 0; i--) { arraySnapshot[i] = healthCheckMonitors.get(categ)[i-1]; }
			healthCheckMonitors.remove(healthCheckCategories[j]);
			healthCheckMonitors.put(healthCheckCategories[j], arraySnapshot);
			currAvgVals[j] = ArrayUtils.getAverageAsLong(healthCheckMonitors.get(categ));
		}

		boolean displayLogging = true;
		doCheckInConditionsAllowCheckInRequeuing = true;
		StringBuilder healthCheckLogging = new StringBuilder();

		for (int j = 0; j < healthCheckCategories.length; j++) {

			long currAvgVal = currAvgVals[j];

			// some average values require modification before comparison to upper/lower bounds...
			if (healthCheckCategories[j].equalsIgnoreCase("recent")) {
				currAvgVal = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(currAvgVals[j]));
			}

			// compare to upper lower bounds, check for pass/fail
			if ((currAvgVal > healthCheckTargetUpperBounds[j]) || (currAvgVal < healthCheckTargetLowerBounds[j])) {
				doCheckInConditionsAllowCheckInRequeuing = false;
			}

			// generate some verbose logging feedback
			healthCheckLogging.append(", ").append(healthCheckCategories[j]).append(": ").append(currAvgVal).append("/")
					.append((healthCheckTargetLowerBounds[j] > 1) ? healthCheckTargetLowerBounds[j] + "-" : "")
					.append(healthCheckTargetUpperBounds[j]);

			if (healthCheckCategories[j].equalsIgnoreCase("time-of-day") && (currAvgVal > 24)) {
				// In this case, we suppress logging, as we can be sure that there are less than 6 checkin samples gathered
				displayLogging = false;
			}
		}

		healthCheckLogging.insert(0,"Stashed CheckIn Requeuing: "+( doCheckInConditionsAllowCheckInRequeuing ? "Allowed" : "Not Allowed" )+". Conditions (last "+healthCheckMeasurementCount+" checkins)");

		if (displayLogging) {
			if (!doCheckInConditionsAllowCheckInRequeuing) {
				Log.w(logTag, healthCheckLogging.toString());
			} else {
				Log.i(logTag, healthCheckLogging.toString());
			}
		}
		return doCheckInConditionsAllowCheckInRequeuing;
	}





	public JSONObject apiCheckInStatusAsJsonObj() {
		JSONObject statusObj = null;
		try {
			statusObj = new JSONObject();
			statusObj.put("is_allowed", isApiCheckInAllowed(false, false));
			statusObj.put("is_disabled", isApiCheckInDisabled(false));
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return statusObj;
	}


	public boolean isApiCheckInAllowed(boolean includeSentinel, boolean printFeedbackInLog) {

		// we set this to true, and cycle through conditions that might make it false
		// we then return the resulting true/false value
		boolean isApiCheckInAllowedUnderKnownConditions = true;
		StringBuilder msgNotAllowed = new StringBuilder();
		int reportedDelay = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 2;

		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_INTERNAL_BATTERY) && !isBatteryChargeSufficientForCheckIn()) {
			msgNotAllowed.append("low battery level")
					.append(" (current: ").append(this.app.deviceBattery.getBatteryChargePercentage(this.app.getApplicationContext(), null)).append("%,")
					.append(" required: ").append(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_CUTOFF_INTERNAL_BATTERY)).append("%).");
			isApiCheckInAllowedUnderKnownConditions = false;

		} else if (!app.deviceConnectivity.isConnected()) {
			msgNotAllowed.append("a lack of network connectivity.");
			isApiCheckInAllowedUnderKnownConditions = false;
			reportedDelay = Math.round(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) / 2);

		} else if (includeSentinel && limitBasedOnSentinelBatteryLevel()) {
			msgNotAllowed.append("Low Sentinel Battery level")
					.append(" (required: ").append(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_CUTOFF_SENTINEL_BATTERY)).append("%).");
			isApiCheckInAllowedUnderKnownConditions = false;

		}

		if (!isApiCheckInAllowedUnderKnownConditions) {
			if (printFeedbackInLog) {
				Log.d(logTag, msgNotAllowed
						.insert(0, DateTimeUtils.getDateTime() + " - ApiCheckIn not allowed due to ")
						.append(" Waiting ").append(reportedDelay).append(" seconds before next attempt.")
						.toString());
			}
		}

		return isApiCheckInAllowedUnderKnownConditions;
	}

	public boolean isApiCheckInDisabled(boolean printFeedbackInLog) {

		// we set this to false, and cycle through conditions that might make it true
		// we then return the resulting true/false value
		boolean areApiChecksInDisabledRightNow = false;
		StringBuilder msgIfDisabled = new StringBuilder();

		if (!this.app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CHECKIN_PUBLISH)) {
			msgIfDisabled.append("preference 'enable_checkin_publish' being explicitly set to false.");
			areApiChecksInDisabledRightNow = true;

		// This section is commented out because there is currently no mechanism by which the checkins are filtered by time of day (off hours)
		// ...But we assume this is something that might be added at a future date, as it works for audio capture.
//		} else if (limitBasedOnTimeOfDay()) {
//			msgIfDisabled.append("current time of day/night")
//					.append(" (off hours: '").append(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_SCHEDULE_OFF_HOURS)).append("'.");
//			areApiChecksInDisabledRightNow = true;

		} else if (!app.isGuardianRegistered()) {
			msgIfDisabled.append("the Guardian not having been registered.");
			areApiChecksInDisabledRightNow = true;

		}

		if (areApiChecksInDisabledRightNow) {
			if (printFeedbackInLog) {
				Log.d(logTag, msgIfDisabled
						.insert(0, DateTimeUtils.getDateTime() + " - ApiCheckIn disabled due to ")
						.toString());
			}
		}

		return areApiChecksInDisabledRightNow;
	}



	private boolean limitBasedOnSentinelBatteryLevel() {

		if (this.app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SENTINEL_BATTERY)) {
			try {
				JSONArray jsonArray = RfcxComm.getQuery("admin", "status", "*", app.getResolver());
				if (jsonArray.length() > 0) {
					JSONObject jsonObj = jsonArray.getJSONObject(0);
					if (jsonObj.has("api_checkin")) {
						JSONObject apiCheckInObj = jsonObj.getJSONObject("api_checkin");
						if (apiCheckInObj.has("is_allowed")) {
							if (!apiCheckInObj.getBoolean(("is_allowed"))) {
								return true;
							}
						}
					}
				}
			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);
				return false;
			}
		}
		return false;
	}


	public boolean isBatteryChargeSufficientForCheckIn() {
		int batteryChargeCutoff = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_CUTOFF_INTERNAL_BATTERY);
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		boolean isBatteryChargeSufficient = (batteryCharge >= batteryChargeCutoff);
		if (isBatteryChargeSufficient && (batteryChargeCutoff == 100)) {
			isBatteryChargeSufficient = this.app.deviceBattery.isBatteryCharged(app.getApplicationContext(), null);
			if (!isBatteryChargeSufficient) { Log.d(logTag, "Battery is at 100% but is not yet fully charged."); }
		}
		return isBatteryChargeSufficient;
	}

}
