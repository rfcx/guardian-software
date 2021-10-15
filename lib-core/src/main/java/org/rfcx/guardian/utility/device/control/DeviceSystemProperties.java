package org.rfcx.guardian.utility.device.control;


import android.text.TextUtils;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DeviceSystemProperties {

    private String logTag;
    private boolean haveDefaultValsBeenSet = false;
    private Map<String, String> activeValsMap = new HashMap<>();

    public DeviceSystemProperties(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceSystemProperties");
    }

    public static void setVal(String valKey, String valVal) {
        String execStr = "setprop"
                + " " + valKey.toLowerCase(Locale.US)
                + " " + (((valVal == null) || (valVal.length() == 0)) ? "\"\"" : valVal);
        ShellCommands.executeCommandAsRootAndIgnoreOutput(execStr);
    }

    public void checkSetDefaultVals() {
        if (!this.haveDefaultValsBeenSet) {
            setVals(this.activeValsMap);
        }
        this.haveDefaultValsBeenSet = true;
    }

    public void loadAndSetSerializedValsMap(String serializedValsMap) {
        Map<String, String> deserializedValsMap = deserializeValsMap(serializedValsMap);
        addActiveVals(deserializedValsMap);
        setVals(deserializedValsMap);
    }

    private void addActiveVals(Map<String, String> valsMap) {
        for (Map.Entry valMap : valsMap.entrySet()) {
            addActiveVal(valMap.getKey().toString(), valsMap.get(valMap.getKey().toString()));
        }
    }

    private void addActiveVal(String valKey, String valVal) {
        this.activeValsMap.remove(valKey.toLowerCase(Locale.US));
        this.activeValsMap.put(valKey.toLowerCase(Locale.US), valVal);
    }

    public void setVals(Map<String, String> valsMap) {
        for (Map.Entry valMap : valsMap.entrySet()) {
            String valKey = valMap.getKey().toString();
            setVal(valKey, valsMap.get(valKey));
        }
    }

    public void loadDefaultVals(Map<String, String> valsMap) {
        for (Map.Entry valMap : valsMap.entrySet()) {
            String valKey = valMap.getKey().toString().toLowerCase(Locale.US);
            String valVal = valsMap.get(valKey);
            addActiveVal(valKey, valVal);
        }
    }

    private Map<String, String> deserializeValsMap(String serializedValsMap) {
        Map<String, String> deserializedValsMap = new HashMap<>();
        if (serializedValsMap.length() > 0) {
            for (String valMap : TextUtils.split(serializedValsMap, Pattern.quote(";"))) {
                String[] valMapPair = TextUtils.split(valMap, Pattern.quote(":"));
                if (valMapPair.length == 2) {
                    deserializedValsMap.put(valMapPair[0].toLowerCase(Locale.US), valMapPair[1]);
                }
            }
        }
        return deserializedValsMap;
    }
}
