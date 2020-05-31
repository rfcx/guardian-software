package org.rfcx.guardian.admin.device.sentinel;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceI2cUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentinelCompassUtils {

    public SentinelCompassUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.deviceI2cUtils = new DeviceI2cUtils(sentinelCompassI2cMainAddress);
        initSentinelCompassI2cOptions();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelCompassUtils");

    public static final long samplesTakenPerCaptureCycle = 4;

    RfcxGuardian app;
    private DeviceI2cUtils deviceI2cUtils = null;
    private static final String sentinelCompassI2cMainAddress = "0x1e";

    private String[] i2cValueIndex = new String[]{};
    private Map<String, double[]> i2cTmpValues = new HashMap<>();
    private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();

    private List<double[]> compassValues = new ArrayList<>();

    private boolean verboseLogging = false;

    private void initSentinelCompassI2cOptions() {

        this.i2cValueIndex = new String[]{             "x",        "y",        "z",        "temp"  };
        this.i2cAddresses.put("compass", new String[]{   "0x68",     "0x6a",     "0x6c",     null  });

        resetI2cTmpValues();
    }

    public boolean isCaptureAllowed() {

        // Only allow compass measurement when there is no input current.
        // It seems that the magnetic field from an input charging current disrupts compass measurements.
        boolean isInputPowerAtZero = app.sentinelPowerUtils.isInputPowerAtZero;

        boolean isNotExplicitlyDisabled = app.rfcxPrefs.getPrefAsBoolean("admin_enable_sentinel_capture");
        boolean isI2cHandlerAccessible = false;
        boolean isI2cCompassChipConnected = false;

        if (isNotExplicitlyDisabled && isInputPowerAtZero) {
            isI2cHandlerAccessible = (new File("/dev/i2c-" + DeviceI2cUtils.i2cInterface)).canRead();
            if (isI2cHandlerAccessible) {
                String i2cConnectAttempt = this.deviceI2cUtils.i2cGetAsString("0x4f", true);
                isI2cCompassChipConnected = ((i2cConnectAttempt != null) && (Math.abs(DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)) > 0));
            }
        }
        return isNotExplicitlyDisabled && isInputPowerAtZero && isI2cHandlerAccessible && isI2cCompassChipConnected;
    }

    public void setOrResetSentinelCompassChip() {

        if (isCaptureAllowed()) {

            List<String[]> i2cLabelsAddressesValues = new ArrayList<String[]>();
            i2cLabelsAddressesValues.add(new String[]{"cfg_reg_a", "0x60", "0x0080"});
            i2cLabelsAddressesValues.add(new String[]{"cfg_reg_c", "0x62", "0x0001"});
            this.deviceI2cUtils.i2cSet(i2cLabelsAddressesValues);

        } else {
            Log.e(logTag, "Skipping setOrResetSentinelCompassChip() because Sentinel capture is not allowed or not possible.");
        }
    }

    private void resetI2cTmpValues() {
        resetI2cTmpValue("compass");
    }

    private void resetI2cTmpValue(String statAbbr) {
        /*	                                              x		      y 	      z		      temp      captured_at     */
        this.i2cTmpValues.put(statAbbr, new double[]{     0,          0,          0,          0,        0           });
    }

    private void cacheI2cTmpValues() {
        StringBuilder logStr = new StringBuilder();
        long rightNow = System.currentTimeMillis();

        double[] cmpVals = this.i2cTmpValues.get("compass");
        if (ArrayUtils.getAverageAsDouble(cmpVals) != 0) {
            compassValues.add(new double[] { cmpVals[0], cmpVals[1], cmpVals[2], cmpVals[3], rightNow });
            if (verboseLogging) {
                long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(cmpVals);
          //      logStr.append("[ temp: ").append(sVals[3]).append(" C").append(" ] ");
                logStr.append("[ compass: x ").append(sVals[0]).append(", y ").append(sVals[1]).append(", z ").append(sVals[2]).append(" ]");
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

    public void updateSentinelCompassValues() {
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
                this.i2cTmpValues.put(groupName, valueSet);
            }
            cacheI2cTmpValues();

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private static double applyValueModifier(String i2cLabel, long i2cRawValue) {
        double modifiedValue = 0;

        if (i2cLabel.equals("compass-x") || i2cLabel.equals("compass-y") || i2cLabel.equals("compass-z")) {
            modifiedValue = i2cRawValue;//*0.15;//976.5625*i2cRawValue/16384;

        } else if (i2cLabel.equals("compass-temp")) {
            modifiedValue = i2cRawValue;//+23;

        } else {
            Log.d(logTag, "No known value modifier for i2c label '" + i2cLabel + "'.");
        }
        return modifiedValue;
    }

    public void saveSentinelCompassValuesToDatabase(boolean printValuesToLog) {

        int sampleCount = this.compassValues.size();

        if (sampleCount > 0) {

            StringBuilder logStr = (new StringBuilder("Average of ")).append(sampleCount).append(" samples");

            long[] cmpVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.compassValues));
            this.compassValues = new ArrayList<>();
            app.sentinelSensorDb.dbCompass.insert(cmpVals[4], cmpVals[0]+"", cmpVals[1]+"", cmpVals[2]+"", cmpVals[3]+"");
    //        logStr.append(" [ temp: ").append(cmpVals[3]).append(" C").append(" ]");
            logStr.append(" [ compass: x ").append(cmpVals[0]).append(", y ").append(cmpVals[1]).append(", z ").append(cmpVals[2]).append(" ]");

            if (printValuesToLog) {
                Log.d(logTag, logStr.toString());
            }
        }
    }







}
