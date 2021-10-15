package org.rfcx.guardian.utility.device.control;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceControlUtils {

    private String logTag;
    private String appRole = "Guardian";

    public DeviceControlUtils(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceControlUtils");
        this.appRole = appRole;
    }

    public boolean runOrTriggerDeviceCommand(String cmdFunc, String cmdVal, ContentResolver contentResolver) {

        // replace this with something that more dynamically determines whether the roles has root access
        boolean mustUseContentProvider = appRole.equalsIgnoreCase("Guardian");

        if (mustUseContentProvider) {
            try {

                String[] updaterFunctions = new String[]{"software_update", "software_install"};
                String targetRole = ArrayUtils.doesStringArrayContainString(updaterFunctions, cmdFunc) ? "updater" : "admin";

                String function = (cmdVal == null) ? "control" : cmdFunc;
                String command = (cmdVal == null) ? cmdFunc : cmdVal;

                Log.v(logTag, "Triggering '" + function + "' -> '" + command + "' via " + targetRole + " role content provider.");

                Cursor deviceControlResponse = contentResolver.query(
                        RfcxComm.getUri(targetRole, function, command),
                        RfcxComm.getProjection(targetRole, function),
                        null, null, null);

                if (deviceControlResponse != null) {
                    Log.v(logTag, deviceControlResponse.toString());
                    deviceControlResponse.close();
                }
                return true;
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
                return false;
            }
        } else {

            if (cmdFunc.equalsIgnoreCase("reboot")) {
                // should we trigger the service(s) directly here?
            }
        }
        return false;
    }


}
