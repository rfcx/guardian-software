package org.rfcx.guardian.utility.rfcx;


import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public class RfcxRole {

	private static final String TAG = "Rfcx-Utils-"+RfcxRole.class.getSimpleName();
	
	public static final String[] 
			
			ALL_ROLES= new String[] { 
				"api", 
				"audio", 
				"carrier", 
				"connect", 
				"installer", 
				"reboot", 
				"sentinel", 
				"system", 
				"updater"
			};
	
	public static final class RoleApi {
		
		public static final class api {
			public static final String AUTHORITY = "org.rfcx.guardian.api";
			public static final String[] PROJECTION_1 = { "checkin_id" };
			public static final String ENDPOINT_1 = "checkins";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class audio {
			public static final String AUTHORITY = "org.rfcx.guardian.audio";
			public static final String[] PROJECTION_1 = { "created_at", "timestamp", "format", "digest", "filepath" };
			public static final String ENDPOINT_1 = "audio";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class carrier {
			public static final String AUTHORITY = "org.rfcx.guardian.carrier";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class connect {
			public static final String AUTHORITY = "org.rfcx.guardian.connect";
			public static final String[] PROJECTION_1 = { "last_connected_at" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class installer {
			public static final String AUTHORITY = "org.rfcx.guardian.installer";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class reboot {
			public static final String AUTHORITY = "org.rfcx.guardian.reboot";
			public static final String[] PROJECTION_1 = { "created_at", "rebooted_at" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class sentinel {
			public static final String AUTHORITY = "org.rfcx.guardian.sentinel";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "meta";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class system {
			public static final String AUTHORITY = "org.rfcx.guardian.system";
			public static final String[] PROJECTION_META = { "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer", "disk_usage", "accelerometer" };
			public static final String ENDPOINT_META = "meta";
			public static final String URI_META = "content://"+AUTHORITY+"/"+ENDPOINT_META;
			public static final String[] PROJECTION_SCREENSHOT = { "created_at", "timestamp", "format", "digest", "filepath" };
			public static final String ENDPOINT_SCREENSHOT = "screenshots";
			public static final String URI_SCREENSHOT = "content://"+AUTHORITY+"/"+ENDPOINT_SCREENSHOT;
		}
		
		public static final class updater {
			public static final String AUTHORITY = "org.rfcx.guardian.updater";
			public static final String[] PROJECTION_1 = { "role", "version" };
			public static final String ENDPOINT_1 = "software";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
	}
	
	public static String getRoleVersion(Context context, String logTag) {
		String version = null;
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName.trim();
		} catch (NameNotFoundException e) {
			RfcxLog.logExc(TAG, e);
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
			RfcxLog.logExc(TAG, e);
		}
		return 0;
	}

	public static boolean isRoleInstalled(Context context, String appRole) {
		String mainAppPath = context.getFilesDir().getAbsolutePath();
		return (new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/org.rfcx.guardian."))+"/org.rfcx.guardian."+appRole.toLowerCase(Locale.US))).exists();
	}
	
}
