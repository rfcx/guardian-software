package org.rfcx.guardian.utility.rfcx;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import org.rfcx.guardian.utility.misc.StringUtils;

public class RfcxGuardianIdentity {

	public RfcxGuardianIdentity(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxGuardianIdentity");
		this.context = context;
		this.appRole = appRole;
		checkSetPreDefinedGuid();
	}

	private String logTag;

	private Context context;
	private String appRole;

	public static final int GUID_LENGTH = 12;
	public static final int PINCODE_LENGTH = 4;

	private String guid;
	private String serial;
	private String authToken;
	private String keystorePassPhrase;
	private String pinCode;

	private void checkSetPreDefinedGuid() {
		String fromContentProvider = readIdentityInfoFromContentProvider("guid");
		if (fromContentProvider != null) {
			Log.v(logTag, "Predefined Guardian Guid retrieved via content provider");
			setGuid(fromContentProvider);
		} else {
			String fromTxtFile = RfcxPrefs.readFromGuardianRoleTxtFile(this.context, this.logTag, this.appRole, this.appRole, "guid");
			if (fromTxtFile != null) {
				Log.v(logTag, "Predefined Guardian Guid retrieved from file");
				this.guid = fromTxtFile;
			}
		}
	}

	private void checkSetPreDefinedSerial() {
		String fromContentProvider = readIdentityInfoFromContentProvider("serial");
		if (fromContentProvider != null) {
			Log.v(logTag, "Predefined Serial # retrieved via content provider");
			setSerial(fromContentProvider);
		} else {
			String fromTxtFile = RfcxPrefs.readFromGuardianRoleTxtFile(this.context, this.logTag, this.appRole, this.appRole, "serial");
			if (fromTxtFile != null) {
				Log.v(logTag, "Predefined Serial # retrieved from file");
				this.serial = fromTxtFile;
			}
		}
	}

	private void checkSetPreDefinedAuthToken() {
		String fromContentProvider = readIdentityInfoFromContentProvider("token");
		if (fromContentProvider != null) {
			Log.v(logTag, "Predefined Auth Token retrieved via content provider");
			setAuthToken(fromContentProvider);
		} else {
			String fromTxtFile = RfcxPrefs.readFromGuardianRoleTxtFile(this.context, this.logTag, this.appRole, this.appRole, "token");
			if (fromTxtFile != null) {
				Log.v(logTag, "Predefined Auth Token retrieved from file");
				this.authToken = fromTxtFile;
			}
		}
	}

	private void checkSetPreDefinedKeystorePassPhrase() {
		String fromContentProvider = readIdentityInfoFromContentProvider("keystore_passphrase");
		if (fromContentProvider != null) {
			Log.v(logTag, "Predefined Keystore Passphrase retrieved via content provider");
			setKeystorePassPhrase(fromContentProvider);
		} else {
			String fromTxtFile = RfcxPrefs.readFromGuardianRoleTxtFile(this.context, this.logTag, this.appRole, this.appRole, "keystore_passphrase");
			if (fromTxtFile != null) {
				Log.v(logTag, "Predefined Keystore Passphrase retrieved from file");
				this.keystorePassPhrase = fromTxtFile;
			}
		}
	}

	private void checkSetPreDefinedPinCode() {
		String fromContentProvider = readIdentityInfoFromContentProvider("pin_code");
		if (fromContentProvider != null) {
			Log.v(logTag, "Predefined PIN Code retrieved via content provider");
			setPinCode(fromContentProvider);
		} else {
			String fromTxtFile = RfcxPrefs.readFromGuardianRoleTxtFile(this.context, this.logTag, this.appRole, this.appRole, "pin_code");
			if (fromTxtFile != null) {
				Log.v(logTag, "Predefined PIN Code retrieved from file");
				this.pinCode = fromTxtFile;
			}
		}
	}

	public void setIdentityValue(String idKey, String idVal) {
		if (idKey.equalsIgnoreCase("guid")) {
			setGuid(idVal.toLowerCase());
		} else if (idKey.equalsIgnoreCase("serial")) {
			setSerial(idVal.toLowerCase());
		} else if (idKey.equalsIgnoreCase("token")) {
			setAuthToken(idVal.toLowerCase());
		} else if (idKey.equalsIgnoreCase("keystore_passphrase")) {
			setKeystorePassPhrase(idVal);
		} else if (idKey.equalsIgnoreCase("pin_code")) {
			setPinCode(idVal);
		}
	}

	public void setGuid(String guid) {
		RfcxPrefs.writeToGuardianRoleTxtFile(this.context, this.logTag, "guid", guid, true);
		this.guid = guid;
	}

	public void setSerial(String serial) {
		RfcxPrefs.writeToGuardianRoleTxtFile(this.context, this.logTag, "serial", authToken, true);
		this.serial = serial;
	}

	public void setAuthToken(String authToken) {
		RfcxPrefs.writeToGuardianRoleTxtFile(this.context, this.logTag, "token", authToken, true);
		this.authToken = authToken;
	}

	public void setKeystorePassPhrase(String keystorePassPhrase) {
		RfcxPrefs.writeToGuardianRoleTxtFile(this.context, this.logTag, "keystore_passphrase", keystorePassPhrase, true);
		this.keystorePassPhrase = keystorePassPhrase;
	}

	public void setPinCode(String pinCode) {
		RfcxPrefs.writeToGuardianRoleTxtFile(this.context, this.logTag, "pin_code", pinCode, true);
		this.pinCode = pinCode;
	}

	public void unSetIdentityValue(String idKey) {
		if (idKey.equalsIgnoreCase("guid")) {
			this.guid = null;
			RfcxPrefs.deleteGuardianRoleTxtFile(this.context, "guid");
		} else if (idKey.equalsIgnoreCase("serial")) {
			this.serial = null;
			RfcxPrefs.deleteGuardianRoleTxtFile(this.context, "serial");
		} else if (idKey.equalsIgnoreCase("token")) {
			this.authToken = null;
			RfcxPrefs.deleteGuardianRoleTxtFile(this.context, "token");
		} else if (idKey.equalsIgnoreCase("keystore_passphrase")) {
			this.keystorePassPhrase = null;
			RfcxPrefs.deleteGuardianRoleTxtFile(this.context, "keystore_passphrase");
		} else if (idKey.equalsIgnoreCase("pin_code")) {
			this.pinCode = null;
			RfcxPrefs.deleteGuardianRoleTxtFile(this.context, "pin_code");
		}
	}

	public String getIdentityValue(String idKey) {
		if (idKey.equalsIgnoreCase("guid")) {
			return getGuid();
		} else if (idKey.equalsIgnoreCase("serial")) {
			return getSerial();
		} else if (idKey.equalsIgnoreCase("token")) {
			return getAuthToken();
		} else if (idKey.equalsIgnoreCase("keystore_passphrase")) {
			return getKeystorePassphrase();
		} else if (idKey.equalsIgnoreCase("pin_code")) {
			return getPinCode();
		} else {
			return null;
		}
	}
	
    public String getGuid() {
		if (this.guid == null) {
			checkSetPreDefinedGuid();
			if (this.guid == null) {
				Log.e(logTag, "Failed to find pre-defined guid.");
				setGuid(StringUtils.randomAlphanumericString(GUID_LENGTH, false));
				Log.e(logTag, "New Guardian Guid generated (random): "+this.guid);
			}
		}
		return this.guid;
    }

	public String getSerial() {
		if (this.serial == null) {
			checkSetPreDefinedSerial();
			if (this.serial == null) {
				Log.e(logTag, "Failed to find pre-defined serial #.");
				Log.e(logTag, "Serial # cannot be generated by the guardian itself.");
				Log.e(logTag, "Serial # must be set manually.");
			}
		}
		return this.serial;
	}

    public String getAuthToken() {
		if (this.authToken == null) {
			checkSetPreDefinedAuthToken();
			if (this.authToken == null) {
				Log.e(logTag, "Failed to find pre-defined auth token.");
				Log.e(logTag, "Auth token cannot be generated by the guardian itself.");
				Log.e(logTag, "Please re-register this device, or set the value manually via content provider.");
			}
		}
		return this.authToken;
    }

	public String getKeystorePassphrase() {
		if (this.keystorePassPhrase == null) {
			checkSetPreDefinedKeystorePassPhrase();
			if (this.keystorePassPhrase == null) {
				Log.e(logTag, "Failed to find pre-defined keystore passphrase.");
				Log.e(logTag, "Keystore passphrase cannot be generated by the guardian itself.");
				Log.e(logTag, "Please re-register this device, or set the value manually via content provider.");
			}
		}
		return this.keystorePassPhrase;
	}

	public String getPinCode() {
		if (this.pinCode == null) {
			checkSetPreDefinedPinCode();
			if (this.pinCode == null) {
				Log.e(logTag, "Failed to find pre-defined PIN Code.");
				setPinCode("0000000000000000000000000000000000000000".substring(0, PINCODE_LENGTH));
				Log.e(logTag, "PIN Code has been defaulted to '" + this.pinCode + "'.");
				Log.e(logTag, "This default PIN Code may not provide full functionality with the API.");
				Log.e(logTag, "To obtain a valid PIN Code, please re-register this device, or set the value manually via content provider.");
			}
		}
		return this.pinCode;
	}


	private String readIdentityInfoFromContentProvider(String idKey) {
		try {

			if (!this.appRole.equalsIgnoreCase("guardian")) {

				Cursor idCur = this.context.getContentResolver().query(
						RfcxComm.getUri("guardian", "identity", idKey),
						RfcxComm.getProjection("guardian", "identity"),
						null, null, null);

				if ((idCur != null) && (idCur.getCount() > 0)) { if (idCur.moveToFirst()) { try { do {
					if (idCur.getString(idCur.getColumnIndex("identity_key")).equalsIgnoreCase(idKey)) {
						String idVal = idCur.getString(idCur.getColumnIndex("identity_value"));
						return idVal;
					}
				} while (idCur.moveToNext()); } finally { idCur.close(); } } }
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}

	public void reSyncGuardianIdentity() {
		if (this.guid != null) { this.guid = null; checkSetPreDefinedGuid(); }
		if (this.serial != null) { this.serial = null; checkSetPreDefinedSerial(); }
		if (this.authToken != null) { this.authToken = null; checkSetPreDefinedAuthToken(); }
		if (this.keystorePassPhrase != null) { this.keystorePassPhrase = null; checkSetPreDefinedKeystorePassPhrase(); }
		if (this.pinCode != null) { this.pinCode = null; checkSetPreDefinedPinCode(); }
	}

	public void reSyncIdentityInExternalRoleViaContentProvider(String targetAppRole, Context context) {
		try {
			Cursor targetAppRoleResponse =
					context.getContentResolver().query(
							RfcxComm.getUri(targetAppRole, "identity_resync", "all"),
							RfcxComm.getProjection(targetAppRole, "identity_resync"),
							null, null, null);
			if (targetAppRoleResponse != null) {
				targetAppRoleResponse.close();
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
}
