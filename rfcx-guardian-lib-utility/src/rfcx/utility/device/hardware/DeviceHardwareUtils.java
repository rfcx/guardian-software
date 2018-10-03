package rfcx.utility.device.hardware;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceHardwareUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceHardwareUtils.class);
	
	public static JSONObject getDeviceHardwareInfoJson() {
		List<String[]> hardwareInfoList = getDeviceHardwareInfo();
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
	
	private static List<String[]> getDeviceHardwareInfo() {
		List<String[]> hardwareInfo = new ArrayList<String[]>();
		hardwareInfo.add(new String[] { "brand", android.os.Build.BRAND });
		hardwareInfo.add(new String[] { "manufacturer", android.os.Build.MANUFACTURER });
		hardwareInfo.add(new String[] { "product", android.os.Build.PRODUCT });
		hardwareInfo.add(new String[] { "model", android.os.Build.MODEL });
		hardwareInfo.add(new String[] { "android", android.os.Build.VERSION.RELEASE });
		return hardwareInfo;
	}
	
	public static String getDeviceHardwareName() {
		return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
	}
	
}
