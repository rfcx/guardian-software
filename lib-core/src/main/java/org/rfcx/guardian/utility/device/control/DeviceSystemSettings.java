package org.rfcx.guardian.utility.device.control;


import android.text.TextUtils;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DeviceSystemSettings {

	public DeviceSystemSettings(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceSystemSettings");
	}

	private String logTag;
	private boolean haveActiveValsBeenSet = false;

	private Map<String, String[]> activeValsMap = new HashMap<>();

	public void checkSetActiveVals() {
		if (!this.haveActiveValsBeenSet) {
			setVals(this.activeValsMap);
		}
		this.haveActiveValsBeenSet = true;
	}

	public void setSerializedValsMap(String serializedValsMap) {
		Map<String, String[]> deserializedValsMap = deserializeValsMap(serializedValsMap);
		setVals(deserializedValsMap);
	}

	private void addActiveVals(Map<String, String[]> valsMap) {
		for (Map.Entry valMap : valsMap.entrySet()) {
			addActiveVal(valMap.getKey().toString(), valsMap.get(valMap.getKey().toString()));
		}
	}

	private void addActiveVal(String valKey, String[] valMeta) {
		this.activeValsMap.remove(valKey.toLowerCase(Locale.US));
		this.activeValsMap.put(valKey.toLowerCase(Locale.US), valMeta);
	}

	public static void setVals(Map<String, String[]> valsMap) {
		List<String> execList = new ArrayList<>();
		for (Map.Entry valMap : valsMap.entrySet()) {
			String valKey = valMap.getKey().toString();
			String[] valMeta = valsMap.get(valKey);
			if (valMeta.length == 3) {
				execList.add(setValExecStr(valKey, valMeta[0], valMeta[1], valMeta[2]));
			}
		}
		ShellCommands.executeCommandAsRootAndIgnoreOutput(ArrayUtils.ListToStringArray(execList));
	}

	public static void setVal(String valKey, String valGrp, String valType, String valVal) {
		ShellCommands.executeCommandAsRootAndIgnoreOutput( setValExecStr(valKey, valGrp, valType, valVal) );
	}

	private static String setValExecStr(String valKey, String valGrp, String valType, String valVal) {
		return 	"content update --uri content://settings"
				+ "/" + valGrp.toLowerCase(Locale.US)
				+ "/" + valKey.toLowerCase(Locale.US)
				+ " --bind value:" + valType.toLowerCase(Locale.US) + ":" + valVal;
	}

	public void loadActiveVals(Map<String, String[]> valsMap) {
		for (Map.Entry valMap : valsMap.entrySet()) {
			String valKey = valMap.getKey().toString().toLowerCase(Locale.US);
			String[] valMeta = valsMap.get(valKey);
			addActiveVal(valKey, valMeta);
		}
	}

	private Map<String, String[]> deserializeValsMap(String serializedValsMap) {
		Map<String, String[]> deserializedValsMap = new HashMap<>();
		if (serializedValsMap.length() > 0) {
			for (String valMap : TextUtils.split(serializedValsMap, Pattern.quote(";"))) {
				String[] valMapPair = TextUtils.split(valMap, Pattern.quote(":"));
				if (valMapPair.length == 2) {
					String[] valMapMeta = TextUtils.split(valMapPair[1], Pattern.quote(","));
					if (valMapMeta.length == 3) {
						deserializedValsMap.put(valMapPair[0].toLowerCase(Locale.US), valMapMeta);
					}
				}
			}
		}
		return deserializedValsMap;
	}
}
