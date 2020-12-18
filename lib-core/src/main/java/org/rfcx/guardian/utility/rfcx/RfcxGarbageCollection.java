package org.rfcx.guardian.utility.rfcx;

import android.content.Context;
import org.rfcx.guardian.utility.asset.RfcxAudioUtils;
import org.rfcx.guardian.utility.asset.RfcxLogcatUtils;
import org.rfcx.guardian.utility.asset.RfcxPhotoUtils;
import org.rfcx.guardian.utility.asset.RfcxScreenShotUtils;
import org.rfcx.guardian.utility.asset.RfcxVideoUtils;
import org.rfcx.guardian.utility.misc.FileUtils;

public class RfcxGarbageCollection {
	
	public RfcxGarbageCollection(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxGarbageCollection");
		this.context = context;
		this.appRole = appRole;
	}
	
	private String logTag;
	
	private Context context;
	private String appRole;
	
	public static void runAndroidGarbageCollection() {
		
		Runtime.getRuntime().runFinalization();
		System.runFinalization();
		
		Runtime.getRuntime().gc();
		System.gc();
	}
	
	public static void runRfcxGarbageCollection(Context context) {
		
		String[] captureDirectories = new String[] {
				RfcxLogcatUtils.logCaptureDir(context),
				RfcxLogcatUtils.logPostCaptureDir(context),
				RfcxScreenShotUtils.screenShotCaptureDir(context),
				RfcxPhotoUtils.photoCaptureDir(context),
				RfcxVideoUtils.videoCaptureDir(context),
				RfcxAudioUtils.audioCaptureDir(context),
				RfcxAudioUtils.audioEncodeDir(context)
		};
		
		int expirationAgeInDays = 2; // delete files older than two days old
		
		for (String dirPath : captureDirectories) {
			
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(dirPath, ( expirationAgeInDays * 24 * 60 ));
			
		}
		
	}

}
