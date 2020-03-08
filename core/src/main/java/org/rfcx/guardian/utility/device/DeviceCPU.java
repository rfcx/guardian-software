package org.rfcx.guardian.utility.device;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.SystemClock;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceCPU {
	
	public DeviceCPU(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceCPU.class);
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceCPU.class);
	
	private int reportingSampleCount = 60;
	
	public void setReportingSampleCount(int reportingSampleCount) {
		this.reportingSampleCount = reportingSampleCount;
		cpuPercentage_Previous = new float[this.reportingSampleCount];
		cpuClockSpeed_Previous = new float[this.reportingSampleCount];
	}

	public static final long SAMPLE_DURATION_MILLISECONDS = 360;
	
	private float cpuPercentage_Current = 0;
	private float cpuPercentage_Average = 0;
	private float[] cpuPercentage_Previous = new float[this.reportingSampleCount];
	
	private float cpuClockSpeed_Current = 0;
	private float cpuClockSpeed_Average = 0;
	private float[] cpuClockSpeed_Previous = new float[this.reportingSampleCount];
	private boolean isCPUClockSpeedSamplingAllowed = false;

	public int[] getCurrentStats() {
		return new int[] { Math.round(100*this.cpuPercentage_Average), Math.round(this.cpuClockSpeed_Average/1000) };
	}
	
	public void update() {
		
		this.cpuPercentage_Current = getCurrentCPUPercentage(this.logTag);
		incrementPercentageAverage();
		
		if (this.isCPUClockSpeedSamplingAllowed) { this.cpuClockSpeed_Current = getCurrentCPUClockSpeed(this.logTag); }
		incrementClockSpeedAverage();
		
		this.isCPUClockSpeedSamplingAllowed = !this.isCPUClockSpeedSamplingAllowed;
	}
	
	private void incrementPercentageAverage() {
		float percentageAverage_Total = 0;
		for (int i = 0; i < this.reportingSampleCount-1; i++) {
			this.cpuPercentage_Previous[i] = this.cpuPercentage_Previous[i+1];
			percentageAverage_Total = percentageAverage_Total + this.cpuPercentage_Previous[i+1];
		}
		this.cpuPercentage_Previous[this.reportingSampleCount-1] = this.cpuPercentage_Current;
		this.cpuPercentage_Average = (percentageAverage_Total + this.cpuPercentage_Current) / this.reportingSampleCount;
	}
	
	private void incrementClockSpeedAverage() {
		float clockSpeedAverage_Total = 0;
		for (int i = 0; i < this.reportingSampleCount-1; i++) {
			this.cpuClockSpeed_Previous[i] = this.cpuClockSpeed_Previous[i+1];
			clockSpeedAverage_Total = clockSpeedAverage_Total + this.cpuClockSpeed_Previous[i+1];
		}
		this.cpuClockSpeed_Previous[this.reportingSampleCount-1] = this.cpuClockSpeed_Current;
		this.cpuClockSpeed_Average = (clockSpeedAverage_Total + this.cpuClockSpeed_Current) / this.reportingSampleCount;
	}
	
	private static float getCurrentCPUPercentage(String logTag) {
        if (!DeviceRoot.isRooted()) {
            return 0;
        }
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();
            String[] toks = load.split(" ");
            long idle1 = Long.parseLong(toks[5]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            try {
                SystemClock.sleep(SAMPLE_DURATION_MILLISECONDS);
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
            reader.seek(0);
            load = reader.readLine();
            reader.close();
            toks = load.split(" ");
            long idle2 = Long.parseLong(toks[5]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));
        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
        }
		return 0;
	}
	
	private static float getCurrentCPUClockSpeed(String logTag) {
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
