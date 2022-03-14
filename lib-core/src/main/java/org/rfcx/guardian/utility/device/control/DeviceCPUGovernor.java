package org.rfcx.guardian.utility.device.control;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceCPUGovernor {

    private final String logTag;
    private boolean haveActiveValsBeenSet = false;
    private String cpuGovernorDirPath;
    private final Map<String, String[]> activeValsMap = new HashMap<>();
    public DeviceCPUGovernor(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceCPUGovernor");
    }

    private static List<String> presetParamsExecStr(String cpuGovernorDirPath) {
        int setCpuCount = 1;
        List<String> execList = new ArrayList<>();
        for (int cpuNum = 0; cpuNum < setCpuCount; cpuNum++) {
            execList.add("chmod 666 " + cpuGovernorDirPath + "/cpu" + cpuNum + "/cpufreq/scaling_governor");
            execList.add("echo hotplug > " + cpuGovernorDirPath + "/cpu" + cpuNum + "/cpufreq/scaling_governor");
            execList.add("chmod 666 " + cpuGovernorDirPath + "/cpu" + cpuNum + "/cpufreq/scaling_max_freq");
            execList.add("chmod 666 " + cpuGovernorDirPath + "/cpu" + cpuNum + "/cpufreq/scaling_min_freq");
        }
        return execList;
    }

    public static void setVals(String cpuGovernorDirPath, Map<String, String[]> paramMap) {
        List<String> execList = presetParamsExecStr(cpuGovernorDirPath);
        for (Map.Entry pMap : paramMap.entrySet()) {
            String paramKey = pMap.getKey().toString();
            String[] paramMeta = paramMap.get(paramKey);
            if (paramMeta.length == 2) {
                execList.add(setValsExecStr(cpuGovernorDirPath, paramMeta[0], paramKey, paramMeta[1]));
            }
        }
        ShellCommands.executeCommandAsRootAndIgnoreOutput(ArrayUtils.ListToStringArray(execList));
    }

    private static String setValsExecStr(String dirPath, String govName, String paramName, String paramVal) {
        return "echo " + paramVal + " > " + dirPath + "/cpufreq/" + govName + "/" + paramName;
    }

    public void checkSetActiveVals() {
        if (!this.haveActiveValsBeenSet) {
            setVals(this.activeValsMap);
        }
        this.haveActiveValsBeenSet = true;
    }

    public void setVals(Map<String, String[]> paramMap) {
        List<String> execList = presetParamsExecStr(cpuGovernorDirPath);
        for (Map.Entry pMap : paramMap.entrySet()) {
            String paramKey = pMap.getKey().toString();
            String[] paramMeta = paramMap.get(paramKey);
            if (paramMeta.length == 2) {
                execList.add(setValsExecStr(cpuGovernorDirPath, paramMeta[0], paramKey, paramMeta[1]));
            }
        }
        ShellCommands.executeCommandAsRootAndIgnoreOutput(ArrayUtils.ListToStringArray(execList));
    }

    public void loadDirPath(String dirPath) {
        this.cpuGovernorDirPath = dirPath;
    }

    public void loadActiveVals(Map<String, String[]> valsMap) {
        for (Map.Entry valMap : valsMap.entrySet()) {
            String valKey = valMap.getKey().toString().toLowerCase(Locale.US);
            String[] valMeta = valsMap.get(valKey);
            addActiveVal(valKey, valMeta);
        }
    }

    private void addActiveVal(String valKey, String[] valMeta) {
        this.activeValsMap.remove(valKey.toLowerCase(Locale.US));
        this.activeValsMap.put(valKey.toLowerCase(Locale.US), valMeta);
    }

}
