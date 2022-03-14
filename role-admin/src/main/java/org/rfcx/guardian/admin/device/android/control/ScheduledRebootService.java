package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

import java.util.Calendar;

public class ScheduledRebootService extends IntentService {

    public static final String SERVICE_NAME = "ScheduledReboot";
    public static final long SCHEDULED_REBOOT_CYCLE_DURATION = 24 * 60 * 60 * 1000;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledRebootService");

    public ScheduledRebootService() {
        super(logTag);
    }

    @Override
    protected void onHandleIntent(Intent inputIntent) {
        Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
        sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));

        RfcxGuardian app = (RfcxGuardian) getApplication();
        if (app.initializedDateTime.get(Calendar.YEAR) >= 2022) {
            app.rfcxSvc.triggerService(RebootTriggerService.SERVICE_NAME, true);
        }

    }


}
