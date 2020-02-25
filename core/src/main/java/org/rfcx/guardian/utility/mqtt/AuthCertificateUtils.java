package org.rfcx.guardian.utility.mqtt;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AuthCertificateUtils {

	public AuthCertificateUtils(String appRole, String guardianGuid) {
		this.logTag = RfcxLog.generateLogTag(appRole, AuthCertificateUtils.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", AuthCertificateUtils.class);
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	

		
}
