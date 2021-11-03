package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.swm.data.SwmRTBackgroundResponse;
import org.rfcx.guardian.admin.comms.swm.data.SwmRTResponse;
import org.rfcx.guardian.admin.comms.swm.data.SwmUnsentMsg;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.List;

public class SwmDispatchCycleService extends Service {

    public static final String SERVICE_NAME = "SwmDispatchCycle";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDispatchCycleService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private SwmDispatchCycleSvc swmDispatchCycleSvc;

    private final long swmDispatchCycleDuration = 30000;

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
            if (!app.swmUtils.isSatelliteAllowedAtThisTimeOfDay()) {
                Log.d(logTag, "Swarm is OFF at this time");
                app.swmUtils.getPower().setOn(false);
                return;
            }

            // Make sure it is on and dispatching
            Log.d(logTag, "Swarm is ON");
            app.swmUtils.getPower().setOn(true);

            // Check if Swarm should be OFF due to inactivity
            List<SwmUnsentMsg> queuedOnSwarm = app.swmUtils.getApi().getUnsentMessages();
            int queuedInDatabase = app.swmMessageDb.dbSwmQueued.getCount();
            if (queuedOnSwarm != null && queuedOnSwarm.size() == 0 && queuedInDatabase == 0) {
                Log.d(logTag, "Swarm is OFF due to inactivity");
                app.swmUtils.getPower().setOn(false);
                return;
            }

            getDiagnostics();
            updateQueueBetweenGuardianAndSwarm();
            sendQueuedMessages();
        }

        private void sendQueuedMessages() throws InterruptedException {
            List<String[]> swmQueuedForDispatch = app.swmMessageDb.dbSwmQueued.getUnsentMessagesToSwarmInOrderOfTimestamp();

            for (String[] swmForDispatch : swmQueuedForDispatch) {

                // only proceed with dispatch process if there is a valid queued swm message in the database
                if (swmForDispatch[0] != null) {

                    long sendAtOrAfter = Long.parseLong(swmForDispatch[1]);
                    long rightNow = System.currentTimeMillis();

                    if (sendAtOrAfter <= rightNow) {

                        String msgId = swmForDispatch[4];
                        String msgBody = swmForDispatch[3];

                        // getting unsent message count from Swarm
                        int unsentMessageNumbers = app.swmUtils.getApi().getNumberOfUnsentMessages();
                        if (unsentMessageNumbers > 30) {
                            return;
                        }
                        // send message
                        String swmMessageId = app.swmUtils.getApi().transmitData("\"" + msgBody + "\"");
                        if (swmMessageId != null) {
                            app.swmMessageDb.dbSwmQueued.updateSwmMessageIdByMessageId(msgId, swmMessageId);

                            String concatSegId = msgBody.substring(0, 4) + "-" + msgBody.substring(4, 7);
                            Log.v(logTag, DateTimeUtils.getDateTime(rightNow) + " - Segment '" + concatSegId + "' sent by SWM (" + msgBody.length() + " chars)");
                            RfcxComm.updateQuery("guardian", "database_set_last_accessed_at", "segments|" + concatSegId, app.getResolver());
                        }

                        Thread.sleep(333);
                    }
                }
            }
        }

        private void updateQueueBetweenGuardianAndSwarm() {
            app.swmUtils.updateQueueMessagesFromSwarm(app.swmUtils.getApi().getUnsentMessages());
        }

        private void getDiagnostics() {
            SwmRTBackgroundResponse rtBackground = app.swmUtils.getApi().getRTBackground();
            SwmRTResponse rtSatellite = app.swmUtils.getApi().getRTSatellite();
            Integer unsentMessageNumbers = app.swmUtils.getApi().getNumberOfUnsentMessages();

            Integer rssiBackground = null;
            if (rtBackground != null) {
                rssiBackground = rtBackground.getRssi();
            }

            Integer rssiSat = null;
            Integer snr = null;
            Integer fdev = null;
            String time = null;
            String satId = null;
            if (rtSatellite != null) {
                if (rtSatellite.getRssi() != 0) rssiSat = rtSatellite.getRssi();
                if (rtSatellite.getSignalToNoiseRatio() != 0)
                    snr = rtSatellite.getSignalToNoiseRatio();
                if (rtSatellite.getFrequencyDeviation() != 0)
                    fdev = rtSatellite.getFrequencyDeviation();
                if (!rtSatellite.getPacketTimestamp().equals("1970-01-01 00:00:00"))
                    time = rtSatellite.getPacketTimestamp();
                if (!rtSatellite.getSatelliteId().equals("0x000000"))
                    satId = rtSatellite.getSatelliteId();
            }

            if (rtBackground != null || rtSatellite != null) {
                app.swmMetaDb.dbSwmDiagnostic.insert(rssiBackground, rssiSat, snr, fdev, time, satId, unsentMessageNumbers);
            }
        }
    }


}
