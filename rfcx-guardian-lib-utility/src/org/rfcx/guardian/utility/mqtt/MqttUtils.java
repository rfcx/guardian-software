package org.rfcx.guardian.utility.mqtt;

import android.content.Context;

public class MqttUtils {

	public MqttUtils(Context context, String appName) {
		this.logTag = (new StringBuilder()).append("Rfcx-").append(appName).append("-").append(MqttUtils.class.getSimpleName()).toString();
	}
	
	private String logTag = (new StringBuilder()).append("Rfcx-Utils-").append(MqttUtils.class.getSimpleName()).toString();
	
	private String filePath_authCertificate = null;
	private String filePath_authKey = null;
	
	

	
}
