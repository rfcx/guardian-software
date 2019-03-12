package rfcx.utility.install;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.util.Log;
import rfcx.utility.http.HttpGet;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.rfcx.RfcxLog;

public class RfcxUpdateCheckInUtils {
	
	public RfcxUpdateCheckInUtils (Context context, String appRole) {
		this.context = context;
		this.appRole = appRole;
		this.logTag = RfcxLog.generateLogTag(appRole, RfcxUpdateCheckInUtils.class);
	}
	
	private Context context;
	private String logTag = RfcxLog.generateLogTag("Utils", RfcxUpdateCheckInUtils.class);
	private String appRole = "Utils";
	
	public long lastApiCheckVersionAt = System.currentTimeMillis();
	private long lastApiCheckVersionTriggeredAt = 0;
	
	private String getApiCheckVersionUrl(String apiUrlBase, String guardianGuid, String targetAppRoleApiEndpoint) {
		
		return (new StringBuilder())
				.append( (apiUrlBase!=null) ? apiUrlBase : "https://api.rfcx.org" )
				.append("/v1/guardians/").append(guardianGuid).append("/software/").append(targetAppRoleApiEndpoint)
				.append("?role=").append("")
				.append("&version=").append("")
				.append("&battery=").append("")
				.append("&timestamp=").append(System.currentTimeMillis())
				.toString();
		
	}
	
	public boolean allowApiCheckVersion() {
		if ((System.currentTimeMillis() - this.lastApiCheckVersionTriggeredAt) > 1000) {
			this.lastApiCheckVersionTriggeredAt = System.currentTimeMillis();
			return true;
		} else {
			Log.d(logTag,"Skipping attempt to double check-in");
			return false;
		}
	}
	
}
