package org.rfcx.guardian.guardian.api.methods.ping;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.Date;
import java.util.Locale;

public class ApiPingUtils {

    public static final long delayInitialRepeatingPingCycleByThisManyMs = 2 * 60 * 1000; // two minutes
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiPingUtils");
    public long repeatingPingCycleDuration;
    public long repeatingPingLastAttemptedAt = 0;
    public long repeatingPingLastQueuedAt = 0;
    public long repeatingPingLastCompletedOrSkippedAt = 0;
    private RfcxGuardian app;
    public ApiPingUtils(Context context) {

        this.app = (RfcxGuardian) context.getApplicationContext();
        updateRepeatingPingCycleDuration();
    }

    public void updateRepeatingPingCycleDuration() {
        this.repeatingPingCycleDuration = (app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.API_PING_CYCLE_DURATION) * 60 * 1000);
    }

    public boolean sendPing(boolean includeAllExtraFields, String[] includeExtraFields, int includeAssetBundleCount, String forceProtocol, boolean allowSegmentProtocols) {

        String[] apiProtocols = app.rfcxPrefs.getDefaultPrefValueAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(",");
        if (forceProtocol.equalsIgnoreCase("all")) {
            apiProtocols = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(",");
            Log.v(logTag, "Allowed Ping protocols (in order): " + TextUtils.join(", ", apiProtocols).toUpperCase(Locale.US));
        } else if (ArrayUtils.doesStringArrayContainString(apiProtocols, forceProtocol)) {
            apiProtocols = new String[]{forceProtocol};
        }

        boolean isPublished = false;

        try {

            String pingJson = app.apiPingJsonUtils.buildPingJson(includeAllExtraFields, includeExtraFields, includeAssetBundleCount, true, new String[]{}, true);

            for (String apiProtocol : apiProtocols) {

                Log.v(logTag, "Attempting Ping publication via " + apiProtocol.toUpperCase(Locale.US) + " protocol...");

                if ((apiProtocol.equalsIgnoreCase("mqtt")
                        && app.apiMqttUtils.sendMqttPing(pingJson)
                )
                        || (apiProtocol.equalsIgnoreCase("rest")
                        && app.apiRestUtils.sendRestPing(pingJson)
                )
                        || (allowSegmentProtocols
                        && ((apiProtocol.equalsIgnoreCase("sms")
                        && app.apiSmsUtils.sendSmsPing(pingJson, getPriority(pingJson))
                )
                        || ((apiProtocol.equalsIgnoreCase("sat") || apiProtocol.equalsIgnoreCase("sbd") || apiProtocol.equalsIgnoreCase("swm"))
                        && app.apiSatUtils.sendSatPing(pingJson, getPriority(pingJson))
                )
                )
                )
                ) {
                    isPublished = true;
                    String actionVerb = (apiProtocol.equalsIgnoreCase("mqtt") || apiProtocol.equalsIgnoreCase("rest")) ? "published" : "queued to be sent";
                    Log.v(logTag, "Ping has been " + actionVerb + " via " + apiProtocol.toUpperCase(Locale.US) + ".");
                    break;
                }
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }

        if (!isPublished) {
            Log.e(logTag, "Ping failed to publish via protocol(s): " + TextUtils.join(", ", apiProtocols).toUpperCase(Locale.US));
        }

        return isPublished;
    }

    public boolean sendPing(boolean includeAllExtraFields, String[] includeExtraFields, boolean allowSegmentProtocols) {
        return sendPing(includeAllExtraFields, includeExtraFields, 0, "all", allowSegmentProtocols);
    }

    public boolean isScheduledPingAllowedAtThisTimeOfDay() {
        for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PING_SCHEDULE_OFF_HOURS), ",")) {
            String[] offHours = TextUtils.split(offHoursRange, "-");
            if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
                return false;
            }
        }
        return true;
    }

    private int getPriority(String pingJson) {
        if (pingJson.contains("detections")) return 1;
        return 2;
    }

}
