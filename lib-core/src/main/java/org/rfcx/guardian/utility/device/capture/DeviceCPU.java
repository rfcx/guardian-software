package org.rfcx.guardian.utility.device.capture;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceCPU {
	
	public DeviceCPU(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceCPU");
	}

	private String logTag;

	public static final long SAMPLE_DURATION_MILLISECONDS = 0;

	private List<Double> cpuPctVals = new ArrayList<>();
	private List<Double> cpuSpdVals = new ArrayList<>();

	double[] prevCpuPct = new double[] { 0, 0 };
	double prevCpuClk = 0;
//	double[] prevCpu0Pct = new double[] { 0, 0 };
//	double[] prevCpu1Pct = new double[] { 0, 0 };

	public int[] getCurrentStats() {
		int[] currentStats = new int[] { (int) Math.round(ArrayUtils.getAverage(cpuPctVals)), (int) Math.round(ArrayUtils.getAverage(cpuSpdVals)) };
		Log.v(logTag, DateTimeUtils.getDateTime()+" - CPU - " + currentStats[0] + "% at " + currentStats[1] + " MHz (" + cpuPctVals.size() + " samples)");
		cpuPctVals = new ArrayList<>();
		cpuSpdVals = new ArrayList<>();
		return currentStats;
	}
	
	public void update(boolean verboseLogging) {

		double[] currCpuPctAll = getCurrentCPUPercentage(this.logTag);
		double[] currCpuPct = new double[] { currCpuPctAll[0], currCpuPctAll[1] };
		double currCpuClk = getCurrentCPUClockSpeed(this.logTag);
//		double[] currCpu0Pct = new double[] { currCpuPctAll[2], currCpuPctAll[3] };
//		double[] currCpu1Pct = new double[] { currCpuPctAll[4], currCpuPctAll[5] };

		if (((prevCpuPct[0]+ prevCpuPct[1]) > 0) && ((currCpuPct[0]+currCpuPct[1]) > 0)) {

			double cpuPct = 100 * (currCpuPct[1] - prevCpuPct[1]) / ((currCpuPct[1] + currCpuPct[0]) - (prevCpuPct[1] + prevCpuPct[0]));
//			double cpu0Pct = 100 * (currCpu0Pct[1] - prevCpu0Pct[1]) / ((currCpu0Pct[1] + currCpu0Pct[0]) - (prevCpu0Pct[1] + prevCpu0Pct[0]));
//			double cpu1Pct = 100 * (currCpu1Pct[1] - prevCpu1Pct[1]) / ((currCpu1Pct[1] + currCpu1Pct[0]) - (prevCpu1Pct[1] + prevCpu1Pct[0]));

			if ((cpuPct <= 100) && (cpuPct > 0)) {

				double cpuClk = ((currCpuClk + prevCpuClk) / 2) / 1000;

				cpuPctVals.add(cpuPct);
				cpuSpdVals.add(cpuClk);

				if (verboseLogging) {
					Log.d(logTag, DateTimeUtils.getDateTime()+" - CPU - " + Math.round(cpuPct) + "%"
							//	+" ( "+Math.round(cpu0Pct)+"% / "+Math.round(cpu1Pct)+"%)"
							+ " at " + Math.round(cpuClk) + " MHz");
				}
			}

		}
		prevCpuPct = currCpuPct;
		prevCpuClk = currCpuClk;
//		prevCpu0Pct = currCpu0Pct;
//		prevCpu1Pct = currCpu1Pct;
	}
	
	private double[] getCurrentCPUPercentage(String logTag) {
		double[] rtrnVals = new double[] { 0, 0, 0, 0, 0, 0 };
        try {

            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");

            String cpuLine = reader.readLine();
			String[] cpuVals = cpuLine.split(" ");
			if (cpuVals[0].equalsIgnoreCase("cpu")) {
				rtrnVals[0] = Long.parseLong(cpuVals[5]);
				rtrnVals[1] = Long.parseLong(cpuVals[2]) + Long.parseLong(cpuVals[3]) + Long.parseLong(cpuVals[4]) + Long.parseLong(cpuVals[6]) + Long.parseLong(cpuVals[7]) + Long.parseLong(cpuVals[8]);
			}

//			String cpu0Line = reader.readLine();
//			String[] cpu0Vals = cpu0Line.split(" ");
//			if (cpu0Vals[0].equalsIgnoreCase("cpu0")) {
//				rtrnVals[2] = Long.parseLong(cpu0Vals[5]);
//				rtrnVals[3] = Long.parseLong(cpu0Vals[2]) + Long.parseLong(cpu0Vals[3]) + Long.parseLong(cpu0Vals[4]) + Long.parseLong(cpu0Vals[6]) + Long.parseLong(cpu0Vals[7]) + Long.parseLong(cpu0Vals[8]);
//			}
//
//			String cpu1Line = reader.readLine();
//			String[] cpu1Vals = cpu1Line.split(" ");
//			if (cpu1Vals[0].equalsIgnoreCase("cpu1")) {
//				rtrnVals[4] = Long.parseLong(cpu1Vals[5]);
//				rtrnVals[5] = Long.parseLong(cpu1Vals[2]) + Long.parseLong(cpu1Vals[3]) + Long.parseLong(cpu1Vals[4]) + Long.parseLong(cpu1Vals[6]) + Long.parseLong(cpu1Vals[7]) + Long.parseLong(cpu1Vals[8]);
//			}

			reader.close();

        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
        }
		return rtrnVals;
	}
	
	private static double getCurrentCPUClockSpeed(String logTag) {
		float cpuClockSpeed = 0;
		try {
			RandomAccessFile scaling_cur_freq = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r");
			cpuClockSpeed = Integer.parseInt(scaling_cur_freq.readLine());
			scaling_cur_freq.close();
		}
		catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return cpuClockSpeed;
	}
	
}
