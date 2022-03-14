package org.rfcx.guardian.utility.device.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class DeviceMobilePhone {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceHardwareUtils");
    private final Context context;

    private String simPhoneNumber = null;
    private String simSerial = null;
    private String deviceIMSI = null;
    private String deviceIMEI = null;

    public DeviceMobilePhone(Context context) {
        this.context = context;
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private String getSimSerial() {
        setSimSerial(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getSimSerialNumber());
        return this.simSerial;
    }

    public void setSimSerial(String simSerial) {
        if ((simSerial != null)
                && (simSerial.length() != 0)
        ) {
            this.simSerial = simSerial;
        }
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public String getSimPhoneNumber() {
        setSimPhoneNumber(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
        return this.simPhoneNumber;
    }

    public void setSimPhoneNumber(String simPhoneNumber) {
        if ((simPhoneNumber != null)
                && (simPhoneNumber.length() != 0)
        ) {
            this.simPhoneNumber = simPhoneNumber;
        }
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private String getDeviceIMSI() {
        setDeviceIMSI(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId());
        return this.deviceIMSI;
    }

    public void setDeviceIMSI(String deviceIMSI) {
        if ((deviceIMSI != null)
                && (deviceIMSI.length() != 0)
        ) {
            this.deviceIMSI = deviceIMSI;
        }
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private String getDeviceIMEI() {
        setDeviceIMEI(((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
        return this.deviceIMEI;
    }

    public void setDeviceIMEI(String deviceIMEI) {
        if ((deviceIMEI != null)
                && (deviceIMEI.length() != 0)
        ) {
            this.deviceIMEI = deviceIMEI;
        }
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
        List<String[]> phoneInfo = new ArrayList<>();
        phoneInfo.add(new String[]{"sim", getSimSerial()});
        phoneInfo.add(new String[]{"number", getSimPhoneNumber()});
        phoneInfo.add(new String[]{"imsi", getDeviceIMSI()});
        phoneInfo.add(new String[]{"imei", getDeviceIMEI()});
        return phoneInfo;
    }
}
