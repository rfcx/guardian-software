package org.rfcx.guardian.utility.device.capture;

import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class DeviceCPU {

    public static final long SAMPLE_DURATION_MILLISECONDS = 0;
    public boolean verboseLogging = false;
    double[] prevCpuPct = new double[]{-1, -1};
    double prevCpuClk = 0;
    double[] prevCpu0Pct = new double[]{-1, -1};
    double[] prevCpu1Pct = new double[]{-1, -1};
    double prevCpuCnt = 0;
    private String logTag;
    private List<Double> cpuPctVals = new ArrayList<>();
    private List<Double> cpuSpdVals = new ArrayList<>();
    private List<Double> cpuCntVals = new ArrayList<>();
    public DeviceCPU(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceCPU");
    }

    private static double getCurrentCPUClockSpeed(String logTag) {
        float cpuClockSpeed = 0;
        try {
            RandomAccessFile scaling_cur_freq = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r");
            cpuClockSpeed = Integer.parseInt(scaling_cur_freq.readLine());
            scaling_cur_freq.close();
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return cpuClockSpeed;
    }

    public int[] getAverageOfCachedValuesAndClearCache() {
        int[] avgVals = new int[]{
                (cpuPctVals.size() > 0) ? (int) Math.round(ArrayUtils.getAverage(cpuPctVals)) : 0,
                (cpuSpdVals.size() > 0) ? (int) Math.round(ArrayUtils.getAverage(cpuSpdVals)) : 0,
                (cpuCntVals.size() > 0) ? (int) Math.round(ArrayUtils.getAverage(cpuCntVals)) : 0
        };
        Log.d(logTag, "Avg of " + cpuPctVals.size() + " samples for " + DateTimeUtils.getDateTime() + " [ total: " + avgVals[0] + "%, " + avgVals[1] + " MHz, " + avgVals[2] + "% core usage ]");
        cpuPctVals = new ArrayList<>();
        cpuSpdVals = new ArrayList<>();
        return avgVals;
    }

    public int[] getCurrentValues() {
        return new int[]{
                (cpuPctVals.size() > 0) ? (int) Math.round(ArrayUtils.getAverage(cpuPctVals)) : 0,
                (cpuSpdVals.size() > 0) ? (int) Math.round(ArrayUtils.getAverage(cpuSpdVals)) : 0,
                (cpuCntVals.size() > 0) ? (int) Math.round(ArrayUtils.getAverage(cpuCntVals)) : 0
        };
    }

    public void update() {

        double[] currCpuPctAll = getCurrentCPUPercentage(this.logTag);
        double[] currCpuPct = new double[]{currCpuPctAll[0], currCpuPctAll[1]};
        double currCpuClk = getCurrentCPUClockSpeed(this.logTag);
        double[] currCpu0Pct = new double[]{currCpuPctAll[2], currCpuPctAll[3]};
        double[] currCpu1Pct = new double[]{currCpuPctAll[4], currCpuPctAll[5]};
        double currCpuCnt = ((currCpu1Pct[0] + currCpu1Pct[1]) > 0) ? 2 : 1;

        if (((prevCpuPct[0] + prevCpuPct[1]) > 0) && ((currCpuPct[0] + currCpuPct[1]) > 0)) {

            double cpuPct = 100 * (currCpuPct[1] - prevCpuPct[1]) / ((currCpuPct[1] + currCpuPct[0]) - (prevCpuPct[1] + prevCpuPct[0]));
//			double cpu0Pct = 100 * (currCpu0Pct[1] - prevCpu0Pct[1]) / ((currCpu0Pct[1] + currCpu0Pct[0]) - (prevCpu0Pct[1] + prevCpu0Pct[0]));
//			double cpu1Pct = 100 * (currCpu1Pct[1] - prevCpu1Pct[1]) / ((currCpu1Pct[1] + currCpu1Pct[0]) - (prevCpu1Pct[1] + prevCpu1Pct[0]));

            if ((cpuPct <= 100) && (cpuPct > 0)) {

                double cpuClk = ((currCpuClk + prevCpuClk) / 2) / 1000;
                double cpuCnt = 100 * (currCpuCnt + prevCpuCnt) / 2;

                cpuPctVals.add(cpuPct);
                cpuSpdVals.add(cpuClk);
                cpuCntVals.add(cpuCnt);

                if (verboseLogging) {
                    Log.d(logTag, "[ total: " + Math.round(cpuPct) + "%, " + Math.round(cpuClk) + " MHz, " + Math.round(cpuCnt / 100) + " core(s) ]");
                }
            }

        }
        prevCpuPct = currCpuPct;
        prevCpuClk = currCpuClk;
        prevCpuCnt = currCpuCnt;
        prevCpu0Pct = currCpu0Pct;
        prevCpu1Pct = currCpu1Pct;
    }

    private double[] getCurrentCPUPercentage(String logTag) {
        double[] rtrnVals = new double[]{-1, -1, -1, -1, -1, -1};
        try {

            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");

            String cpuLine = reader.readLine();
            String[] cpuVals = cpuLine.split(" ");
            if (cpuVals[0].equalsIgnoreCase("cpu")) {
                rtrnVals[0] = Long.parseLong(cpuVals[5]);
                rtrnVals[1] = Long.parseLong(cpuVals[2]) + Long.parseLong(cpuVals[3]) + Long.parseLong(cpuVals[4]) + Long.parseLong(cpuVals[6]) + Long.parseLong(cpuVals[7]) + Long.parseLong(cpuVals[8]);
            }

            String cpu0Line = reader.readLine();
            String[] cpu0Vals = cpu0Line.split(" ");
            if (cpu0Vals[0].equalsIgnoreCase("cpu0")) {
                rtrnVals[2] = Long.parseLong(cpu0Vals[5]);
                rtrnVals[3] = Long.parseLong(cpu0Vals[2]) + Long.parseLong(cpu0Vals[3]) + Long.parseLong(cpu0Vals[4]) + Long.parseLong(cpu0Vals[6]) + Long.parseLong(cpu0Vals[7]) + Long.parseLong(cpu0Vals[8]);
            }

            String cpu1Line = reader.readLine();
            String[] cpu1Vals = cpu1Line.split(" ");
            if (cpu1Vals[0].equalsIgnoreCase("cpu1")) {
                rtrnVals[4] = Long.parseLong(cpu1Vals[5]);
                rtrnVals[5] = Long.parseLong(cpu1Vals[2]) + Long.parseLong(cpu1Vals[3]) + Long.parseLong(cpu1Vals[4]) + Long.parseLong(cpu1Vals[6]) + Long.parseLong(cpu1Vals[7]) + Long.parseLong(cpu1Vals[8]);
            }

            reader.close();

        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
        }
        return rtrnVals;
    }

}
