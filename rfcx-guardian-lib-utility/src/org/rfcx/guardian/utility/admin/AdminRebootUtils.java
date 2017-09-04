package org.rfcx.guardian.utility.admin;

import android.location.LocationManager;

public class AdminRebootUtils {
	
	public AdminRebootUtils(String appRole) {
		this.logTag = this.logTag = (new StringBuilder()).append("Rfcx-").append(appRole).append("-").append(AdminRebootUtils.class.getSimpleName()).toString();
		this.appRole = appRole;
	}

	private String logTag = (new StringBuilder()).append("Rfcx-Utils-").append(AdminRebootUtils.class.getSimpleName()).toString();
	
	private String appRole = "Utils";

	/// add some stuff here...
	
	
	
	
}
