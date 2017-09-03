package org.rfcx.guardian.utility.rfcx;


import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public class RfcxRole {

	private static final String logTag = "Rfcx-Utils-"+RfcxRole.class.getSimpleName();
	
	public static final String[] 
			
			ALL_ROLES= new String[] { 
		
				"guardian",
				"admin",
				
				"updater",
				"setup",  
				
//				"api", 
//				"audio",  
//				"carrier", 
//				"connect",
//				"encode", 
//				"reboot", 
//				"sentinel",
//				"system"
			};
	
	public static final class ContentProvider {
		
		public static final class guardian {
			
			public static final String AUTHORITY = "org.rfcx.guardian.guardian";
			
			public static final String[] PROJECTION_PREFS = { "pref_key", "pref_value" };
			public static final String ENDPOINT_PREFS = "prefs";
			public static final String URI_PREFS = "content://"+AUTHORITY+"/"+ENDPOINT_PREFS;

			public static final String[] PROJECTION_VERSION = { "role", "version" };
			public static final String ENDPOINT_VERSION = "version";
			public static final String URI_VERSION = "content://"+AUTHORITY+"/"+ENDPOINT_VERSION;
			
		}
		
		public static final class updater {
			
			public static final String AUTHORITY = "org.rfcx.guardian.updater";
			
			public static final String[] PROJECTION_PREFS = { "pref_key", "pref_value" };
			public static final String ENDPOINT_PREFS = "prefs";
			public static final String URI_PREFS = "content://"+AUTHORITY+"/"+ENDPOINT_PREFS;

			public static final String[] PROJECTION_VERSION = { "role", "version" };
			public static final String ENDPOINT_VERSION = "version";
			public static final String URI_VERSION = "content://"+AUTHORITY+"/"+ENDPOINT_VERSION;
			
//			public static final String[] PROJECTION_1 = { "role", "version" };
//			public static final String ENDPOINT_1 = "software";
//			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class setup {
			
			public static final String AUTHORITY = "org.rfcx.guardian.setup";
			
			public static final String[] PROJECTION_PREFS = { "pref_key", "pref_value" };
			public static final String ENDPOINT_PREFS = "prefs";
			public static final String URI_PREFS = "content://"+AUTHORITY+"/"+ENDPOINT_PREFS;

			public static final String[] PROJECTION_VERSION = { "role", "version" };
			public static final String ENDPOINT_VERSION = "version";
			public static final String URI_VERSION = "content://"+AUTHORITY+"/"+ENDPOINT_VERSION;
			
		}
		
		
		
//		public static final class api {
//			public static final String AUTHORITY = "org.rfcx.guardian.api";
//			public static final String[] PROJECTION_CHECKIN = { "checkin_id" };
//			public static final String ENDPOINT_CHECKIN = "checkins";
//			public static final String URI_CHECKIN = "content://"+AUTHORITY+"/"+ENDPOINT_CHECKIN;
//		}
//		
//		public static final class audio {
//			public static final String AUTHORITY = "org.rfcx.guardian.audio";
//			public static final String[] PROJECTION_1 = { "created_at", "timestamp", "format", "digest", "filepath" };
//			public static final String ENDPOINT_1 = "audio";
//			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
//		}
//		
//		public static final class carrier {
//			public static final String AUTHORITY = "org.rfcx.guardian.carrier";
//			public static final String[] PROJECTION_1 = { "current_time" };
//			public static final String ENDPOINT_1 = "events";
//			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
//		}
//		
//		public static final class connect {
//			public static final String AUTHORITY = "org.rfcx.guardian.connect";
//			public static final String[] PROJECTION_1 = { "last_connected_at" };
//			public static final String ENDPOINT_1 = "events";
//			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
//		}
//		
//		public static final class encode {
//			public static final String AUTHORITY = "org.rfcx.guardian.encode";
//			public static final String[] PROJECTION_QUEUE = { "created_at", "timestamp", "format", "digest", "samplerate", "bitrate", "codec", "duration", "encode_duration", "filepath" };
//			public static final String ENDPOINT_QUEUE = "queue";
//			public static final String URI_QUEUE = "content://"+AUTHORITY+"/"+ENDPOINT_QUEUE;
//			public static final String[] PROJECTION_ENCODED = { "created_at", "timestamp", "format", "digest", "samplerate", "bitrate", "codec", "duration", "encode_duration", "filepath" };
//			public static final String ENDPOINT_ENCODED = "encoded";
//			public static final String URI_ENCODED = "content://"+AUTHORITY+"/"+ENDPOINT_ENCODED;
//		}
//		
//		public static final class reboot {
//			public static final String AUTHORITY = "org.rfcx.guardian.reboot";
//			public static final String[] PROJECTION_1 = { "current_time" };
//			public static final String ENDPOINT_1 = "events";
//			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
//		}
//		
//		public static final class sentinel {
//			public static final String AUTHORITY = "org.rfcx.guardian.sentinel";
//			public static final String[] PROJECTION_1 = { "current_time" };
//			public static final String ENDPOINT_1 = "meta";
//			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
//		}
//		
//		public static final class system {
//			public static final String AUTHORITY = "org.rfcx.guardian.system";
//			public static final String[] PROJECTION_META = { "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer", "disk_usage", "accelerometer", "reboots" };
//			public static final String ENDPOINT_META = "meta";
//			public static final String URI_META = "content://"+AUTHORITY+"/"+ENDPOINT_META;
//			public static final String[] PROJECTION_SCREENSHOT = { "created_at", "timestamp", "format", "digest", "filepath" };
//			public static final String ENDPOINT_SCREENSHOT = "screenshots";
//			public static final String URI_SCREENSHOT = "content://"+AUTHORITY+"/"+ENDPOINT_SCREENSHOT;
//		}
		
	}
	
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
