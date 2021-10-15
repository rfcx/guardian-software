package org.rfcx.guardian.utility.device.hardware;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class DeviceHardwareUtils {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceHardwareUtils");

    public static JSONObject getInfoAsJson() {
        List<String[]> hardwareInfoList = getInfo();
        JSONObject hardwareInfoJson = new JSONObject();
        for (int i = 0; i < hardwareInfoList.size(); i++) {
            try {
                hardwareInfoJson.put(hardwareInfoList.get(i)[0], hardwareInfoList.get(i)[1]);
            } catch (JSONException e) {
                RfcxLog.logExc(logTag, e);
            }
        }
        return hardwareInfoJson;
    }

    private static List<String[]> getInfo() {
        List<String[]> hardwareInfo = new ArrayList<String[]>();
        hardwareInfo.add(new String[]{"brand", getBrand()});
        hardwareInfo.add(new String[]{"manufacturer", getManufacturer()});
        hardwareInfo.add(new String[]{"product", getProduct()});
        hardwareInfo.add(new String[]{"model", getModel()});
        hardwareInfo.add(new String[]{"android", getRelease()});
        hardwareInfo.add(new String[]{"build", getBuildNumber()});
        return hardwareInfo;
    }

    public static String getName() {
        return getManufacturer() + " " + getModel();
    }

    public static String getBrand() {
        return android.os.Build.BRAND;
    }

    public static String getManufacturer() {
        return android.os.Build.MANUFACTURER;
    }

    public static String getProduct() {
        return android.os.Build.PRODUCT;
    }

    public static String getModel() {
        return android.os.Build.MODEL;
    }

    public static String getRelease() {
        return android.os.Build.VERSION.RELEASE;
    }

    public static String getBuildNumber() {
        return android.os.Build.DISPLAY;
    }

}
