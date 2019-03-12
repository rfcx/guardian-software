package rfcx.utility.mqtt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Locale;

import javax.crypto.Cipher;

import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class AuthCertificateUtils {

	public AuthCertificateUtils(String appRole, String guardianGuid) {
		this.logTag = RfcxLog.generateLogTag(appRole, AuthCertificateUtils.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", AuthCertificateUtils.class);
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	

		
}
