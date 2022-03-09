package org.rfcx.guardian.gpio;

import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceGpioUtils {

    private static final String[] GPIO_POSSIBLE_COMMANDS = new String[]{"MODE", "PULL_SEL", "DOUT", "PULL_EN", "DIR", "IES", "SMT"};
    private final String logTag;
    private final Map<String, String> addrMap = new HashMap<String, String>();
    private final Map<String, String> dirMap = new HashMap<String, String>();
    private String gpioHandlerFilepath;
    private boolean isHandlerReadable = false;

    public DeviceGpioUtils(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceGpioUtils");
    }

    public void setGpioHandlerFilepath(String handlerFilepath) {
        this.gpioHandlerFilepath = handlerFilepath;
    }

    public void setupAddresses(Map<String, String[]> addrMap) {
        for (Map.Entry addr : addrMap.entrySet()) {
            String addrName = addr.getKey().toString();
            setAddrNumByName(addrName, addrMap.get(addrName)[0]);
            setAddrDirByName(addrName, addrMap.get(addrName)[1]);
        }
    }

    private boolean checkSetIsHandlerAccessible() {
        if (!this.isHandlerReadable) {
            File handlerFile = (new File(gpioHandlerFilepath));
            if (handlerFile.exists() && handlerFile.canRead() && handlerFile.canWrite()) {
                this.isHandlerReadable = true;
            } else {
                Log.e(logTag, "Could not access GPIO Handler: " + gpioHandlerFilepath);
            }
        }
        return this.isHandlerReadable;
    }

    private String checkSetGpioCommand(String gpioCmd) {
        String outputCmd = null;

        if (ArrayUtils.doesStringArrayContainString(GPIO_POSSIBLE_COMMANDS, gpioCmd.toUpperCase(Locale.US))) {
            if ((gpioCmd.length() > 5) && gpioCmd.substring(0, 5).equalsIgnoreCase("PULL_")) {
                gpioCmd = "P" + gpioCmd.substring(5);
            }
            outputCmd = gpioCmd;
        } else {
            Log.e(logTag, "Command '" + gpioCmd + "' could not be matched to a GPIO command");
        }

        return outputCmd;
    }

    public void runGpioCommand(String cmd, String addrName, boolean setToHigh) {

        try {

            int addrNum = getAddrNumByName(addrName);
            String execCmd = checkSetGpioCommand(cmd);

            if (checkSetIsHandlerAccessible() && (addrNum != 0) && (execCmd != null)) {

                String execStr = "echo"
                        + " -w" + execCmd.toLowerCase(Locale.US)
                        + " " + addrNum
                        + " " + (setToHigh ? "1" : "0")
                        + " > " + gpioHandlerFilepath;

                Log.v(logTag, "Running GPIO command on '" + addrName + "': " + execCmd.toUpperCase(Locale.US) + " -> " + ((setToHigh) ? "HIGH" : "LOW"));

                ShellCommands.executeCommandAndIgnoreOutput(execStr);
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }


    public boolean readGpioValue(String addrName, String readField) {

        try {

            int addrNum = getAddrNumByName(addrName);

            if (checkSetIsHandlerAccessible() && (addrNum != 0)) {

                String execStr = "cat " + gpioHandlerFilepath + " | grep '" + addrNum + ":'";

                for (String execRtrn : ShellCommands.executeCommand(execStr)) {
                    String rtrnStr = execRtrn.substring(execRtrn.indexOf(":") + 1, execRtrn.lastIndexOf("-"));
                    int readFieldInd = ArrayUtils.indexOfStringInStringArray(GPIO_POSSIBLE_COMMANDS, readField.toUpperCase(Locale.US));
                    if (readFieldInd >= 0) {
                        return 1 == Integer.parseInt(rtrnStr.substring(readFieldInd, readFieldInd + 1), 2);
                    }
                }
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return false;
    }


    private void setAddrNumByName(String addrName, int addrNum) {
        this.addrMap.remove(addrName.toUpperCase(Locale.US));
        this.addrMap.put(addrName.toUpperCase(Locale.US), "" + addrNum);
    }

    private void setAddrNumByName(String addrName, String addrNum) {
        setAddrNumByName(addrName, Integer.parseInt(addrNum));
    }

    private int getAddrNumByName(String addrName) {
        int addrNum = 0;
        if (this.addrMap.containsKey(addrName.toUpperCase(Locale.US))) {
            addrNum = Integer.parseInt(this.addrMap.get(addrName.toUpperCase(Locale.US)));
        } else {
            Log.e(logTag, "No GPIO Numeric Address assignment for '" + addrName + "'");
        }
        return addrNum;
    }


    private void setAddrDirByName(String addrName, String addrDir) {
        this.dirMap.remove(addrName.toUpperCase(Locale.US));
        this.dirMap.put(addrName.toUpperCase(Locale.US), addrDir);
        boolean isWrite = addrDir.equalsIgnoreCase("write") || addrDir.equalsIgnoreCase("w");
        runGpioCommand("DIR", addrName, isWrite);
    }

    private String getAddrDirByName(String addrName) {
        String addrDir = "write";
        if (this.dirMap.containsKey(addrName.toUpperCase(Locale.US))) {
            addrDir = this.dirMap.get(addrName.toUpperCase(Locale.US));
        } else {
            Log.e(logTag, "No GPIO Direction assignment for '" + addrName + "'");
        }
        return addrDir;
    }

}
