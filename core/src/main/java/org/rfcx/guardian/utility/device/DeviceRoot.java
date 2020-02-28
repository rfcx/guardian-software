package org.rfcx.guardian.utility.device;

import android.os.Build;

import java.io.File;

public class DeviceRoot {
    public static boolean isRooted() {
        String buildTags = Build.TAGS;
        if(buildTags != null && buildTags.contains("test-keys")) {
            return true;
        } else {
            File file = new File("/system/app/Superuser.apk");
            if(file.exists()) {
                return true;
            } else {
                file = new File("/system/xbin/su");
                return file.exists();
            }
        }
    }
}


