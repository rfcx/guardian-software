package org.rfcx.guardian.utility.mqtt;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;

public class MqttUtils {

	public MqttUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, MqttUtils.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", MqttUtils.class);
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	
	

	
}
