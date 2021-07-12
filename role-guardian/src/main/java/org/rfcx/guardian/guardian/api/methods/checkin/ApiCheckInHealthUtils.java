package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ApiCheckInHealthUtils {

	public ApiCheckInHealthUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInHealthUtils");

	private RfcxGuardian app;

	private Map<String, long[]> healthCheckMonitors = new HashMap<>();
	private static final String[] healthCheckCategories = new String[] { "latency", "queued", "recent" };
	private long[] healthCheckTargetLowerBounds = new long[healthCheckCategories.length];
	private long[] healthCheckTargetUpperBounds = new long[healthCheckCategories.length];
	private static final int healthCheckMeasurementCount = 6;
	private long[] healthCheckInitValues = new long[healthCheckMeasurementCount];
	private boolean doCheckInConditionsAllowCheckInRequeuing = false;

	private long lastKnownAudioCycleDuration = 0;

	private String inFlightCheckInAudioId = null;
	private String latestCheckInAudioId = null;
	private Map<String, String[]> inFlightCheckInEntries = new HashMap<>();
	private Map<String, long[]> inFlightCheckInStats = new HashMap<>();

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

	private void setOrResetRecentCheckInHealthCheck(long prefsAudioCycleDuration) {

		if (	!healthCheckMonitors.containsKey(healthCheckCategories[0])
			|| 	(lastKnownAudioCycleDuration != prefsAudioCycleDuration)
		) {

			Log.v(logTag, "Resetting RecentCheckInHealthCheck metrics...");

			lastKnownAudioCycleDuration = prefsAudioCycleDuration;

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
		}
	}

	public boolean validateRecentCheckInHealthCheck(long prefsAudioCycleDuration, long[] currentCheckInStats) {

		setOrResetRecentCheckInHealthCheck(prefsAudioCycleDuration);

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

			if (healthCheckCategories[j].equalsIgnoreCase("queued") && (currAvgVal > 10000)) {
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

		} else if (includeSentinel && !app.rfcxStatus.getFetchedStatus( RfcxStatus.Group.API_CHECKIN, RfcxStatus.Type.ALLOWED)) {
			msgNotAllowed.append("Low Sentinel Battery level")
					.append(" (required: ").append(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_CUTOFF_SENTINEL_BATTERY)).append("%).");
			isApiCheckInAllowedUnderKnownConditions = false;

		}

		if (!isApiCheckInAllowedUnderKnownConditions && printFeedbackInLog) {
			Log.d(logTag, msgNotAllowed
					.insert(0, DateTimeUtils.getDateTime() + " - ApiCheckIn not allowed due to ")
					.append(" Waiting ").append(reportedDelay).append(" seconds before next attempt.")
					.toString());
		}

		return isApiCheckInAllowedUnderKnownConditions;
	}

	public boolean isApiCheckInDisabled(boolean printFeedbackInLog) {

		// we set this to false, and cycle through conditions that might make it true
		// we then return the resulting true/false value
		boolean areApiChecksInDisabledRightNow = false;

		StringBuilder msgIfDisabled = new StringBuilder();

		if (!this.app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CHECKIN_PUBLISH)) {
			msgIfDisabled.append("preference '" + RfcxPrefs.Pref.ENABLE_CHECKIN_PUBLISH.toLowerCase() + "' being explicitly set to false.");
			areApiChecksInDisabledRightNow = true;

		} else if (!isCheckInPublishAllowedAtThisTimeOfDay()) {
			msgIfDisabled.append("current time of day/night")
					.append(" (off hours: '").append(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_CHECKIN_PUBLISH_SCHEDULE_OFF_HOURS)).append("'.");
			areApiChecksInDisabledRightNow = true;

		} else if (!app.isGuardianRegistered()) {
			msgIfDisabled.append("the Guardian not having been registered.");
			areApiChecksInDisabledRightNow = true;

		}

		if (areApiChecksInDisabledRightNow && printFeedbackInLog) {
			Log.d(logTag, msgIfDisabled
					.insert(0, DateTimeUtils.getDateTime() + " - ApiCheckIn disabled due to ")
					.toString());
		}

		return areApiChecksInDisabledRightNow;
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


	private boolean isCheckInPublishAllowedAtThisTimeOfDay() {
		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SCHEDULE_OFF_HOURS)) {
			String prefsOffHours = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_CHECKIN_PUBLISH_SCHEDULE_OFF_HOURS);
			for (String offHoursRange : TextUtils.split(prefsOffHours, ",")) {
				String[] offHours = TextUtils.split(offHoursRange, "-");
				if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isCheckInReQueueAllowedAtThisTimeOfDay() {
		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SCHEDULE_OFF_HOURS)) {
			String prefsOffHours = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_CHECKIN_REQUEUE_SCHEDULE_OFF_HOURS);
			for (String offHoursRange : TextUtils.split(prefsOffHours, ",")) {
				String[] offHours = TextUtils.split(offHoursRange, "-");
				if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
					Log.w(logTag, "Stashed CheckIn Requeuing blocked due to current time of day/night (off hours: '" + prefsOffHours + "')");
					return false;
				}
			}
		}
		return true;
	}
}
