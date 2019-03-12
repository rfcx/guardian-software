package rfcx.utility.rfcx;

import java.security.MessageDigest;

import android.content.Context;
import android.telephony.TelephonyManager;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.device.control.DeviceLogCat;
import rfcx.utility.misc.FileUtils;

public class RfcxGarbageCollection {
	
	public RfcxGarbageCollection(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, RfcxGarbageCollection.class);
		this.context = context;
		this.appRole = appRole;
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", RfcxGarbageCollection.class);
	
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
				DeviceLogCat.captureDir(context),
				DeviceLogCat.postCaptureDir(context),
				RfcxAudioUtils.captureDir(context),
				RfcxAudioUtils.encodeDir(context)
		};
		
		int expirationAgeInDays = 2; // delete files older than two days old
		
		for (String dirPath : captureDirectories) {
			
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(dirPath, ( expirationAgeInDays * 24 * 60 ));
			
		}
		
	}

}
