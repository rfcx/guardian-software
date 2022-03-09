package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceAirplaneMode {

    private final String logTag;

    public DeviceAirplaneMode(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceAirplaneMode");
    }

    public static boolean isEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    private static void setAirplaneMode(Context context, int value, String logTag) {
        try {
            Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, value);
            Intent airplaneModeIntent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            airplaneModeIntent.putExtra("state", value == 1);
            context.sendBroadcast(airplaneModeIntent);
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    public void setOn(Context context) {
        Log.v(logTag, "Turning AirplaneMode ON");
        setAirplaneMode(context, 1, logTag);
    }

    public void setOff(Context context) {
        Log.v(logTag, "Turning AirplaneMode OFF");
        setAirplaneMode(context, 0, logTag);
    }

}
