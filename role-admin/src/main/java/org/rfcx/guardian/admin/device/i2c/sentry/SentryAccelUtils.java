package org.rfcx.guardian.admin.device.i2c.sentry;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.i2c.DeviceI2cUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentryAccelUtils {

    public SentryAccelUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        initSentryAccelI2cOptions();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentryAccelUtils");

    public static final long samplesTakenPerCaptureCycle = 1;

    RfcxGuardian app;
    private static final String i2cMainAddr = "0x18";

    private String[] i2cValueIndex = new String[]{};
    private Map<String, double[]> i2cTmpValues = new HashMap<>();
    private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();

    private List<double[]> accelValues = new ArrayList<>();

    public boolean verboseLogging = false;

    private void initSentryAccelI2cOptions() {

        this.i2cValueIndex = new String[]{             "x",        "y",        "z",        "temp"     };
        this.i2cAddresses.put("accel", new String[]{   "0x04",     "0x06",      "0x02",     null  });

        resetI2cTmpValues();

    }

    public boolean isChipAccessibleByI2c() {

        boolean isNotExplicitlyDisabled = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SENTRY_SENSOR);
        boolean isI2cHandlerAccessible = false;
        boolean isI2cAccelChipConnected = false;

        if (isNotExplicitlyDisabled) {
            isI2cHandlerAccessible = app.deviceI2cUtils.isI2cHandlerAccessible();
            if (isI2cHandlerAccessible) {
                String i2cConnectAttempt = app.deviceI2cUtils.i2cGetAsString("0x00", i2cMainAddr, true);
                isI2cAccelChipConnected = ((i2cConnectAttempt != null) && (Math.abs(DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)) > 0));
                if (!isI2cAccelChipConnected) { Log.e(logTag, "Sentry Accelerometer Chip is NOT Accessible via I2C..."); }
            }
        }
        return isNotExplicitlyDisabled && isI2cHandlerAccessible && isI2cAccelChipConnected;
    }

    private void resetI2cTmpValues() {
        resetI2cTmpValue("accel");
    }

    private void resetI2cTmpValue(String statAbbr) {
        /*	                                              x		      y 	      z		      temp      captured_at     */
        this.i2cTmpValues.put(statAbbr, new double[]{     0,          0,          0,          0,        0           });
    }

    private void cacheI2cTmpValues() {
        StringBuilder logStr = new StringBuilder();
        long rightNow = System.currentTimeMillis();

        double[] accVals = this.i2cTmpValues.get("accel");
        if (ArrayUtils.getAverageAsDouble(accVals) != 0) {
            accelValues.add(new double[] { accVals[0], accVals[1], accVals[2], accVals[3], rightNow });
            if (verboseLogging) {
                long[] sVals = ArrayUtils.roundArrayValuesAndCastToLong(accVals);
        //        logStr.append("[ temp: ").append(sVals[3]).append(" C").append(" ] ");
                logStr.append("[ accelerometer: x ").append(sVals[0]).append(", y ").append(sVals[1]).append(", z ").append(sVals[2]).append(" ]");
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

    public void updateSentryAccelValues() {
        try {

            resetI2cTmpValues();

            for (String[] i2cLabeledOutput : app.deviceI2cUtils.i2cGet(buildI2cQueryList(), i2cMainAddr, true)) {
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
                valueSet[4] = System.currentTimeMillis();
                this.i2cTmpValues.put(groupName, valueSet);
            }
            cacheI2cTmpValues();

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private static double applyValueModifier(String i2cLabel, long i2cRawValue) {
        double modifiedValue = 0;

        if (i2cLabel.equals("accel-x") || i2cLabel.equals("accel-y") || i2cLabel.equals("accel-z")) {
            modifiedValue = 976.5625*i2cRawValue/16384;

        } else if (i2cLabel.equals("accel-temp")) {
            modifiedValue = i2cRawValue+23;

        } else {
            Log.d(logTag, "No known value modifier for i2c label '" + i2cLabel + "'.");
        }
        return modifiedValue;
    }

    public void saveSentryAccelValuesToDatabase(boolean printValuesToLog) {

        int sampleCount = this.accelValues.size();

        if (sampleCount > 0) {

            long[] accVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(this.accelValues));
            this.accelValues = new ArrayList<>();
            app.sentrySensorDb.dbAccelerometer.insert(accVals[4], accVals[0]+"", accVals[1]+"", accVals[2]+"", accVals[3]+"");

            if (printValuesToLog) {
                Log.d(logTag,
                    (new StringBuilder("Avg of ")).append(sampleCount).append(" samples for ").append(DateTimeUtils.getDateTime(accVals[4]))//.append(":")
                    .append(" [ accelerometer: x ").append(accVals[0]).append(", y ").append(accVals[1]).append(", z ").append(accVals[2]).append(" ]")
                    //.append(" [ temp: ").append(accVals[3]).append(" C").append(" ]")
                    .toString());
            }
        }
    }


    public List<double[]> getAccelValues() {
        return this.accelValues;
    }

    public void resetAccelValues() {
        this.accelValues = new ArrayList<>();
    }

}
