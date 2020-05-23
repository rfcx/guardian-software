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

public class SentinelPositionUtils {

    public SentinelPositionUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.deviceI2cUtils = new DeviceI2cUtils(sentinelPositionI2cMainAddress);
        initSentinelPositionI2cOptions();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelPositionUtils");

    RfcxGuardian app;
    private DeviceI2cUtils deviceI2cUtils = null;
    private static final String sentinelPositionI2cMainAddress = "0x1e";

    private String[] i2cValueIndex = new String[]{};
    private Map<String, double[]> i2cTmpValues = new HashMap<>();
    private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();

    private List<double[]> envEnclosureValues = new ArrayList<>();

    private boolean verboseLogging = false;

    private void initSentinelPositionI2cOptions() {

//        this.i2cValueIndex = new String[]{"voltage", "current", "temperature", "power"};
//
//        //										        voltage     current     temp
//        this.i2cAddresses.put("battery", new String[]{  "0x3a",     "0x3d",     null    });
//        this.i2cAddresses.put("input", new String[]{    "0x3b",     "0x3e",     null    });
//        this.i2cAddresses.put("system", new String[]{   "0x3c",     null,       "0x3f"  });
//
//        //										        voltage     current     temp
//        this.i2cLastReadAt.put("battery", new long[]{   0,          0,          0   });
//        this.i2cLastReadAt.put("input", new long[]{     0,          0,          0   });
//        this.i2cLastReadAt.put("system", new long[]{    0,          0,          0   });
//
//        //												voltage		current 	temp		power
//        this.i2cValues.put("battery", new double[]{     0,          0,          0,          0   });
//        this.i2cValues.put("input", new double[]{       0,          0,          0,          0   });
//        this.i2cValues.put("system", new double[]{      0,          0,          0,          0   });

    }


    public static JSONArray getSentinelPositionValuesAsJsonArray(Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray positionJsonArray = new JSONArray();
        try {
            JSONObject sensorJson = new JSONObject();
            sensorJson.put("enclosure", app.sentinelPositionDb.dbSentinelPositionEnclosure.getConcatRowsWithLabelPrepended("enclosure"));
            positionJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return positionJsonArray;
        }
    }

    public static int deleteSentinelPositionValuesBeforeTimestamp(String timeStamp, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));

        app.sentinelPositionDb.dbSentinelPositionEnclosure.clearRowsBefore(clearBefore);

        return 1;
    }

}
