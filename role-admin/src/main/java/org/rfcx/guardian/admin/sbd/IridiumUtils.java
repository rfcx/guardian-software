package org.rfcx.guardian.admin.sbd;

import android.content.Context;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class IridiumUtils {

    public IridiumUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "IridiumUtils");

    private Context context;
    private RfcxGuardian app;


    public void setPower(boolean setToOn) {
        app.deviceGPIOUtils.runGPIOCommand("DOUT", "iridium_power", !setToOn);
    }

}
