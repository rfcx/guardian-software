package org.rfcx.guardian.guardian;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

import android.app.IntentService;
import android.content.Intent;

public class ServiceMonitor extends IntentService {

    public static final String SERVICE_NAME = "ServiceMonitor";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ServiceMonitor");

    public static final long SERVICE_MONITOR_CYCLE_DURATION = 600000;
    // Please note that services that register as 'active' less frequently than this cycle duration will be forced to retrigger.
    // For continuous, long running services, measures should be taken to ensure that they register as 'active' more often than this monitor runs.

    public ServiceMonitor() {
        super(logTag);
    }

    @Override
    protected void onHandleIntent(Intent inputIntent) {
        Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        ;

        RfcxGuardian app = (RfcxGuardian) getApplication();

        if (app.rfcxSvc.isRunning(SERVICE_NAME)) {

            app.rfcxSvc.triggerServiceSequence("ServiceMonitorSequence", app.RfcxCoreServices, false, SERVICE_MONITOR_CYCLE_DURATION);
        }

        app.rfcxSvc.setRunState(SERVICE_NAME, true);
    }


}
