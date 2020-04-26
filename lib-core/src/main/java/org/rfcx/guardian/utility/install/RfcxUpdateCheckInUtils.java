package org.rfcx.guardian.utility.install;

import android.content.Context;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class RfcxUpdateCheckInUtils {
	
	public RfcxUpdateCheckInUtils (Context context, String appRole) {
		this.context = context;
		this.appRole = appRole;
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxUpdateCheckInUtils");
	}
	
	private Context context;
	private String logTag;
	private String appRole = "Utils";
	
//	public long lastApiCheckVersionAt = System.currentTimeMillis();
//	private long lastApiCheckVersionTriggeredAt = 0;
//
//	private String getApiCheckVersionUrl(String apiUrlBase, String guardianGuid, String targetAppRoleApiEndpoint) {
//
//		return (new StringBuilder())
//				.append( (apiUrlBase!=null) ? apiUrlBase : "https://api.rfcx.org" )
//				.append("/v1/guardians/").append(guardianGuid).append("/software/").append(targetAppRoleApiEndpoint)
//				.append("?role=").append("")
//				.append("&version=").append("")
//				.append("&battery=").append("")
//				.append("&timestamp=").append(System.currentTimeMillis())
//				.toString();
//
//	}
//
//	public boolean allowApiCheckVersion() {
//		if ((System.currentTimeMillis() - this.lastApiCheckVersionTriggeredAt) > 1000) {
//			this.lastApiCheckVersionTriggeredAt = System.currentTimeMillis();
//			return true;
//		} else {
//			Log.d(logTag,"Skipping attempt to double check-in");
//			return false;
//		}
//	}
	
}
