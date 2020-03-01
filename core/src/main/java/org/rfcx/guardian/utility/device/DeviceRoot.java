package org.rfcx.guardian.utility.device;

import android.os.Build;

import java.io.File;

public class DeviceRoot {
    public static boolean isRooted() {
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }
        File file = new File("/system/app/KingoUser.apk");
        return file.exists();
    }
}


