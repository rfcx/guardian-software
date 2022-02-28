package org.rfcx.guardian.admin.asset;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class ScheduledAssetCleanupService extends IntentService {

    public static final String SERVICE_NAME = "ScheduledAssetCleanup";
    public static final int ASSET_CLEANUP_CYCLE_DURATION_MINUTES = 120;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledAssetCleanupService");

    public ScheduledAssetCleanupService() {
        super(logTag);
    }

    @Override
    protected void onHandleIntent(Intent inputIntent) {
        Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        ;

        RfcxGuardian app = (RfcxGuardian) getApplication();

        app.rfcxSvc.reportAsActive(SERVICE_NAME);

        try {
            app.assetUtils.runFileSystemAssetCleanup();
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }

    }


}
