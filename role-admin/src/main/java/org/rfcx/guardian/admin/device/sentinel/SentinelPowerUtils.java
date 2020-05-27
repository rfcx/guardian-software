package org.rfcx.guardian.admin.device.sentinel;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.rfcx.guardian.admin.RfcxGuardian;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.device.DeviceI2cUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SentinelPowerUtils {

    public SentinelPowerUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.deviceI2cUtils = new DeviceI2cUtils(sentinelPowerI2cMainAddress);
        initSentinelPowerI2cOptions();
        setOrResetSentinelPowerChip();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelPowerUtils");

    RfcxGuardian app;
    private DeviceI2cUtils deviceI2cUtils = null;
    private static final String sentinelPowerI2cMainAddress = "0x68";

    private String[] i2cValueIndex = new String[]{};
    private Map<String, double[]> i2cTmpValues = new HashMap<>();
    private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();

    private List<double[]> powerBatteryValues = new ArrayList<>();
    private List<double[]> powerInputValues = new ArrayList<>();
    private List<double[]> powerSystemValues = new ArrayList<>();

    private boolean verboseLogging = false;

    public boolean isCaptureAllowed() {

        boolean isNotExplicitlyDisabled = app.rfcxPrefs.getPrefAsBoolean("admin_enable_sentinel_capture");
        boolean isI2cHandlerAccessible = false;
        boolean isI2cPowerChipConnected = false;

        if (isNotExplicitlyDisabled) {
            isI2cHandlerAccessible = (new File("/dev/i2c-"+DeviceI2cUtils.i2cInterface)).canRead();
            if (isI2cHandlerAccessible) {
                String i2cConnectAttempt = this.deviceI2cUtils.i2cGetAsString("0x4a", true);
                isI2cPowerChipConnected = ((i2cConnectAttempt != null) && (Math.abs(DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)) > 0));
            }
        }
        return isNotExplicitlyDisabled && isI2cHandlerAccessible && isI2cPowerChipConnected;
    }

    private void initSentinelPowerI2cOptions() {

        this.i2cValueIndex = new String[]{"voltage", "current", "temperature", "power"};
        //										        voltage     current     temp
        this.i2cAddresses.put("system", new String[]{   "0x3c",     null,       "0x3f"  });
        this.i2cAddresses.put("battery", new String[]{  "0x3a",     "0x3d",     null  });
        this.i2cAddresses.put("input", new String[]{    "0x3b",     "0x3e",     null  });

        resetI2cTmpValues();
    }

    public void setOrResetSentinelPowerChip() {

        if (isCaptureAllowed()) {

            List<String[]> i2cLabelsAddressesValues = new ArrayList<String[]>();
            i2cLabelsAddressesValues.add(new String[]{"force_meas_sys_on", "0x14", "0x0018"});   //000011000  // bits 3 and 4 set to 1
            this.deviceI2cUtils.i2cSet(i2cLabelsAddressesValues);

        } else {
            Log.e(logTag, "Skipping setOrResetSentinelPowerChip() because Sentinel capture is not allowed or not possible.");
        }
    }

    private void resetI2cTmpValues() {
        resetI2cTmpValue("system");
        resetI2cTmpValue("battery");
        resetI2cTmpValue("input");
    }

    private void resetI2cTmpValue(String statAbbr) {
        											/*	voltage		current 	temp		power       captured_at     */
        this.i2cTmpValues.put(statAbbr, new double[]{     0,          0,          0,          0,           0           });
    }

    private void cacheI2cTmpValues() {
        StringBuilder logStr = new StringBuilder();
        long rightNow = System.currentTimeMillis();

        double[] sysVals = this.i2cTmpValues.get("system");
        if (ArrayUtils.getAverageAsDouble(sysVals) != 0) {
            powerSystemValues.add(new double[] { sysVals[0], sysVals[1], sysVals[2], sysVals[3], rightNow });
            if (verboseLogging) {
                long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(sysVals);
                logStr.append("[ temp: ").append(sVals[2]).append(" C").append(" ]");
                logStr.append(" [ system: ").append(sVals[0]).append(" mV, ").append(sVals[1]).append(" mA, ").append(sVals[3]).append(" mW").append(" ]");
            }
        }
        double[] battVals = this.i2cTmpValues.get("battery");
        if (ArrayUtils.getAverageAsDouble(battVals) != 0) {
            powerBatteryValues.add(new double[] { battVals[0], battVals[1], battVals[2], battVals[3], rightNow });
            if (verboseLogging) {
                long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(battVals);
                logStr.append(" [ battery: ").append(bVals[0]).append(" mV, ").append(bVals[1]).append(" mA, ").append(bVals[3]).append(" mW").append(" ]");
            }
        }
        double[] inpVals = this.i2cTmpValues.get("input");
        if (ArrayUtils.getAverageAsDouble(inpVals) != 0) {
            powerInputValues.add(new double[] { inpVals[0], inpVals[1], inpVals[2], inpVals[3], rightNow });
            if (verboseLogging) {
                long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(inpVals);
                logStr.append(" [ input: ").append(iVals[0]).append(" mV, ").append(iVals[1]).append(" mA, ").append(iVals[3]).append(" mW").append(" ]");
            }
        }
        if (verboseLogging) { Log.d(logTag, logStr.toString()); }
    }

    private List<String[]> buildI2cQueryList() {
        List<String[]> i2cLabelsAndSubAddresses = new ArrayList<String[]>();
        for (String sentinelLabel : this.i2cAddresses.keySet()) {
            for (int i = 0; i < this.i2cAddresses.get(sentinelLabel).length; i++) {
                if (this.i2cAddresses.get(sentinelLabel)[i] != null) {
                    i2cLabelsAndSubAddresses.add(new String[]{
                        sentinelLabel + "-" + this.i2cValueIndex[i],
                        this.i2cAddresses.get(sentinelLabel)[i]
                    });
                }
            }
        }
        return i2cLabelsAndSubAddresses;
    }

    public void updateSentinelPowerValues() {
        try {

            resetI2cTmpValues();

            for (String[] i2cLabeledOutput : this.deviceI2cUtils.i2cGet(buildI2cQueryList(), true)) {
                String groupName = i2cLabeledOutput[0].substring(0, i2cLabeledOutput[0].indexOf("-"));
                String valueType = i2cLabeledOutput[0].substring(1 + i2cLabeledOutput[0].indexOf("-"));
                double[] valueSet = this.i2cTmpValues.get(groupName);
                int valueTypeIndex = 0;
                for (int i = 0; i < this.i2cValueIndex.length; i++) {
                    if (this.i2cValueIndex[i].equals(valueType)) {
                        valueTypeIndex = i;
                        break;
                    }
                }
                valueSet[valueTypeIndex] = (i2cLabeledOutput[1] == null) ? 0 : applyValueModifier(i2cLabeledOutput[0], Long.parseLong(i2cLabeledOutput[1]));
                valueSet[3] = valueSet[0] * valueSet[1] / 1000;
                this.i2cTmpValues.put(groupName, valueSet);
            }
            calculateMissingSystemPowerValues();
            cacheI2cTmpValues();

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private void calculateMissingSystemPowerValues() {
        double[] sysVals = this.i2cTmpValues.get("system");
        sysVals[3] = this.i2cTmpValues.get("input")[3] - this.i2cTmpValues.get("battery")[3];
        sysVals[1] = 1000 * sysVals[3] / sysVals[0];
        this.i2cTmpValues.put("system", sysVals);
    }

    private static double applyValueModifier(String i2cLabel, long i2cRawValue) {
        double modifiedValue = 0;

        // this is a test to see if this is how i2c handles negative values...
        //if (i2cRawValue > 32767) { i2cRawValue = i2cRawValue - 65535; }

        if (i2cLabel.equals("system-voltage")) {
            modifiedValue = i2cRawValue * 1.648;
        } else if (i2cLabel.equals("system-temperature")) {
            modifiedValue = (i2cRawValue - 12010) / 45.6;

        } else if (i2cLabel.equals("battery-voltage")) {
            modifiedValue = i2cRawValue * 0.192264;
        } else if (i2cLabel.equals("battery-current")) {
            modifiedValue = i2cRawValue * (0.00146487 / 0.003); // hardcoded resistor value R[SNSB] = 0.003 ohms

        } else if (i2cLabel.equals("input-voltage")) {
            modifiedValue = i2cRawValue * 1.648;
        } else if (i2cLabel.equals("input-current")) {
            modifiedValue = i2cRawValue * (0.00146487 / 0.005); // hardcoded resistor value R[SNSI] = 0.005 ohms


        } else {
            Log.d(logTag, "No known value modifier for i2c label '" + i2cLabel + "'.");
        }
        return modifiedValue;
    }

    public void saveSentinelPowerValuesToDatabase() {

        int sampleCount = Math.round((this.powerSystemValues.size()+this.powerBatteryValues.size()+this.powerInputValues.size())/3);

        if (sampleCount > 0) {

            StringBuilder logStr = (new StringBuilder("Saved Average (of ")).append(sampleCount).append(" samples)");

            long[] sysVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerSystemValues));
            this.powerSystemValues = new ArrayList<>();
            app.sentinelPowerDb.dbSentinelPowerSystem.insert(sysVals[4], sysVals[0], sysVals[1], sysVals[2], sysVals[3]);
            logStr.append(" [ temp: ").append(sysVals[2]).append(" C").append(" ]");
            logStr.append(" [ system: ").append(sysVals[0]).append(" mV, ").append(sysVals[1]).append(" mA, ").append(sysVals[3]).append(" mW").append(" ]");

            long[] battVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
            this.powerBatteryValues = new ArrayList<>();
            app.sentinelPowerDb.dbSentinelPowerBattery.insert(battVals[4], battVals[0], battVals[1], battVals[2], battVals[3]);
            logStr.append(" [ battery: ").append(battVals[0]).append(" mV, ").append(battVals[1]).append(" mA, ").append(battVals[3]).append(" mW").append(" ]");

            long[] inpVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerInputValues));
            this.powerInputValues = new ArrayList<>();
            app.sentinelPowerDb.dbSentinelPowerInput.insert(inpVals[4], inpVals[0], inpVals[1], inpVals[2], inpVals[3]);
            logStr.append(" [ input: ").append(inpVals[0]).append(" mV, ").append(inpVals[1]).append(" mA, ").append(inpVals[3]).append(" mW").append(" ]");

            Log.d(logTag, logStr.toString());
        }
    }

    public static JSONArray getSentinelPowerValuesAsJsonArray(Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray powerJsonArray = new JSONArray();
        try {
            JSONObject powerJson = new JSONObject();

            powerJson.put("system", app.sentinelPowerDb.dbSentinelPowerSystem.getConcatRowsWithLabelPrepended("system"));
            powerJson.put("battery", app.sentinelPowerDb.dbSentinelPowerBattery.getConcatRowsWithLabelPrepended("battery"));
            powerJson.put("input", app.sentinelPowerDb.dbSentinelPowerInput.getConcatRowsWithLabelPrepended("input"));
            powerJsonArray.put(powerJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return powerJsonArray;
        }
    }

    public static int deleteSentinelPowerValuesBeforeTimestamp(String timeStamp, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));

        app.sentinelPowerDb.dbSentinelPowerSystem.clearRowsBefore(clearBefore);
        app.sentinelPowerDb.dbSentinelPowerBattery.clearRowsBefore(clearBefore);
        app.sentinelPowerDb.dbSentinelPowerInput.clearRowsBefore(clearBefore);

        return 1;
    }


}
