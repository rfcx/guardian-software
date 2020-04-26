package org.rfcx.guardian.utility.mqtt;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AuthCertificateUtils {

	public AuthCertificateUtils(String appRole, String guardianGuid) {
		this.logTag = RfcxLog.generateLogTag(appRole, "AuthCertificateUtils");
	}
	
	private String logTag;
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	

		
}
