package org.rfcx.guardian.guardian.api.checkin;

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

	private void resetRecentCheckInHealthMonitors() {
		healthCheckMonitors = new HashMap<String, long[]>();
		healthCheckTargetLowerBounds = new long[healthCheckCategories.length];
		healthCheckTargetUpperBounds = new long[healthCheckCategories.length];
		healthCheckInitValues = new long[healthCheckMeasurementCount];
		doCheckInConditionsAllowCheckInRequeuing = false;
	}

	private void setOrResetRecentCheckInHealthCheck(long prefsAudioCycleDuration, long[] timeOfDayBounds) {

		if (	!healthCheckMonitors.containsKey(healthCheckCategories[0])
			|| 	(lastKnownAudioCaptureDuration != prefsAudioCycleDuration)
			) {

			Log.v(logTag, "Resetting RecentCheckInHealthCheck metrics...");

			lastKnownAudioCaptureDuration = prefsAudioCycleDuration;
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

			/* time-of-day */	healthCheckTargetLowerBounds[3] = timeOfDayBounds[0];
								healthCheckTargetUpperBounds[3] = timeOfDayBounds[1];
		}
	}

	public boolean validateRecentCheckInHealthCheck(long prefsAudioCycleDuration, long[] timeOfDayBounds, long[] inputValues) {

		setOrResetRecentCheckInHealthCheck(prefsAudioCycleDuration, timeOfDayBounds);

		long[] currAvgVals = new long[healthCheckCategories.length]; Arrays.fill(currAvgVals, 0);

		for (int j = 0; j < healthCheckCategories.length; j++) {
			String categ = healthCheckCategories[j];
			long[] arraySnapshot = new long[healthCheckMeasurementCount];
			arraySnapshot[0] = inputValues[j];
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
			if (healthCheckCategories[j].equalsIgnoreCase("recent")) { currAvgVal = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(currAvgVals[j])); }

			// compare to upper lower bounds, check for pass/fail
			if ((currAvgVal > healthCheckTargetUpperBounds[j]) || (currAvgVal < healthCheckTargetLowerBounds[j])) {
				doCheckInConditionsAllowCheckInRequeuing = false;
			}

			// generate some verbose logging feedback
			healthCheckLogging.append(", ").append(healthCheckCategories[j]).append(": ").append(currAvgVal).append("/")
					.append((healthCheckTargetLowerBounds[j] > 1) ? healthCheckTargetLowerBounds[j] + "-" : "")
					.append(healthCheckTargetUpperBounds[j]);

			if (healthCheckCategories[j].equalsIgnoreCase("time-of-day") && (currAvgVal > 24)) {
				// In this case, we suppress logging, as we can guess that there are less than 6 checkin samples gathered
				displayLogging = false;
			}
		}

		healthCheckLogging.insert(0,"Stashed CheckIn Requeuing: "+( doCheckInConditionsAllowCheckInRequeuing ? "Allowed" : "Not Allowed" )+". Conditions (last "+healthCheckMeasurementCount+" checkins)");

		if (displayLogging) {
			if (!doCheckInConditionsAllowCheckInRequeuing) {
				Log.w(logTag, healthCheckLogging.toString());
			} else {
				Log.i(logTag, healthCheckLogging.toString());
				// this is where we could choose to reload stashed checkins into the queue
				return true;
			}
		}
		return false;
	}






}
