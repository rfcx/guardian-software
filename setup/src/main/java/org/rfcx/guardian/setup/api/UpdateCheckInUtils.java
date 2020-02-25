package org.rfcx.guardian.setup.api;

import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.util.Log;
import org.rfcx.guardian.setup.RfcxGuardian;

public class UpdateCheckInUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, UpdateCheckInUtils.class);

	public long lastCheckInTime = System.currentTimeMillis();

	private long lastCheckInTriggered = 0;
	
	public String apiCheckVersionEndpoint = null;
	public String targetAppRoleApiEndpoint = "all";

	public String apiRegisterEndpoint = null;
	public String apiRegisterToken = "XXXXXXXX";

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
		
		this.lastCheckInTime = System.currentTimeMillis();
		
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
		
			String currentGuardianVersion = app.rfcxPrefs.getVersionFromFile(app.targetAppRole);
			int currentGuardianVersionValue = calculateVersionValue(currentGuardianVersion);
			
			if (	(	(this.latestVersion != null) && (currentGuardianVersion == null))
				||	(!currentGuardianVersion.equals(this.latestVersion) && (currentGuardianVersionValue < this.latestVersionValue))
				) {
				this.installRole = this.latestRole;
				this.installVersion = this.latestVersion;
				this.installVersionUrl = this.latestVersionUrl;
				this.installVersionSha1 = this.latestVersionSha1;
				this.installVersionValue = this.latestVersionValue;
				
				if (isBatteryChargeSufficientForDownloadAndInstall(app)) {
					Log.d(logTag, "Latest version detected and download triggered: "+this.installVersion+" ("+this.installVersionValue+")");	
					app.rfcxServiceHandler.triggerService("DownloadFile", true);
				} else {
					Log.i(logTag, "Download & Installation disabled due to low battery level"
							+" (current: "+app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)+"%, required: "+app.getPref("install_battery_cutoff")+"%)."
							);
				}
				return true;
			} else if (!currentGuardianVersion.equals(this.latestVersion) && (currentGuardianVersionValue > this.latestVersionValue)) { 
				Log.d(logTag,"org.rfcx.org.rfcx.guardian.guardian."+this.latestRole+" is newer than the api version: "+currentGuardianVersion+" ("+currentGuardianVersionValue+")");
			} else {
				Log.d(logTag,"org.rfcx.org.rfcx.guardian.guardian."+this.latestRole+" is already up-to-date: "+currentGuardianVersion+" ("+currentGuardianVersionValue+")");
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}
	
	public void setApiCheckVersionEndpoint(String guardianId) {
		this.apiCheckVersionEndpoint = "/v1/guardians/"+guardianId+"/software/"+this.targetAppRoleApiEndpoint;
	}
	
	public void setApiRegisterEndpoint() {
		this.apiRegisterEndpoint = "/v1/guardians/register";
	}
	
	private static int calculateVersionValue(String versionName) {
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
	
	public boolean allowTriggerCheckIn() {
		if ((System.currentTimeMillis() - lastCheckInTriggered) > 1000) {
			lastCheckInTriggered = System.currentTimeMillis();
			return true;
		} else {
			Log.d(logTag,"Skipping attempt to double check-in");
			return false;
		}
	}
	
	private boolean isBatteryChargeSufficientForDownloadAndInstall(RfcxGuardian app) {
		int batteryCharge = app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		int installBatteryCutoff = (int) Integer.parseInt(app.getPref("install_battery_cutoff"));
		return (batteryCharge >= installBatteryCutoff);
	}
	
}
