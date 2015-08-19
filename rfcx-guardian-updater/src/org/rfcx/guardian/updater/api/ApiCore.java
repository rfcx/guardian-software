package org.rfcx.guardian.updater.api;

import java.util.Calendar;
import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.updater.R;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.text.TextUtils;
import android.util.Log;

public class ApiCore {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+ApiCore.class.getSimpleName();

	public long lastCheckInTime = Calendar.getInstance().getTimeInMillis();

	private long lastCheckInTriggered = 0;
	
	public String apiCheckVersionEndpoint = null;
	public String targetAppRoleApiEndpoint = "all";

	public String latestRole = null;
	public String latestVersion = null;
	private String latestVersionUrl = null;
	private String latestVersionSha1 = null;
	private int latestVersionValue = 0;

	public String installRole = null;
	public String installVersion = null;
	public String installVersionUrl = null;
	public String installVersionSha1 = null;
	private int installVersionValue = 0;
	
	public boolean apiCheckVersionFollowUp(RfcxGuardian app, String targetRole, List<JSONObject> jsonList) {
		
		this.lastCheckInTime = Calendar.getInstance().getTimeInMillis();
		
		try {
		
			for (JSONObject jsonListItem : jsonList) {
				if (jsonListItem.getString("role").equals(targetRole.toLowerCase())) {
					this.latestRole = jsonListItem.getString("role");
					this.latestVersion = jsonListItem.getString("version");
					this.latestVersionUrl = jsonListItem.getString("url");
					this.latestVersionSha1 = jsonListItem.getString("sha1");
					this.latestVersionValue = calculateVersionValue(this.latestVersion);
				}
			}
		
			String currentGuardianVersion = app.getCurrentGuardianTargetRoleVersion();
			int currentGuardianVersionValue = calculateVersionValue(currentGuardianVersion);
			
			if (	(	(this.latestVersion != null) && (currentGuardianVersion == null))
				||	(!currentGuardianVersion.equals(this.latestVersion) && (currentGuardianVersionValue < this.latestVersionValue))
				) {
				this.installRole = this.latestRole;
				this.installVersion = this.latestVersion;
				this.installVersionUrl = this.latestVersionUrl;
				this.installVersionSha1 = this.latestVersionSha1;
				this.installVersionValue = this.latestVersionValue;
				Log.d(TAG, "Latest version detected and download triggered: "+this.installVersion+" ("+this.installVersionValue+")");	
				app.triggerService("DownloadFile", true);
				return true;
			} else if (!currentGuardianVersion.equals(this.latestVersion) && (currentGuardianVersionValue > this.latestVersionValue)) { 
				Log.d(TAG,"org.rfcx.guardian."+this.latestRole+" is newer than the api version: "+currentGuardianVersion+" ("+currentGuardianVersionValue+")");
			} else {
				Log.d(TAG,"org.rfcx.guardian."+this.latestRole+" is already up-to-date: "+currentGuardianVersion+" ("+currentGuardianVersionValue+")");
			}
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return false;
	}
	
	public void setApiCheckVersionEndpoint(String guardianId) {
		this.apiCheckVersionEndpoint = "/v1/guardians/"+guardianId+"/software/"+this.targetAppRoleApiEndpoint+"/latest?role=updater&version=0.0.0";
	}
	
	private static int calculateVersionValue(String versionName) {
		try {
			int majorVersion = (int) Integer.parseInt(versionName.substring(0, versionName.indexOf(".")));
			int subVersion = (int) Integer.parseInt(versionName.substring(1+versionName.indexOf("."), versionName.lastIndexOf(".")));
			int updateVersion = (int) Integer.parseInt(versionName.substring(1+versionName.lastIndexOf(".")));
			return 1000*majorVersion+100*subVersion+updateVersion;
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return 0;
	}
	
	public boolean allowTriggerCheckIn() {
		if ((System.currentTimeMillis() - lastCheckInTriggered) > 1000) {
			lastCheckInTriggered = System.currentTimeMillis();
			return true;
		} else {
			Log.d(TAG,"Skipping attempt to double check-in");
			return false;
		}
	}
}
