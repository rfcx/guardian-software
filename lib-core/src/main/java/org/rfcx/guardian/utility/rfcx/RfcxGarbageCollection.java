package org.rfcx.guardian.utility.rfcx;

import android.content.Context;

import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxLogcatFileUtils;
import org.rfcx.guardian.utility.asset.RfcxPhotoFileUtils;
import org.rfcx.guardian.utility.asset.RfcxScreenShotFileUtils;
import org.rfcx.guardian.utility.asset.RfcxVideoFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;

public class RfcxGarbageCollection {

    private final String logTag;
    private final Context context;
    private final String appRole;
    public RfcxGarbageCollection(Context context, String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "RfcxGarbageCollection");
        this.context = context;
        this.appRole = appRole;
    }

    public static void runAndroidGarbageCollection() {

        Runtime.getRuntime().runFinalization();
        System.runFinalization();

        Runtime.getRuntime().gc();
        System.gc();
    }

    public static void runRfcxGarbageCollection(Context context) {

        String[] captureDirectories = new String[]{
                RfcxLogcatFileUtils.logcatCaptureDir(context),
                RfcxLogcatFileUtils.logcatPostCaptureDir(context),
                RfcxScreenShotFileUtils.screenShotCaptureDir(context),
                RfcxPhotoFileUtils.photoCaptureDir(context),
                RfcxVideoFileUtils.videoCaptureDir(context),
                RfcxAudioFileUtils.audioCaptureDir(context),
                RfcxAudioFileUtils.audioEncodeDir(context)
        };

        int expirationAgeInDays = 2; // delete files older than two days old

        for (String dirPath : captureDirectories) {

            FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(dirPath, (expirationAgeInDays * 24 * 60));

        }

    }

}
