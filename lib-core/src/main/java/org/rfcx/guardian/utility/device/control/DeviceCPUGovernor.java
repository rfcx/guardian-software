package org.rfcx.guardian.utility.device.control;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeviceCPUGovernor {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceCPUGovernor");

	public static void setParams(String cpuGovernorDirPath, Map<String, String[]> paramMap) {
		List<String> execList = presetParamsExecStr(cpuGovernorDirPath);
		for (Map.Entry pMap : paramMap.entrySet()) {
			String paramKey = pMap.getKey().toString();
			String[] paramMeta = paramMap.get(paramKey);
			if (paramMeta.length == 2) {
				execList.add(setParamExecStr(cpuGovernorDirPath, paramMeta[0], paramKey, paramMeta[1]));
			}
		}
		ShellCommands.executeCommandAsRootAndIgnoreOutput(ArrayUtils.ListToStringArray(execList));
	}

	private static String setParamExecStr(String dirPath, String govName, String paramName, String paramVal) {
		return 	"echo " + paramVal + " > " + dirPath + "/cpufreq/" + govName + "/" + paramName;
	}

	private static List<String> presetParamsExecStr(String cpuGovernorDirPath) {
		int setCpuCount = 1;
		List<String> execList = new ArrayList<>();
		for (int cpuNum = 0; cpuNum < setCpuCount; cpuNum++) {
			execList.add("chmod 666 "+cpuGovernorDirPath+"/cpu"+cpuNum+"/cpufreq/scaling_governor");
			execList.add("echo hotplug > "+cpuGovernorDirPath+"/cpu"+cpuNum+"/cpufreq/scaling_governor");
			execList.add("chmod 666 "+cpuGovernorDirPath+"/cpu"+cpuNum+"/cpufreq/scaling_max_freq");
			execList.add("chmod 666 "+cpuGovernorDirPath+"/cpu"+cpuNum+"/cpufreq/scaling_min_freq");
		}
		return execList;
	}

}
