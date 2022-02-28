package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class SystemCPUGovernorService extends IntentService {

    public static final String SERVICE_NAME = "SystemCPUGovernor";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SystemCPUGovernorService");

    public SystemCPUGovernorService() {
        super(logTag);
    }

    @Override
    protected void onHandleIntent(Intent inputIntent) {
        Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        ;

        RfcxGuardian app = (RfcxGuardian) getApplication();

        app.deviceCPUGovernor.checkSetActiveVals();

    }


}
