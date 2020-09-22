package org.rfcx.guardian.admin.device.sentinel;

import java.io.File;
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
    private String[] getWithoutTwoComplement = new String[] {};

    private List<double[]> powerBatteryValues = new ArrayList<>();
    private List<double[]> powerInputValues = new ArrayList<>();
    private List<double[]> powerSystemValues = new ArrayList<>();

    private boolean verboseLogging = false;

    public boolean isInputPowerAtZero = false;
    public boolean isBatteryCharging = false;
    public boolean isBatteryCharged = false;

    private static final long qCountCalibratedMax = (65535 - 16384);

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

        this.i2cValueIndex =            new String[]{   "voltage",      "current",      "misc",     "power" };

        this.i2cAddresses.put("system", new String[]{   "0x3c",         null,           "0x3f" /* die temperature */    });
        this.i2cAddresses.put("battery", new String[]{  "0x3a",         "0x3d",         "0x13" /* coulomb_counter */    });
        this.i2cAddresses.put("input", new String[]{    "0x3b",         "0x3e",         "0x34" /* charger_state */      });

        this.getWithoutTwoComplement = new String[] { "battery-misc", "input-misc" };

        resetI2cTmpValues();
    }

    public void setOrResetSentinelPowerChip() {

        if (isCaptureAllowed()) {

            List<String[]> i2cLabelsAddressesValues = new ArrayList<String[]>();

            i2cLabelsAddressesValues.add(new String[]{ "config_bits",            "0x14", "0x001c"});    // 000011100 (binary)  // set bits 2, 3, 4 to "1"
                                                                                                        // 'en_qcount' (bit 2) enabled
                                                                                                        // 'mppt_en_i2c' (bit 3) enabled
                                                                                                        // 'force_meas_sys_on' (bit 4) enabled
                                                                                                        // 'run_bsr' (bit 5) disabled
                                                                                                        // 'suspend_charger' (bit 8) disabled

            i2cLabelsAddressesValues.add(new String[]{ "charger_config_bits",    "0x29", "0x0004"});    // 0100 (binary)  // set bit 2 to "1"
                                                                                                        // 'en_c_over_x_term' (bit 2) enabled
                                                                                                        // 'en_lead_acid_temp_comp' (bit 1) disabled
                                                                                                        // 'en_jeita' (bit 0) disabled

            i2cLabelsAddressesValues.add(new String[]{ "max_cv_time",            "0x1d", "0x0000"});    // setting value (in seconds) to zero to disable charging timeout
            i2cLabelsAddressesValues.add(new String[]{ "max_charge_time",        "0x1e", "0x0000"});    // setting value (in seconds) to zero to disable charging timeout
//            i2cLabelsAddressesValues.add(new String[]{ "max_absorb_time",        "0x2b", "0x0000"});    // setting value (in seconds) to zero to disable charging timeout

            i2cLabelsAddressesValues.add(new String[]{ "qcount_prescale_factor", "0x12", "0x002d"});    // 45 (decimal) is QCOUNT_PRESCALE_FACTOR
                                                                                                        // LiFePO4 Battery Capacity: 16.5 Ah * 3600 sec = 59400C
                                                                                                        // qLSB (max) = 59400C / 65535 =  0.906
                                                                                                        //  K_QC: 8333.33 Hz/V       //  R_SNSB: 0.003 Ohms
                                                                                                        //  QCOUNT_PRESCALE_FACTOR = 2 * (qLSB * K_QC * R_SNSB)
            this.deviceI2cUtils.i2cSet(i2cLabelsAddressesValues);

        } else {
            Log.e(logTag, "Skipping setOrResetSentinelPowerChip() because Sentinel Power Capture is not allowed or not possible.");
        }
    }

    private void resetI2cTmpValues() {
        resetI2cTmpValue("system");
        resetI2cTmpValue("battery");
        resetI2cTmpValue("input");
    }

    private void resetI2cTmpValue(String statAbbr) {
        											/*	voltage		current 	misc		power       captured_at     */
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
                String bPctStr = (bVals[2]+"").substring(0,(bVals[2]+"").length()-2)+"."+(bVals[2]+"").substring((bVals[2]+"").length()-2);
                logStr.append(" [ battery: ").append(bPctStr).append(" %, ").append(bVals[0]).append(" mV, ").append(bVals[1]).append(" mA, ").append(bVals[3]).append(" mW").append(" ]");
            }
        }
        double[] inpVals = this.i2cTmpValues.get("input");
        if (ArrayUtils.getAverageAsDouble(inpVals) != 0) {
            powerInputValues.add(new double[] { inpVals[0], inpVals[1], inpVals[2], inpVals[3], rightNow });
            if (verboseLogging) {
                long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(inpVals);
                logStr.append(" [ input: ").append(iVals[0]).append(" mV, ").append(iVals[1]).append(" mA, ").append(iVals[3]).append(" mW").append(" ]");
                logStr.append(" [ (").append(iVals[2]).append(") Charging: ").append(this.isBatteryCharging).append(", Charged: ").append(this.isBatteryCharged).append(" ]");
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

            for (String[] i2cLabeledOutput : this.deviceI2cUtils.i2cGet(buildI2cQueryList(), true, this.getWithoutTwoComplement)) {
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
                valueSet[4] = System.currentTimeMillis();
                this.i2cTmpValues.put(groupName, valueSet);
            }
            calculateMissingValues();
            cacheI2cTmpValues();

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private void calculateMissingValues() {

        double[] sysVals = this.i2cTmpValues.get("system");
        double[] battVals = this.i2cTmpValues.get("battery");
        double[] inpVals = this.i2cTmpValues.get("input");

        sysVals[3] = inpVals[3] - battVals[3];
        sysVals[1] = 1000 * sysVals[3] / sysVals[0];
        this.i2cTmpValues.put("system", sysVals);

        this.isInputPowerAtZero = !(Math.round(inpVals[3]) > 5);

        long chargerState = Math.round(inpVals[2]);
        this.isBatteryCharging = (chargerState == 64);
        this.isBatteryCharged = (chargerState == 8);

        checkSetQCountCalibration(Math.round(battVals[2]));

        battVals[2] = (10000*(battVals[2]-16384)/32768);
        this.i2cTmpValues.put("battery", battVals);
    }

    private void checkSetQCountCalibration(long qCountVal) {

        if (this.isBatteryCharged && !this.isBatteryCharging && (qCountVal != this.qCountCalibratedMax)) {

            Log.v(logTag, "Max Charge State Attained. Calibrating Coulomb Counter Maximum to "+this.qCountCalibratedMax+" (previously at "+qCountVal+")");

            if (isCaptureAllowed()) {
                List<String[]> i2cLabelsAddressesValues = new ArrayList<String[]>();
                i2cLabelsAddressesValues.add(new String[]{ "qcount", "0x13", "0x"+Long.toHexString(this.qCountCalibratedMax)});
                this.deviceI2cUtils.i2cSet(i2cLabelsAddressesValues);
            } else {
                Log.e(logTag, "Could not calibrate/reset qcount value");
            }
        }
    }

    private static double applyValueModifier(String i2cLabel, long i2cRawValue) {
        double modifiedValue = 0;

        // this is a test to see if this is how i2c handles negative values...
        //if (i2cRawValue > 32767) { i2cRawValue = i2cRawValue - 65535; }

        if (i2cLabel.equals("system-voltage")) {
            modifiedValue = i2cRawValue * 1.648;
        } else if (i2cLabel.equals("system-misc")) {
            modifiedValue = (i2cRawValue - 12010) / 45.6;

        } else if (i2cLabel.equals("battery-voltage")) {
            modifiedValue = i2cRawValue * 0.192264;
        } else if (i2cLabel.equals("battery-current")) {
            modifiedValue = i2cRawValue * (0.00146487 / 0.003); // hardcoded resistor value R[SNSB] = 0.003 ohms
        } else if (i2cLabel.equals("battery-misc")) {
            modifiedValue = i2cRawValue;

        } else if (i2cLabel.equals("input-voltage")) {
            modifiedValue = i2cRawValue * 1.648;
        } else if (i2cLabel.equals("input-current")) {
            modifiedValue = i2cRawValue * (0.00146487 / 0.005); // hardcoded resistor value R[SNSI] = 0.005 ohms
        } else if (i2cLabel.equals("input-misc")) {
            modifiedValue = i2cRawValue;


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
            long[] misc = new long[] { pVal("temp", sysVals[2]), pVal("percent", battVals[2]), pVal("misc", inpVals[2]) };
            long[] powers = new long[] { pVal("power", sysVals[3]), pVal("power", battVals[3]), pVal("power", inpVals[3]) };

            String bPctStr = (misc[1]+"").substring(0, (misc[1]+"").length()-2) +"."+ (misc[1]+"").substring((misc[1]+"").length()-2);

            app.sentinelPowerDb.dbSentinelPowerSystem.insert( measuredAt, voltages[0], currents[0], misc[0], powers[0] );
            app.sentinelPowerDb.dbSentinelPowerBattery.insert( measuredAt, voltages[1], currents[1], bPctStr, powers[1] );
            app.sentinelPowerDb.dbSentinelPowerInput.insert( measuredAt, voltages[2], currents[2], misc[2], powers[2] );

            if (printValuesToLog) {
                Log.d(logTag,
                    (new StringBuilder("Avg of ")).append(sampleCount).append(" samples for ").append(DateTimeUtils.getDateTime(measuredAt))//.append(":")
                    .append(" [ system: ").append(voltages[0]).append(" mV, ").append(currents[0]).append(" mA, ").append(powers[0]).append(" mW").append(" ]")
                    .append(" [ battery: ").append(bPctStr).append(" %, ").append(voltages[1]).append(" mV, ").append(currents[1]).append(" mA, ").append(powers[1]).append(" mW").append(" ]")
                    .append(" [ input: ").append(voltages[2]).append(" mV, ").append(currents[2]).append(" mA, ").append(powers[2]).append(" mW").append(" ]")
                    .append(" [ temp: ").append(misc[0]).append(" C").append(" ]")
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


    public JSONArray getMomentarySentinelPowerValuesAsJsonArray() {

        JSONArray powerJsonArray = new JSONArray();

        if ((this.powerBatteryValues.size() == 0) && isCaptureAllowed()) { updateSentinelPowerValues(); }

        long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
        long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerInputValues));
        long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerSystemValues));

        double measuredAtAvg = (sVals[4]+bVals[4]+iVals[4])/3;
        long measuredAt = Math.round(measuredAtAvg);

        try {
            JSONObject powerJson = new JSONObject();

            long bPct = pVal("percent", bVals[2]);
            String bPctStr = (bPct+"").substring(0, (bPct+"").length()-2) +"."+ (bPct+"").substring((bPct+"").length()-2);

            powerJson.put("system", "system*"+measuredAt
                                                    +"*"+pVal("voltage", sVals[0])
                                                    +"*"+pVal("current", sVals[1])
                                                    +"*"+pVal("temp", sVals[2])
                                                    +"*"+pVal("power", sVals[3]) );
            powerJson.put("battery", "battery*"+measuredAt
                                                    +"*"+pVal("voltage", bVals[0])
                                                    +"*"+pVal("current", bVals[1])
                                                    +"*"+bPctStr
                                                    +"*"+pVal("power", bVals[3]) );
            powerJson.put("input", "input*"+measuredAt
                                                    +"*"+pVal("voltage", iVals[0])
                                                    +"*"+pVal("current", iVals[1])
                                                    +"*"+pVal("misc", iVals[2])
                                                    +"*"+pVal("power", iVals[3]) );
            powerJsonArray.put(powerJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return powerJsonArray;
        }
    }

    public JSONObject getMomentarySentinelPowerValuesAsJson() {

        JSONObject jsonObj = new JSONObject();

        try {

            if ((this.powerBatteryValues.size() == 0) && isCaptureAllowed()) { updateSentinelPowerValues(); }

            long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
            JSONObject jsonBatteryObj = new JSONObject();
            jsonBatteryObj.put("percentage", Math.round(bVals[2]/100) );
            jsonBatteryObj.put("voltage", bVals[0]);
            jsonBatteryObj.put("current", bVals[1]);
            jsonBatteryObj.put("power", bVals[3]);
            jsonObj.put("battery", jsonBatteryObj);

            long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerInputValues));
            JSONObject jsonInputObj = new JSONObject();
            jsonInputObj.put("voltage", iVals[0]);
            jsonInputObj.put("current", iVals[1]);
            jsonInputObj.put("power", iVals[3]);
            jsonObj.put("input", jsonInputObj);

            long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerSystemValues));
            JSONObject jsonSystemObj = new JSONObject();
            jsonSystemObj.put("voltage", sVals[0]);
            jsonSystemObj.put("current", sVals[1]);
            jsonSystemObj.put("power", sVals[3]);
            jsonObj.put("system", jsonSystemObj);

        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);
        }

        return jsonObj;
    }

    public JSONObject sentinelPowerStatusAsJsonObj(String activityTag) {
        JSONObject statusObj = null;
        try {

            statusObj = new JSONObject();
            statusObj.put("is_allowed", !isReducedCaptureModeActive_BasedOnSentinelPower(activityTag) );
            statusObj.put("is_blocked", false);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return statusObj;
    }

    public boolean isReducedCaptureModeActive_BasedOnSentinelPower(String activityTag) {

        boolean isAllowed = !app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_sentinel_battery");

        if (!isAllowed) {

            if (    (this.powerBatteryValues.size() == 0)
               &&   isCaptureAllowed()
            ) {
                updateSentinelPowerValues();
            }

            if (this.powerBatteryValues.size() > 0) {
                long battPct = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getMinimumValuesAsArrayFromArrayList(this.powerBatteryValues))[2];
                int prefsVal = activityTag.equalsIgnoreCase("audio_capture") ? app.rfcxPrefs.getPrefAsInt("audio_cutoff_sentinel_battery") : app.rfcxPrefs.getPrefAsInt("checkin_cutoff_sentinel_battery");
                isAllowed = battPct >= (prefsVal * 100);
            }
        }

        return !isAllowed;
    }

    private static long pVal(String fieldName, long val) {

        double divVal = Double.parseDouble(""+val);

        if (fieldName.equalsIgnoreCase("voltage")) {
            if (val < 2000) {
                divVal = Math.round(divVal / 100) * 100;
            }
        } else if (fieldName.equalsIgnoreCase("current")) {
            if ((val < 100) && (val > -100)) {
                divVal = Math.round(divVal / 10) * 10;
            }
        }

        return Math.round(divVal);
    }


}
