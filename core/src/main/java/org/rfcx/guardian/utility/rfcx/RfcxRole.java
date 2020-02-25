package org.rfcx.guardian.utility.rfcx;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;

public class RfcxRole {

	public static final class updater {
		public static final String AUTHORITY = "org.rfcx.guardian.updater";
		public static final String[] PROJECTION_1 = { "role", "version" };
		public static final String ENDPOINT_1 = "software";
		public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
	}

	private static final String logTag = RfcxLog.generateLogTag("Utils", RfcxRole.class);
	
	public static final String[] 
			
		ALL_ROLES= new String[] { 
			"guardian",
			"admin",
			"setup",
			"updater"  
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
		return (new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/org.rfcx.org.rfcx.guardian.guardian."))+"/org.rfcx.org.rfcx.guardian.guardian."+appRole.toLowerCase(Locale.US))).exists();
	}
	
	public static List<String> getInstalledRoleVersions(String thisAppRole, Context context) {

		List<String> softwareVersions = new ArrayList<String>();

		for (String appRole : RfcxRole.ALL_ROLES) {
			
			String roleVersion = null;
			
			try {
				
				if (appRole.equalsIgnoreCase(thisAppRole)) {
					roleVersion = RfcxRole.getRoleVersion(context, logTag);
					
				} else {
					
					// try to get version from content provider
					Cursor versionCursor = context.getContentResolver().query(
							RfcxComm.getUri(appRole, "version", null), RfcxComm.getProjection(appRole, "version"), null, null, null);
					
					if (		(versionCursor != null) 
						&& 	(versionCursor.getCount() > 0)
						) { 
						if (versionCursor.moveToFirst()) {
							try { 
								do {
									if (		(versionCursor.getColumnIndex("app_role") > -1) 
										&& 	versionCursor.getString(versionCursor.getColumnIndex("app_role")).equalsIgnoreCase(appRole)
										) {
											roleVersion = versionCursor.getString(versionCursor.getColumnIndex("app_version"));
									}
								} while (versionCursor.moveToNext()); 
							} finally { versionCursor.close(); } 
						} 
					}

					// if still not found, try to get version from app directory
					if (roleVersion == null) {
						
						String versionFromFile = RfcxPrefs.readFromGuardianRoleTxtFile(context, logTag, thisAppRole, appRole, "version");
						if (versionFromFile != null) { roleVersion = versionFromFile; }
					}
				}
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				
			} finally {
				if (roleVersion != null) { 
					softwareVersions.add(appRole+"*"+roleVersion);
				} else {
//					Log.e(logTag, "Failed to retrieve version for app role '"+appRole+"'");
				}
				
			}
		}
		
		return softwareVersions;
	}
	
}
