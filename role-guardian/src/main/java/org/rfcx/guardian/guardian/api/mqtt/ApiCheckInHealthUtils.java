package org.rfcx.guardian.guardian.api.mqtt;

import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ApiCheckInHealthUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInHealthUtils");

	private Map<String, long[]> healthCheckMonitors = new HashMap<String, long[]>();
	private static final String[] healthCheckCategories = new String[] { "latency", "queued", "recent", "time-of-day" };
	private long[] healthCheckTargetLowerBounds = new long[healthCheckCategories.length];
	private long[] healthCheckTargetUpperBounds = new long[healthCheckCategories.length];
	private static final int healthCheckMeasurementCount = 6;
	private long[] healthCheckInitValues = new long[healthCheckMeasurementCount];
	private boolean doCheckInConditionsAllowCheckInRequeuing = false;

	private long lastKnownAudioCaptureDuration = 0;
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
			|| 	(lastKnownAudioCaptureDuration != prefsAudioCycleDuration)
			|| 	(lastKnownTimeOfDayLowerBound != prefsTimeOfDayLowerBound)
			|| 	(lastKnownTimeOfDayUpperBound != prefsTimeOfDayUpperBound)
		) {

			Log.v(logTag, "Resetting RecentCheckInHealthCheck metrics...");

			lastKnownAudioCaptureDuration = prefsAudioCycleDuration;
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
								healthCheckTargetUpperBounds[0] = Math.round( 0.4 * lastKnownAudioCaptureDuration * 1000);

			/* queued */		healthCheckTargetLowerBounds[1] = 0;
								healthCheckTargetUpperBounds[1] = 1;

			/* recent */		healthCheckTargetLowerBounds[2] = 0;
								healthCheckTargetUpperBounds[2] = ( healthCheckMeasurementCount / 2 ) * (lastKnownAudioCaptureDuration * 1000);

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






}
