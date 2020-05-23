package org.rfcx.guardian.admin.device.sentinel;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceI2cUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentinelEnvironmentUtils {

    public SentinelEnvironmentUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.deviceI2cUtils = new DeviceI2cUtils(sentinelEnvironmentI2cMainAddress);
        initSentinelEnvironmentI2cOptions();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelEnvironmentUtils");

    RfcxGuardian app;
    private DeviceI2cUtils deviceI2cUtils = null;
    private static final String sentinelEnvironmentI2cMainAddress = "0x40";

    private String[] i2cValueIndex = new String[]{};
    private Map<String, double[]> i2cTmpValues = new HashMap<>();
    private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();

    private List<double[]> envEnclosureValues = new ArrayList<>();

    private boolean verboseLogging = false;

    private void initSentinelEnvironmentI2cOptions() {

        this.i2cValueIndex = new String[]{ "temperature", "humidity" };
        //										        temperature     humidity
        this.i2cAddresses.put("enclosure", new String[]{   null,     null  });

        resetI2cTempValues();
    }

    public void setOrResetSentinelEnvironmentChip() {

//        if (isCaptureAllowed()) {
//
//            List<String[]> i2cLabelsAddressesValues = new ArrayList<String[]>();
//            i2cLabelsAddressesValues.add(new String[]{"force_meas_sys_on", "0x14", "0x0011"});
//            this.deviceI2cUtils.i2cSet(i2cLabelsAddressesValues);
//
//        } else {
//            Log.e(logTag, "Skipping setOrResetSentinelEnvironmentChip() because Sentinel capture is not allowed or not possible.");
//        }
    }

    private void resetI2cTempValues() {
        resetI2cTmpValue("enclosure");
    }

    private void resetI2cTmpValue(String statAbbr) {
        /*	                                        temperature		humidity       captured_at     */
        this.i2cTmpValues.put(statAbbr, new double[]{     0,          0,          0           });
    }




    public static JSONArray getSentinelEnvironmentValuesAsJsonArray(Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();
        try {
            JSONObject sensorJson = new JSONObject();
            sensorJson.put("enclosure", app.sentinelEnvironmentDb.dbSentinelEnvironmentEnclosure.getConcatRowsWithLabelPrepended("enclosure"));
            sensorJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return sensorJsonArray;
        }
    }

    public static int deleteSentinelEnvironmentValuesBeforeTimestamp(String timeStamp, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));

        app.sentinelEnvironmentDb.dbSentinelEnvironmentEnclosure.clearRowsBefore(clearBefore);

        return 1;
    }


}
