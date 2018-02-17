package rfcx.utility.rfcx;


import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public class RfcxRole {

	private static final String logTag = RfcxLog.generateLogTag("Utils", RfcxRole.class);
	
	public static final String[] 
			
		ALL_ROLES= new String[] { 
			"guardian",
			"admin",
			"updater",
			"setup"  
		};
	
	public static String getRoleVersion(Context context, String logTag) {
		String version = null;
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName.trim();
		} catch (NameNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		}
		return version;
	}
	
	public static int getRoleVersionValue(String versionName) {
		try {
			int majorVersion = (int) Integer.parseInt(versionName.substring(0, versionName.indexOf(".")));
			int subVersion = (int) Integer.parseInt(versionName.substring(1+versionName.indexOf("."), versionName.lastIndexOf(".")));
			int updateVersion = (int) Integer.parseInt(versionName.substring(1+versionName.lastIndexOf(".")));
			return 1000*majorVersion+100*subVersion+updateVersion;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return 0;
	}

	public static boolean isRoleInstalled(Context context, String appRole) {
		String mainAppPath = context.getFilesDir().getAbsolutePath();
		return (new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/org.rfcx.guardian."))+"/org.rfcx.guardian."+appRole.toLowerCase(Locale.US))).exists();
	}
	
}
