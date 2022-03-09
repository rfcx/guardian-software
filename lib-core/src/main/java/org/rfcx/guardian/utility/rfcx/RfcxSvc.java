package org.rfcx.guardian.utility.rfcx;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.utility.misc.DateTimeUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class RfcxSvc {

    Context context;
    private final String logTag;
    private final Map<String, Class<?>> svcClasses = new HashMap<String, Class<?>>();
    private final Map<String, boolean[]> svcRunStates = new HashMap<String, boolean[]>();
    private final Map<String, boolean[]> svcAbsoluteRunStates = new HashMap<String, boolean[]>();
    private final Map<String, long[]> svcLastReportedActiveAt = new HashMap<String, long[]>();

    public RfcxSvc(Context context, String appRole) {
        this.context = context;
        this.logTag = RfcxLog.generateLogTag(appRole, "RfcxSvc");
    }

    public static String intentServiceTags(boolean isNotificationTag, String appRole, String svcName) {
        return "org.rfcx.guardian.guardian." +
                appRole.toLowerCase(Locale.US) +
                (isNotificationTag ? ".RECEIVE_" : ".") +
                svcName.toUpperCase(Locale.US) +
                (isNotificationTag ? "_NOTIFICATIONS" : "");
    }

    public void triggerService(String[] svcToTrigger, boolean forceReTrigger) {
        triggerService(svcToTrigger, forceReTrigger, true);
    }

    public void triggerService(String[] svcToTrigger, boolean forceReTrigger, boolean verboseLogging) {

        String svcName = svcToTrigger[0];
        String svcId = svcName.toLowerCase(Locale.US);

        if (!this.svcClasses.containsKey(svcId)) {

            Log.e(logTag, "There is no service named '" + svcName + "'.");

        } else if (!this.isRunning(svcName) || forceReTrigger) {
            try {
                // this means it's likely an intent service (rather than a service)
                if (svcToTrigger.length > 1) {

                    String svcStart = svcToTrigger[1];
                    String svcRepeat = svcToTrigger[2];

                    long startTimeMillis = System.currentTimeMillis();
                    boolean isSvcScheduled = false;
                    if (!svcStart.equals("0")
                            && !svcStart.equalsIgnoreCase("now")
                    ) {
                        try {
                            startTimeMillis = Long.parseLong(svcStart);
                            isSvcScheduled = true;
                        } catch (Exception e) {
                            RfcxLog.logExc(logTag, e);
                        }
                    }

                    long repeatIntervalMillis = 0;
                    if (!svcRepeat.equals("0")
                            && !svcRepeat.equalsIgnoreCase("norepeat")
                    ) {
                        try {
                            repeatIntervalMillis = Long.parseLong(svcRepeat);
                        } catch (Exception e) {
                            RfcxLog.logExc(logTag, e);
                        }
                    }

                    if (repeatIntervalMillis == 0) {
                        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, startTimeMillis, PendingIntent.getService(this.context, -1, new Intent(context, svcClasses.get(svcId)), PendingIntent.FLAG_UPDATE_CURRENT));
                        Log.i(logTag, ((isSvcScheduled) ? "Scheduled" : "Triggered")
                                + " IntentService '" + svcName + "'"
                                + ((isSvcScheduled) ? " (begins at " + DateTimeUtils.getDateTime(startTimeMillis) + ")" : "")
                        );
                    } else {
                        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.RTC, startTimeMillis, repeatIntervalMillis, PendingIntent.getService(this.context, -1, new Intent(context, svcClasses.get(svcId)), PendingIntent.FLAG_UPDATE_CURRENT));
                        // could also use setRepeating() here instead, but this was appearing to lead to dropped events the first time around
                        Log.i(logTag, "Scheduled Repeating IntentService '" + svcName + "' (begins at " + DateTimeUtils.getDateTime(startTimeMillis) + ", repeats approx. every " + DateTimeUtils.milliSecondDurationAsReadableString(repeatIntervalMillis) + ")");
                    }

                    // this means it's likely a service (rather than an intent service)
                } else if (svcToTrigger.length == 1) {
                    if (forceReTrigger) {
                        Log.d(logTag, svcName + " activity: " + isRunning(svcName) + " and last reported as active " + DateTimeUtils.timeStampDifferenceFromNowAsReadableString((new Date(getLastReportedActiveAt(svcName)))) + " ago.");
                    }
                    this.context.stopService(new Intent(this.context, svcClasses.get(svcId)));
                    this.context.startService(new Intent(this.context, svcClasses.get(svcId)));
                    Log.i(logTag, "Triggered Service '" + svcName + "'");

                }
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        }
    }

    public void triggerService(String svcToTrigger, boolean forceReTrigger) {
        triggerService(new String[]{svcToTrigger}, forceReTrigger);
    }

    public void triggerIntentService(String svcToTrigger, long startTimeMillis, long repeatIntervalMillis) {
        triggerService(new String[]{svcToTrigger, "" + startTimeMillis, "" + repeatIntervalMillis}, false);
    }

    public void triggerIntentServiceImmediately(String svcToTrigger) {
        triggerService(new String[]{svcToTrigger, "0", "0"}, false);
    }

    public void stopService(String svcToStop) {
        stopService(svcToStop, true);
    }

    public void stopService(String svcToStop, boolean verboseLogging) {

        String svcId = svcToStop.toLowerCase(Locale.US);

        if (!this.svcClasses.containsKey(svcId)) {
            Log.e(logTag, "There is no service named '" + svcToStop + "'.");
        } else {
            try {
                this.context.stopService(new Intent(this.context, svcClasses.get(svcId)));
                if (verboseLogging) {
                    Log.i(logTag, "Stopped Service '" + svcToStop + "'");
                }
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        }
    }

    public void stopAllServices() {
        for (Entry svcEntry : this.svcClasses.entrySet()) {
            stopService(svcEntry.getKey().toString());
        }
    }

    public void triggerServiceSequence(String sequenceName, String[] serviceSequenceSerialized, boolean forceReTrigger, long timeOutDuration) {

        if (!hasRun(sequenceName.toLowerCase(Locale.US))) {

            Log.i(logTag, "Launching ServiceSequence '" + sequenceName + "'.");

            for (String serviceItemSerialized : serviceSequenceSerialized) {
                String[] serviceItem = new String[]{serviceItemSerialized};
                if (serviceItemSerialized.contains("|")) {
                    serviceItem = serviceItemSerialized.split("\\|");
                }
                if (timeOutDuration > 0) {
                    Log.d(logTag, "'" + serviceItem[0] + "' service last registered as active "
                            + DateTimeUtils.timeStampDifferenceFromNowAsReadableString(getLastReportedActiveAt(serviceItem[0]))
                            + " ago."
                    );
                    triggerOrForceReTriggerIfTimedOut(serviceItem[0], timeOutDuration);
                } else {
                    triggerService(serviceItem, forceReTrigger);
                }
            }
            setAbsoluteRunState(sequenceName, true);
        } else {
            Log.w(logTag, "ServiceSequence '" + sequenceName + "' has already run.");
        }
    }


    // Getters and Setters

    public boolean isRunning(String svcName) {

        String svcId = svcName.toLowerCase(Locale.US);

        if (this.svcRunStates.containsKey(svcId)) {
            try {
                return this.svcRunStates.get(svcId)[0];
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        }
        return false;
    }

    public boolean hasRun(String svcName) {

        String svcId = svcName.toLowerCase(Locale.US);

        if (this.svcAbsoluteRunStates.containsKey(svcId)) {
            try {
                return this.svcAbsoluteRunStates.get(svcId)[0];
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        }
        return false;
    }

    public void setRunState(String svcName, boolean isRunning) {

        String svcId = svcName.toLowerCase(Locale.US);
        this.svcRunStates.put(svcId, new boolean[]{isRunning});
        if (isRunning) setAbsoluteRunState(svcName, true);
        reportAsActive(svcId);
    }

    public void setAbsoluteRunState(String svcName, boolean hasRun) {

        String svcId = svcName.toLowerCase(Locale.US);
        this.svcAbsoluteRunStates.put(svcId, new boolean[]{hasRun});
    }

    public void reportAsActive(String svcName) {

        String svcId = svcName.toLowerCase(Locale.US);
        this.svcLastReportedActiveAt.put(svcId, new long[]{System.currentTimeMillis()});
    }

    public long getLastReportedActiveAt(String svcName) {

        String svcId = svcName.toLowerCase(Locale.US);
        if (this.svcLastReportedActiveAt.containsKey(svcId)) {
            return this.svcLastReportedActiveAt.get(svcId)[0];
        } else {
            return 0;
        }
    }

    public void addService(String svcName, Class<?> svcClass) {

        String svcId = svcName.toLowerCase(Locale.US);
        this.svcClasses.put(svcId, svcClass);
        setRunState(svcName, false);
        setAbsoluteRunState(svcName, false);
    }

    public void triggerOrForceReTriggerIfTimedOut(String svcName, long timeOutDuration) {

        long lastActiveAt = getLastReportedActiveAt(svcName);
        if ((lastActiveAt > 0) && ((System.currentTimeMillis() - lastActiveAt) > timeOutDuration)) {
            Log.e(logTag, "Service '" + svcName + "' has not registered as active within the last " + DateTimeUtils.milliSecondDurationAsReadableString(timeOutDuration) + "... Forcing re-trigger...");
            triggerService(svcName, true);
        } else {
            triggerService(svcName, false);
        }
    }

}
