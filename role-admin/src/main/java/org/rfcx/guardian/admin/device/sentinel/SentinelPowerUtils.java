package org.rfcx.guardian.admin.device.sentinel;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.rfcx.guardian.admin.RfcxGuardian;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.i2c.DeviceI2cUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
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

    public boolean isInputPowerAtZero = false;

    public boolean isCaptureAllowed() {

        boolean isNotExplicitlyDisabled = app.rfcxPrefs.getPrefAsBoolean("admin_enable_sentinel_power");
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
            calculateMissingPowerValues();
            cacheI2cTmpValues();

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private void calculateMissingPowerValues() {

        double[] sysVals = this.i2cTmpValues.get("system");
        double[] inpVals = this.i2cTmpValues.get("input");

        sysVals[3] = inpVals[3] - this.i2cTmpValues.get("battery")[3];
        sysVals[1] = 1000 * sysVals[3] / sysVals[0];
        this.i2cTmpValues.put("system", sysVals);

        this.isInputPowerAtZero = !(Math.round(inpVals[3]) > 0);
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

    public void saveSentinelPowerValuesToDatabase(boolean printValuesToLog) {

        int sampleCount = Math.round((this.powerSystemValues.size()+this.powerBatteryValues.size()+this.powerInputValues.size())/3);

        if (sampleCount > 0) {

            long[] sysVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerSystemValues));
            this.powerSystemValues = new ArrayList<>();
            long[] battVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
            this.powerBatteryValues = new ArrayList<>();
            long[] inpVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerInputValues));
            this.powerInputValues = new ArrayList<>();

            double measuredAtAvg = (sysVals[4]+battVals[4]+inpVals[4])/3;
            long measuredAt = Math.round(measuredAtAvg);

            long[] voltages = new long[] { pVal("voltage", sysVals[0]), pVal("voltage", battVals[0]), pVal("voltage", inpVals[0]) };
            long[] currents = new long[] { pVal("current", sysVals[1]), pVal("current", battVals[1]), pVal("current", inpVals[1]) };
            long[] temps = new long[] { pVal("temp", sysVals[2]), pVal("temp", battVals[2]), pVal("temp", inpVals[2]) };
            long[] powers = new long[] { pVal("power", sysVals[3]), pVal("power", battVals[3]), pVal("power", inpVals[3]) };

            app.sentinelPowerDb.dbSentinelPowerSystem.insert( measuredAt, voltages[0], currents[0], temps[0], powers[0] );
            app.sentinelPowerDb.dbSentinelPowerBattery.insert( measuredAt, voltages[1], currents[1], temps[1], powers[1] );
            app.sentinelPowerDb.dbSentinelPowerInput.insert( measuredAt, voltages[2], currents[2], temps[2], powers[2] );
            app.sentinelPowerDb.dbSentinelPowerMeter.insert( measuredAt, battVals[3]);

            if (printValuesToLog) {
                Log.d(logTag,
                    (new StringBuilder("Avg of ")).append(sampleCount).append(" samples at ").append(DateTimeUtils.getDateTime(measuredAt))
                    .append(" [ system: ").append(voltages[0]).append(" mV, ").append(currents[0]).append(" mA, ").append(powers[0]).append(" mW").append(" ]")
                    .append(" [ battery: ").append(voltages[1]).append(" mV, ").append(currents[1]).append(" mA, ").append(powers[1]).append(" mW").append(" ]")
                    .append(" [ input: ").append(voltages[2]).append(" mV, ").append(currents[2]).append(" mA, ").append(powers[2]).append(" mW").append(" ]")
                    .append(" [ temp: ").append(temps[0]).append(" C").append(" ]")
                .toString());
            }
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

    public JSONObject getMomentarySentinelPowerValuesAsJson() {

        JSONObject jsonObj = new JSONObject();

        try {

            updateSentinelPowerValues();

            long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(this.i2cTmpValues.get("battery"));
            JSONObject jsonBatteryObj = new JSONObject();
            jsonBatteryObj.put("voltage",bVals[0]);
            jsonBatteryObj.put("current",bVals[1]);
            jsonBatteryObj.put("power",bVals[3]);
            jsonObj.put("battery",jsonBatteryObj);

            long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(this.i2cTmpValues.get("input"));
            JSONObject jsonInputObj = new JSONObject();
            jsonInputObj.put("voltage",iVals[0]);
            jsonInputObj.put("current",iVals[1]);
            jsonInputObj.put("power",iVals[3]);
            jsonObj.put("input",jsonInputObj);

            long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(this.i2cTmpValues.get("system"));
            JSONObject jsonSystemObj = new JSONObject();
            jsonSystemObj.put("voltage",sVals[0]);
            jsonSystemObj.put("current",sVals[1]);
            jsonSystemObj.put("power",sVals[3]);
            jsonObj.put("system",jsonSystemObj);

        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);
        }

        return jsonObj;
    }

    private static long pVal(String fieldName, long val) {

//        double divVal = val;
//
//
//        if (fieldName.equalsIgnoreCase("voltage")) {
//            divVal = val/100;
//            Log.e(logTag, divVal+"");
//
//        } else if (fieldName.equalsIgnoreCase("current")) {
//            divVal = Math.round(val/100)*100;
//
//        } else if (fieldName.equalsIgnoreCase("power")) {
//            divVal = Math.round(val/100)*100;
//
//        }/* else if (fieldName.equalsIgnoreCase("temps")) {
//
//        }*/

        return val;
    }

    private static int getLiFePO4BatteryChargePercentage(long battMilliVoltage) {

        if (battMilliVoltage >= 3380) {      return 99;  }
        else if (battMilliVoltage >= 3365) { return 95;  }
        else if (battMilliVoltage >= 3350) { return 90;  }
        else if (battMilliVoltage >= 3340) { return 85;  }
        else if (battMilliVoltage >= 3330) { return 80;  }
        else if (battMilliVoltage >= 3315) { return 75;  }
        else if (battMilliVoltage >= 3300) { return 70;  }
        else if (battMilliVoltage >= 3290) { return 65;  }
        else if (battMilliVoltage >= 3280) { return 60;  }
        else if (battMilliVoltage >= 3270) { return 55;  }
        else if (battMilliVoltage >= 3260) { return 50;  }
        else if (battMilliVoltage >= 3255) { return 45;  }
        else if (battMilliVoltage >= 3250) { return 40;  }
        else if (battMilliVoltage >= 3240) { return 35;  }
        else if (battMilliVoltage >= 3230) { return 30;  }
        else if (battMilliVoltage >= 3215) { return 25;  }
        else if (battMilliVoltage >= 3200) { return 20;  }
        else if (battMilliVoltage >= 3190) { return 19;  }
        else if (battMilliVoltage >= 3180) { return 18;  }
        else if (battMilliVoltage >= 3170) { return 17;  }
        else if (battMilliVoltage >= 3160) { return 16;  }
        else if (battMilliVoltage >= 3150) { return 15;  } // cutoff?
        else if (battMilliVoltage >= 3140) { return 14;  }
        else if (battMilliVoltage >= 3105) { return 13;  }
        else if (battMilliVoltage >= 3070) { return 12;  }
        else if (battMilliVoltage >= 3035) { return 11;  }
        else if (battMilliVoltage >= 3000) { return 10;  }
        else if (battMilliVoltage >= 2960) {  return 9;  }
        else if (battMilliVoltage >= 2920) {  return 8;  }
        else if (battMilliVoltage >= 2880) {  return 7;  }
        else if (battMilliVoltage >= 2840) {  return 6;  }
        else if (battMilliVoltage >= 2800) {  return 5;  }
        else if (battMilliVoltage >= 2735) {  return 4;  }
        else if (battMilliVoltage >= 2670) {  return 3;  }
        else if (battMilliVoltage >= 2605) {  return 2;  }
        else if (battMilliVoltage >= 2540) {  return 1;  }
        else {                                return 0;  }
    }

}
