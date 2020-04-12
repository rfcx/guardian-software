package org.rfcx.guardian.utility.device.capture;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceMobilePhone {
	
	public DeviceMobilePhone(Context context) {
		this.context = context;
	}
	
	private Context context;
	
	private String simPhoneNumber = null;
	private String simSerial = null;
	private String deviceIMSI = null;
	private String deviceIMEI = null;

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceHardwareUtils");
	
	public void setSimPhoneNumber(String simPhoneNumber) {
		if (		(simPhoneNumber != null) 
			&& 	(simPhoneNumber.length() != 0)
		) { 
			this.simPhoneNumber = simPhoneNumber; 
		}
	}
	
	public void setSimSerial(String simSerial) {
		if (		(simSerial != null) 
			&& 	(simSerial.length() != 0)
		) { 
			this.simSerial = simSerial; 
		}
	}
	
	public void setDeviceIMSI(String deviceIMSI) {
		if (		(deviceIMSI != null) 
			&& 	(deviceIMSI.length() != 0)
		) { 
			this.deviceIMSI = deviceIMSI; 
		}
	}
	
	public void setDeviceIMEI(String deviceIMEI) {
		if (		(deviceIMEI != null) 
			&& 	(deviceIMEI.length() != 0)
		) { 
			this.deviceIMEI = deviceIMEI; 
		}
	}
	
	@SuppressLint("MissingPermission")
	private String getSimSerial() {
		setSimSerial(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getSimSerialNumber());
		return this.simSerial;
	}

	@SuppressLint("MissingPermission")
	private String getSimPhoneNumber() {
		setSimPhoneNumber(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
		return this.simPhoneNumber;
	}

	@SuppressLint("MissingPermission")
	private String getDeviceIMSI() {
		setDeviceIMSI(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId());
		return this.deviceIMSI;
	}

	@SuppressLint("MissingPermission")
	private String getDeviceIMEI() {
		setDeviceIMEI(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
		return this.deviceIMEI;
	}

	public Boolean hasSim() {
		TelephonyManager tm = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
		return !(tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT);
	}
	
	public JSONObject getMobilePhoneInfoJson() {
		List<String[]> phoneInfoList = getMobilePhoneInfo();
		JSONObject phoneInfoJson = new JSONObject();
		for (String[] phoneInfo : phoneInfoList) {
			try {
				phoneInfoJson.put(phoneInfo[0], phoneInfo[1]);
			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return phoneInfoJson;
	}
	
	private List<String[]> getMobilePhoneInfo() {
		List<String[]> phoneInfo = new ArrayList<String[]>();
		phoneInfo.add(new String[] { "sim", getSimSerial() });
		phoneInfo.add(new String[] { "number", getSimPhoneNumber() });
		phoneInfo.add(new String[] { "imsi", getDeviceIMSI() });
		phoneInfo.add(new String[] { "imei", getDeviceIMEI() });
		return phoneInfo;
	}
}
