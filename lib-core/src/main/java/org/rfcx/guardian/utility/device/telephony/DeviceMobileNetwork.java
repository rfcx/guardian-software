package org.rfcx.guardian.utility.device.telephony;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Date;

public class DeviceMobileNetwork {

    private final String logTag;
    private TelephonyManager telephonyManager;
    private SignalStrength telephonySignalStrength;
    public DeviceMobileNetwork(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceMobileNetwork");
    }

    public static String[] getMobileNetworkSummary(TelephonyManager telephonyManager, SignalStrength signalStrength) {

        // array indices are: measured_at, signal_strength (dBm), network_type, carrier_name
        String[] mobileNetworkSummary = new String[]{"" + (new Date()).getTime(), "", "", ""};

        // GSM values
        boolean isGsmActive = signalStrength.isGsm();
        int gsmBitErrorRate = signalStrength.getGsmBitErrorRate(); // bit error rate values (0-7, 99) as defined in TS 27.007 8.5
        int gsmSignalStrength = signalStrength.getGsmSignalStrength(); // strength values (0-31, 99) as defined in TS 27.007 8.5
        int gsmSignalStrength_dBm = (-113 + (2 * gsmSignalStrength)); // converting signal strength to decibel-milliwatts (dBm)

        // CDMA values
//		int	cdmaRssi = signalStrength.getCdmaDbm(); // CDMA RSSI value in dBm
//		int	cdmaEcIo = signalStrength.getCdmaEcio(); //CDMA Ec/Io value in dB*10

        // EVDO values
//		int	evdoRssi = signalStrength.getEvdoDbm(); //EVDO RSSI value in dBm
//		int	evdoEcIo = signalStrength.getEvdoEcio(); //EVDO Ec/Io value in dB*10
//		int	evdoSnr = signalStrength.getEvdoSnr(); //signal to noise ratio. Valid values are 0-8. 8 is the highest.

        if (gsmSignalStrength_dBm > 0) {
            mobileNetworkSummary[1] = "-120";
        } else {
            mobileNetworkSummary[1] = "" + gsmSignalStrength_dBm;
            mobileNetworkSummary[2] = getNetworkTypeCategoryAsString(telephonyManager.getNetworkType());
            mobileNetworkSummary[3] = telephonyManager.getNetworkOperatorName().replaceAll("\\p{Z}", "");
        }

        return mobileNetworkSummary;
    }

    private static String getNetworkTypeCategoryAsString(int getNetworkType) {
        String networkTypeCategory;
        switch (getNetworkType) {
            case TelephonyManager.NETWORK_TYPE_IDEN:
                networkTypeCategory = "iden";
                break;
            case TelephonyManager.NETWORK_TYPE_GPRS:
                networkTypeCategory = "gprs";
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                networkTypeCategory = "edge";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                networkTypeCategory = "umts";
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                networkTypeCategory = "cdma";
                break;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                networkTypeCategory = "1xrtt";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                networkTypeCategory = "evdo0";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                networkTypeCategory = "evdoA";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                networkTypeCategory = "evdoB";
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                networkTypeCategory = "hsdpa";
                break;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                networkTypeCategory = "hsupa";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                networkTypeCategory = "hspa";
                break;
            default:
                networkTypeCategory = "unknown";
        }
        return networkTypeCategory;
    }

    public boolean isInitializedTelephonyManager() {
        return (this.telephonyManager != null);
    }

    public boolean isInitializedSignalStrength() {
        return (this.telephonySignalStrength != null);
    }

    public void setTelephonySignalStrength(SignalStrength telephonySignalStrength) {
        this.telephonySignalStrength = telephonySignalStrength;
    }

    public TelephonyManager getTelephonyManager() {
        return this.telephonyManager;
    }

    public void setTelephonyManager(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    public void setTelephonyListener(PhoneStateListener phoneStateListener, int events) {
        if (this.telephonyManager != null) {
            this.telephonyManager.listen(phoneStateListener, events);
        }
    }

    public JSONArray getSignalStrengthAsJsonArray() {
        JSONArray signalJsonArray = new JSONArray();
        if (isInitializedSignalStrength()) {
            try {
                JSONObject signalJson = new JSONObject();

                signalJson.put("signal", this.telephonySignalStrength.getGsmSignalStrength());
                signalJsonArray.put(signalJson);

            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);

            } finally {
                return signalJsonArray;
            }
        }
        return signalJsonArray;
    }

    public String[] getMobileNetworkSummary() {
        return getMobileNetworkSummary(this.telephonyManager, this.telephonySignalStrength);
    }


}
