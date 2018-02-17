package org.rfcx.guardian.utility.mqtt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Locale;

import javax.crypto.Cipher;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.util.Log;

public class AuthCertificateUtils {

	public AuthCertificateUtils(String appRole, String guardianGuid) {
		this.logTag = RfcxLog.generateLogTag(appRole, AuthCertificateUtils.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", AuthCertificateUtils.class);
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	

		
}
