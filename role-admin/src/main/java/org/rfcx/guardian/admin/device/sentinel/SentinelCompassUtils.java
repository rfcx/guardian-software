package org.rfcx.guardian.admin.device.sentinel;

import android.content.Context;

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

public class SentinelCompassUtils {

    public SentinelCompassUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.deviceI2cUtils = new DeviceI2cUtils(sentinelCompassI2cMainAddress);
        initSentinelCompassI2cOptions();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelCompassUtils");

    RfcxGuardian app;
    private DeviceI2cUtils deviceI2cUtils = null;
    private static final String sentinelCompassI2cMainAddress = "0x1e";

    private String[] i2cValueIndex = new String[]{};
    private Map<String, double[]> i2cTmpValues = new HashMap<>();
    private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();

    private List<double[]> compassValues = new ArrayList<>();

    private boolean verboseLogging = false;

    private void initSentinelCompassI2cOptions() {

        this.i2cValueIndex = new String[]{ "temperature", "humidity" };
        //										        temperature     humidity
        this.i2cAddresses.put("compass", new String[]{   null,     null  });

        resetI2cTmpValues();
    }

    public void setOrResetSentinelCompassChip() {

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

    private void resetI2cTmpValues() {
        resetI2cTmpValue("compass");
    }

    private void resetI2cTmpValue(String statAbbr) {
        /*	                                        temperature		humidity       captured_at     */
        this.i2cTmpValues.put(statAbbr, new double[]{     0,          0,          0           });
    }







}
