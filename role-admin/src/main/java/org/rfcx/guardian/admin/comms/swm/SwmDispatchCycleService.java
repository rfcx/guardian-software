package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.swm.data.SwmDTResponse;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.TimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.Date;
import java.util.List;

public class SwmDispatchCycleService extends Service {

    public static final String SERVICE_NAME = "SwmDispatchCycle";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDispatchCycleService");
    private final long swmDispatchCycleDuration = 120000;
    private RfcxGuardian app;
    private boolean runFlag = false;
    private SwmDispatchCycleSvc swmDispatchCycleSvc;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.swmDispatchCycleSvc = new SwmDispatchCycleSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.swmDispatchCycleSvc.start();
        } catch (IllegalThreadStateException e) {
            RfcxLog.logExc(logTag, e);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.runFlag = false;
        app.rfcxSvc.setRunState(SERVICE_NAME, false);
        this.swmDispatchCycleSvc.interrupt();
        this.swmDispatchCycleSvc = null;
    }


    private class SwmDispatchCycleSvc extends Thread {

        public SwmDispatchCycleSvc() {
            super("SwmDispatchCycleService-SwmDispatchCycleSvc");
        }

        @Override
        public void run() {
            SwmDispatchCycleService swmDispatchCycleInstance = SwmDispatchCycleService.this;
            app = (RfcxGuardian) getApplication();

            if (!app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("swm")) {
                Log.i(logTag, "Swarm is disabled");
                return;
            }

            app.rfcxSvc.reportAsActive(SERVICE_NAME);
            Log.i(logTag, "Setting up Swarm");
            app.swmUtils.setupSwmUtils();

            while (swmDispatchCycleInstance.runFlag) {
                app.rfcxSvc.reportAsActive(SERVICE_NAME);
                try {
                    Log.d(logTag, "Trigger");
                    trigger();
                    Log.d(logTag, "Sleep");
                    Thread.sleep(swmDispatchCycleDuration);
                } catch (Exception e) {
                    RfcxLog.logExc(logTag, e);
                    break;
                }
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            swmDispatchCycleInstance.runFlag = false;
            Log.v(logTag, "Stopping service: " + logTag);
        }

        private void trigger() throws InterruptedException {
            // Check if Swarm should be OFF due to prefs
            if (!DateTimeUtils.isCurrentTimeBefore2022()) {
                // If socket connected then no need to off swarm tile
                if (!TimeUtils.INSTANCE.isNowOutsideTimeRange(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_OFF_HOURS)) && !app.companionSocketUtils.socketUtils.isConnectingWithCompanion) {
                    Log.d(logTag, "Swarm is OFF at this time");
                    app.swmUtils.getPower().setOn(false);
                    return;
                }
            }

            // Make sure it is on and dispatching
            Log.d(logTag, "Swarm is ON");
            app.swmUtils.getPower().setOn(true);

            setDateTime();
            getDiagnostics();
            sendQueuedMessages();
        }

        private void sendQueuedMessages() throws InterruptedException {

            int unsentMessageNumbers = app.swmUtils.getApi().getNumberOfUnsentMessages();
            if (unsentMessageNumbers > 0) return;

            String[] latestMessageForQueue = app.swmMessageDb.dbSwmQueued.getLatestRow();
            if (latestMessageForQueue[4] == null) return;

            long day = (24 * 60 * 60 * 1000);
            Date date = new Date(Long.parseLong(latestMessageForQueue[0]) - day);
            List<String> groupIds = app.swmMessageDb.dbSwmQueued.getGroupIdsBefore(date);
            app.swmMessageDb.dbSwmQueued.clearRowsByGroupIds(groupIds);

            for (String[] swmForDispatch : app.swmMessageDb.dbSwmQueued.getUnsentMessagesInOrderOfTimestampAndWithinGroupId(latestMessageForQueue[4])) {
                // only proceed with dispatch process if there is a valid queued swm message in the database
                if (swmForDispatch[0] != null) {

                    long sendAtOrAfter = Long.parseLong(swmForDispatch[1]);
                    long rightNow = System.currentTimeMillis();

                    if (sendAtOrAfter <= rightNow) {

                        String msgId = swmForDispatch[5];
                        String msgBody = swmForDispatch[3];
                        int priority = Integer.parseInt(swmForDispatch[7]);

                        // send message
                        String swmMessageId = app.swmUtils.getApi().transmitData("\"" + msgBody + "\"", priority);
                        if (swmMessageId != null) {
                            app.swmMessageDb.dbSwmSent.insert(
                                    Long.parseLong(swmForDispatch[1]),
                                    swmForDispatch[2],
                                    msgBody,
                                    swmForDispatch[4],
                                    msgId,
                                    swmMessageId,
                                    priority
                            );
                            app.swmMessageDb.dbSwmQueued.deleteSingleRowByMessageId(msgId);

                            String concatSegId = msgBody.substring(0, 4) + "-" + msgBody.substring(4, 7);
                            Log.v(logTag, DateTimeUtils.getDateTime(rightNow) + " - Segment '" + concatSegId + "' sent by SWM (" + msgBody.length() + " chars)");
                            RfcxComm.updateQuery("guardian", "database_set_last_accessed_at", "segments|" + concatSegId, app.getResolver());
                        } else {
                            return;
                        }

                        Thread.sleep(333);
                    }
                }
            }
        }

        private void getDiagnostics() {
            app.swmUtils.saveDiagnostic();
        }

        private void setDateTime() {
            SwmDTResponse dateTime = app.swmUtils.api.getDateTime();
            if (dateTime == null) return;
            SystemClock.setCurrentTimeMillis(dateTime.getEpochMs());
        }
    }


}
