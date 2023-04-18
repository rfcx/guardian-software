package org.rfcx.guardian.admin.device.i2c.sentinel;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.i2c.DeviceI2cUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentinelPowerUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelPowerUtils");
    private static final String i2cMainAddr = "0x68";
    private static final double qCountCalibrationVoltageMin = 3000;
    private static final double qCountMeasurementRange = 65535;
    private static final double qCountCalibratedMin = Math.round(qCountMeasurementRange / 4);
    private static final double qCountCalibratedMax = Math.round(qCountMeasurementRange - qCountCalibratedMin);
    private static final double qCountCalibratedQuarterOfOnePercent = (qCountCalibratedMax - qCountCalibratedMin) / (4 * 100);
    private static final int qCountCalibrationDelayCounterMax = 20;
    private static final long reCheckSetConfigAfterThisLong = 5 * 60 * 1000;
    public boolean verboseLogging = false;
    public boolean isInputPowerAtZero = false;
    RfcxGuardian app;
    private String[] i2cValueIndex = new String[]{};
    private final Map<String, double[]> i2cTmpValues = new HashMap<>();
    private final Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();
    private String[] getWithoutTwoComplement = new String[]{};
    private List<double[]> powerBatteryValues = new ArrayList<>();
    private List<double[]> powerInputValues = new ArrayList<>();
    private List<double[]> powerSystemValues = new ArrayList<>();
    private int qCountCalibrationDelayCounter = qCountCalibrationDelayCounterMax;
    private boolean isBatteryCharging = false;
    private boolean isBatteryCharged = false;
    private final boolean isBatteryChargingAllowed = true;

    private String chipAccessibleFailMessage = null;
    private long chipConfigLastCheckedAt = 0;

    private double previousBatteryVoltage = 0;
    private double previousQCountValue = 0;
    private boolean isCalibrationNeeded = false;

    private long lastSavingLog = 0;

    public SentinelPowerUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        initSentinelPowerI2cOptions();
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
        } else if (i2cLabel.equals("battery-temp")) {
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

    private static long _v(String fieldName, long val) {

        double divVal = Double.parseDouble("" + val);

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

    private static String battValAsPctStr(long bValAsLong) {
        String bValsStr = (bValAsLong >= 100) ? ("" + Math.abs(bValAsLong)) : (bValAsLong >= 10) ? ("0" + Math.abs(bValAsLong)) : ("00" + Math.abs(bValAsLong));
        return ((bValAsLong >= 0) ? "" : "-") + bValsStr.substring(0, bValsStr.length() - 2) + "." + bValsStr.substring(bValsStr.length() - 2);
    }

    public boolean isChipAccessibleByI2c() {

        boolean isNotExplicitlyDisabled = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SENTINEL_POWER);
        boolean isI2cHandlerAccessible = false;
        boolean isI2cPowerChipConnected = false;

        if (isNotExplicitlyDisabled) {
            isI2cHandlerAccessible = app.deviceI2cUtils.isI2cHandlerAccessible();
            if (!isI2cHandlerAccessible) {
                chipAccessibleFailMessage = "Sentinel Power Chip could not be accessed because I2C handler is not accessible...";
                Log.e(logTag, chipAccessibleFailMessage);
            } else {
                String i2cConnectAttempt = app.deviceI2cUtils.i2cGetAsString("0x4a", i2cMainAddr, true, true);
                isI2cPowerChipConnected = ((i2cConnectAttempt != null) && (Math.abs(DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)) > 0));
                if (!isI2cPowerChipConnected) {
                    chipAccessibleFailMessage = "Sentinel Power Chip is NOT Accessible via I2C...";
                    Log.e(logTag, chipAccessibleFailMessage);
                } else {
                    chipAccessibleFailMessage = null;
                }
            }
        }
        return isNotExplicitlyDisabled && isI2cHandlerAccessible && isI2cPowerChipConnected;
    }

    private void initSentinelPowerI2cOptions() {

        this.i2cValueIndex = new String[]{"voltage", "current", "misc", "power", "temp"};

        this.i2cAddresses.put("system", new String[]{"0x3c", null, "0x3f" /* die temperature */, null, null});
        this.i2cAddresses.put("battery", new String[]{"0x3a", "0x3d", "0x13" /* coulomb_counter */, null, "0x40" /* NTC Ratio */});
        this.i2cAddresses.put("input", new String[]{"0x3b", "0x3e", "0x34" /* charger_state */, null, null});

        this.getWithoutTwoComplement = new String[]{"battery-misc", "input-misc", "battery-temp"};

        resetI2cTmpValues();
    }

    public boolean checkSetChipConfigByI2c() {

        boolean isChipAccessible = isChipAccessibleByI2c();

        if (isChipAccessible) {

            if (Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(chipConfigLastCheckedAt)) >= reCheckSetConfigAfterThisLong) {

                Map<String, String[]> chipConfig = new HashMap<String, String[]>();

                chipConfig.put("config_bits", new String[]{"0x14", (this.isBatteryChargingAllowed) ? "0x001c" : "0x011c"});
                // 000011100 (binary)  // set bits 2, 3, 4 to "1" (include bit 8 at "1" to suspend charger)
                // 000011100 (binary)  // set bits 2, 3, 4 to "1"
                // 'en_qcount' (bit 2) enabled
                // 'mppt_en_i2c' (bit 3) enabled
                // 'force_meas_sys_on' (bit 4) enabled
                // 'run_bsr' (bit 5) disabled
                // 'suspend_charger' (bit 8) disabled

                chipConfig.put("charger_config_bits", new String[]{"0x29", "0x0004"});
//                chipConfig.put("charger_config_bits", new String[]{"0x29", "0x0005"}); // enable JEITA
                // 0100 (binary)  // set bit 2 to "1"
                // 'en_c_over_x_term' (bit 2) enabled
                // 'en_lead_acid_temp_comp' (bit 1) disabled
                // 'en_jeita' (bit 0) disabled

                chipConfig.put("set_max_jeita_t6", new String[]{"0x24", "0x0eb4"}); // 65 C thermal resistor

                chipConfig.put("max_cv_time", new String[]{"0x1d", "0x0000"});       // setting value (in seconds) to zero to disable charging timeout
                chipConfig.put("max_charge_time", new String[]{"0x1e", "0x0000"});   // setting value (in seconds) to zero to disable charging timeout
                chipConfig.put("max_absorb_time", new String[]{"0x2b", "0x0000"});   // setting value (in seconds) to zero to disable charging timeout

                chipConfig.put("qcount_prescale_factor", new String[]{"0x12", "0x002d"});
                // 45 (decimal) is QCOUNT_PRESCALE_FACTOR
                // LiFePO4 Battery Capacity: 16.5 Ah * 3600 sec = 59400C
                // qLSB (max) = 59400C / 65535 =  0.906
                //  K_QC: 8333.33 Hz/V
                //  R_SNSB: 0.003 Ohms
                //  QCOUNT_PRESCALE_FACTOR = 2 * (qLSB * K_QC * R_SNSB)


                List<String[]> chipConfigI2cLabelsAndSubAddresses = new ArrayList<>();

                // Get config values over I2c

                for (String configLabel : chipConfig.keySet()) {
                    if (chipConfig.get(configLabel)[0] != null) {
                        chipConfigI2cLabelsAndSubAddresses.add(new String[]{configLabel, chipConfig.get(configLabel)[0]});
                    }
                }

                List<String[]> i2cSetConfigLabelsAddressesValues = new ArrayList<>();

                for (String[] i2cLabeledOutput : app.deviceI2cUtils.i2cGet(chipConfigI2cLabelsAndSubAddresses, i2cMainAddr, false, true, new String[]{})) {

                    String outputValue = "0x" + StringUtils.leftPadStringWithChar(i2cLabeledOutput[1].substring(2), 4, "0");

                    if (!chipConfig.get(i2cLabeledOutput[0])[1].equalsIgnoreCase(outputValue)) {
                        Log.v(logTag, "I2C Config Queued: " + i2cLabeledOutput[0] + " - " + outputValue);
                        i2cSetConfigLabelsAddressesValues.add(new String[]{i2cLabeledOutput[0], chipConfig.get(i2cLabeledOutput[0])[0], chipConfig.get(i2cLabeledOutput[0])[1]});
                    }
                }


                if (i2cSetConfigLabelsAddressesValues.size() == 0) {
                    Log.i(logTag, "Sentinel Power I2C Configuration verified.");
                } else if (!app.deviceI2cUtils.i2cSet(i2cSetConfigLabelsAddressesValues, i2cMainAddr, true)) {
                    Log.e(logTag, "Sentinel Power Chip configuration attempted and failed to be set over I2C.");
                } else {
                    Log.v(logTag, "Sentinel Power I2C Configuration was successfully updated.");
                }

                chipConfigLastCheckedAt = System.currentTimeMillis();
            }

        } else {
            Log.e(logTag, "Sentinel Power Chip is not accessible. Configuration could not be verified over I2C.");
            chipConfigLastCheckedAt = 0;
        }

        return isChipAccessible;
    }

    private void resetI2cTmpValues() {
        resetI2cTmpValue("system");
        resetI2cTmpValue("battery");
        resetI2cTmpValue("input");
    }

    private void resetI2cTmpValue(String statAbbr) {
        /*	voltage		current 	misc		power       temp       captured_at     */
        this.i2cTmpValues.put(statAbbr, new double[]{0, 0, 0, 0, 0, 0});
    }

    private void cacheI2cTmpValues() {
        StringBuilder logStr = new StringBuilder();
        long rightNow = System.currentTimeMillis();

        double[] sysVals = this.i2cTmpValues.get("system");
        if (ArrayUtils.getAverageAsDouble(sysVals) != 0) {
            powerSystemValues.add(new double[]{sysVals[0], sysVals[1], sysVals[2], sysVals[3], sysVals[4], rightNow});
            if (verboseLogging) {
                long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(sysVals);
                logStr.append("[ temp LTC Chip: ").append(sVals[2]).append(" C").append(" ]");
                logStr.append(" [ system: ").append(sVals[0]).append(" mV, ").append(sVals[1]).append(" mA, ").append(sVals[3]).append(" mW").append(" ]");
            }
        }
        double[] battVals = this.i2cTmpValues.get("battery");
        if (ArrayUtils.getAverageAsDouble(battVals) != 0) {
            powerBatteryValues.add(new double[]{battVals[0], battVals[1], battVals[2], battVals[3], battVals[4], rightNow});
            if (verboseLogging) {
                long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(battVals);
                double ntcVal = (10000 * (bVals[4]/((double)(21845 - bVals[4]))));
                logStr.append("[ temp NTC: ").append(ntcVal).append(" ]").append(" original: ").append(bVals[4]);
                logStr.append(" [ battery: ").append(battValAsPctStr(bVals[2])).append(" %, ").append(bVals[0]).append(" mV, ").append(bVals[1]).append(" mA, ").append(bVals[3]).append(" mW").append(" ]");
            }
        }
        double[] inpVals = this.i2cTmpValues.get("input");
        if (ArrayUtils.getAverageAsDouble(inpVals) != 0) {
            powerInputValues.add(new double[]{inpVals[0], inpVals[1], inpVals[2], inpVals[3], inpVals[4], rightNow});
            if (verboseLogging) {
                long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(inpVals);
                logStr.append(" [ input: ").append(iVals[0]).append(" mV, ").append(iVals[1]).append(" mA, ").append(iVals[3]).append(" mW").append(" ]");
                logStr.append(" [ (").append(iVals[2]).append(") Charging: ").append(this.isBatteryCharging).append(", Charged: ").append(this.isBatteryCharged).append(" ]");
            }
        }
        commandToLog(logStr.toString());
        if (verboseLogging) {
            Log.d(logTag, logStr.toString());
        }
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

            for (String[] i2cLabeledOutput : app.deviceI2cUtils.i2cGet(buildI2cQueryList(), i2cMainAddr, true, true, this.getWithoutTwoComplement)) {
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
                valueSet[5] = System.currentTimeMillis();
                this.i2cTmpValues.put(groupName, valueSet);
            }

            if (calculateMissingValuesAndValidateResults()) {
                cacheI2cTmpValues();
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private boolean calculateMissingValuesAndValidateResults() {

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

        battVals[2] = checkSetQCountCalibration(battVals[2], battVals[0]);

        battVals[2] = (10000 * (battVals[2] - qCountCalibratedMin) / 32768);
        this.i2cTmpValues.put("battery", battVals);

        return (battVals[0] >= qCountCalibrationVoltageMin) || ((battVals[0] >= (qCountCalibrationVoltageMin / 2)) && (battVals[2] < 1000));
    }

    private double checkSetQCountCalibration(double qCountVal, double voltageVal) {

        boolean doReCalibration = false;
        String calibrationMsg = null;

        if (((qCountVal - qCountCalibratedQuarterOfOnePercent) > qCountCalibratedMax)
                || (this.isBatteryCharged && !this.isBatteryCharging && (qCountVal != qCountCalibratedMax))
        ) {

            calibrationMsg = "Max Charge State Attained. Calibrating Coulomb Counter Maximum (100%) to " + Math.round(qCountCalibratedMax) + " (previously at " + Math.round(qCountVal) + ")";
            qCountVal = qCountCalibratedMax;
            doReCalibration = true;
            this.qCountCalibrationDelayCounter = qCountCalibrationDelayCounterMax;

        } else if ((qCountVal + qCountCalibratedQuarterOfOnePercent) < qCountCalibratedMin) {

            Log.e(logTag, "Battery Voltage considered extremely low at " + Math.round(qCountVal) + " Coulomb Counter. Countdown to Coulomb Counter reset: " + this.qCountCalibrationDelayCounter + "/" + qCountCalibrationDelayCounterMax);

            this.qCountCalibrationDelayCounter--;
            commandToLog("low qCount", qCountVal + "", this.qCountCalibrationDelayCounter);
            if (qCountCalibrationDelayCounter <= 0) {
                calibrationMsg = "Min Charge State Attained. Calibrating Coulomb Counter Minimum (0%) to " + Math.round(qCountCalibratedMin) + " (previously at " + Math.round(qCountVal) + ")";
                qCountVal = qCountCalibratedMin;
                doReCalibration = true;
                this.qCountCalibrationDelayCounter = qCountCalibrationDelayCounterMax;
            }

        } else if ((voltageVal <= qCountCalibrationVoltageMin) && (voltageVal >= (qCountCalibrationVoltageMin) / 2)) {
//            if (verboseLogging) {
            Log.e(logTag, "Battery Voltage considered extremely low at " + Math.round(voltageVal) + " mV. Countdown to Coulomb Counter reset: " + this.qCountCalibrationDelayCounter + "/" + qCountCalibrationDelayCounterMax);
            //  }

            this.qCountCalibrationDelayCounter--;
            commandToLog("low battery voltage", voltageVal + " / " + qCountVal, this.qCountCalibrationDelayCounter);
            if (qCountCalibrationDelayCounter <= 0) {
                calibrationMsg = "Battery is effectively fully discharged (Voltage: " + Math.round(voltageVal) + " mV). Setting Coulomb Counter to " + Math.round(qCountCalibratedMin) + " (0%)";
                qCountVal = qCountCalibratedMin;
                doReCalibration = true;
                this.qCountCalibrationDelayCounter = qCountCalibrationDelayCounterMax;
            }

        } else {
            this.qCountCalibrationDelayCounter = qCountCalibrationDelayCounterMax;
        }

        if (doReCalibration) {
            if (isChipAccessibleByI2c()) {
                List<String[]> i2cLabelsAddressesValues = new ArrayList<String[]>();
                i2cLabelsAddressesValues.add(new String[]{"qcount", "0x13", "0x" + Long.toHexString(Long.parseLong("" + Math.round(qCountVal)))});
                app.deviceI2cUtils.i2cSet(i2cLabelsAddressesValues, i2cMainAddr, true);
                Log.v(logTag, calibrationMsg);
            } else {
                Log.e(logTag, "Failed to Set/Calibrate QCount value via I2C...");
            }
        }

        return qCountVal;
    }

    //TODO: Save NTC values
    public void saveSentinelPowerValuesToDatabase(boolean printValuesToLog) {

        int sampleCount = Math.round((this.powerSystemValues.size() + this.powerBatteryValues.size() + this.powerInputValues.size()) / 3);

        if (sampleCount > 0) {

            long[] sysVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerSystemValues));
            this.powerSystemValues = new ArrayList<>();
            long[] battVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
            this.powerBatteryValues = new ArrayList<>();
            long[] inpVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerInputValues));
            this.powerInputValues = new ArrayList<>();

            double measuredAtAvg = (sysVals[5] + battVals[5] + inpVals[5]) / 3;
            long measuredAt = Math.round(measuredAtAvg);

            long[] voltages = new long[]{_v("voltage", sysVals[0]), _v("voltage", battVals[0]), _v("voltage", inpVals[0])};
            long[] currents = new long[]{_v("current", sysVals[1]), _v("current", battVals[1]), _v("current", inpVals[1])};
            long[] misc = new long[]{_v("temp", sysVals[2]), _v("percent", battVals[2]), _v("misc", inpVals[2])};
            long[] powers = new long[]{_v("power", sysVals[3]), _v("power", battVals[3]), _v("power", inpVals[3])};

            String bPctStr = battValAsPctStr(misc[1]);

            app.sentinelPowerDb.dbSentinelPowerSystem.insert(measuredAt, voltages[0], currents[0], misc[0], powers[0]);
            app.sentinelPowerDb.dbSentinelPowerBattery.insert(measuredAt, voltages[1], currents[1], bPctStr, powers[1]);
            app.sentinelPowerDb.dbSentinelPowerInput.insert(measuredAt, voltages[2], currents[2], misc[2], powers[2]);

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

    public JSONObject getI2cAccessibilityAndFailMessage() {
        JSONObject i2c = new JSONObject();
        try {
            if (chipAccessibleFailMessage == null) {
                i2c.put("is_accessible", true);
            } else {
                i2c.put("is_accessible", false);
                i2c.put("message", chipAccessibleFailMessage);
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return i2c;
    }

    public JSONArray getMomentarySentinelPowerValuesAsJsonArray(boolean resetValues) {

        JSONArray powerJsonArray = new JSONArray();

        if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SENTINEL_POWER)) {

            if (resetValues) {
                this.powerSystemValues = new ArrayList<>();
                this.powerBatteryValues = new ArrayList<>();
                this.powerInputValues = new ArrayList<>();
            }

            if ((this.powerBatteryValues.size() == 0) && isChipAccessibleByI2c()) {
                updateSentinelPowerValues();
            }

            if (this.powerBatteryValues.size() > 0) {

                long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
                long[] iVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerInputValues));
                long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerSystemValues));

                double measuredAtAvg = (sVals[5] + bVals[5] + iVals[5]) / 3;
                long measuredAt = Math.round(measuredAtAvg);

                try {
                    JSONObject powerJson = new JSONObject();

                    powerJson.put("system", "system*" + measuredAt
                            + "*" + _v("voltage", sVals[0])
                            + "*" + _v("current", sVals[1])
                            + "*" + _v("temp", sVals[2])
                            + "*" + _v("power", sVals[3]));
                    powerJson.put("battery", "battery*" + measuredAt
                            + "*" + _v("voltage", bVals[0])
                            + "*" + _v("current", bVals[1])
                            + "*" + battValAsPctStr(_v("percent", bVals[2]))
                            + "*" + _v("power", bVals[3]));
                    powerJson.put("input", "input*" + measuredAt
                            + "*" + _v("voltage", iVals[0])
                            + "*" + _v("current", iVals[1])
                            + "*" + _v("misc", iVals[2])
                            + "*" + _v("power", iVals[3]));
                    powerJsonArray.put(powerJson);

                } catch (Exception e) {
                    RfcxLog.logExc(logTag, e);

                }
            }

        }

        return powerJsonArray;

    }

    public JSONObject getMomentarySentinelPowerValuesAsJson() {

        JSONObject jsonObj = new JSONObject();

        if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SENTINEL_POWER)) {

            try {

                if ((this.powerBatteryValues.size() == 0) && isChipAccessibleByI2c()) {
                    updateSentinelPowerValues();
                }

                long[] bVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.powerBatteryValues));
                JSONObject jsonBatteryObj = new JSONObject();
                jsonBatteryObj.put("percentage", Math.round(bVals[2] / 100));
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
        }

        return jsonObj;
    }

    public boolean isReducedCaptureModeActive_BasedOnSentinelPower(String groupTag) {

        boolean isReduced = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SENTINEL_BATTERY);

        if (isReduced) {

            if ((this.powerBatteryValues.size() == 0) && isChipAccessibleByI2c()) {
                updateSentinelPowerValues();
            }

            if (this.powerBatteryValues.size() > 0) {

                long battPct = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getMinimumValuesAsArrayFromArrayList(this.powerBatteryValues))[2];
                int prefsVal = groupTag.equalsIgnoreCase(RfcxStatus.Group.AUDIO_CAPTURE) ? app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CUTOFF_SENTINEL_BATTERY) : app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_CUTOFF_SENTINEL_BATTERY);
                isReduced = !(battPct >= (prefsVal * 100));

            } else /*if (!isChipAccessibleByI2c())*/ {
                isReduced = isChipAccessibleByI2c();

            }
        }

        return isReduced;
    }

    private void commandToLog(String type, String value, int count) {
        File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/I2CLog.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(type).append(" : ").append(value).append(" count: ").append(String.valueOf(count)).append(" --- ").append(String.valueOf(new Date()));
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void commandToLog(String log) {
        long now = System.currentTimeMillis();
        if (now - lastSavingLog > 60000) {
            File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/BatteryLog.txt");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(String.valueOf(new Date())).append("-----").append(log);
                buf.newLine();
                buf.close();
                lastSavingLog = now;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
