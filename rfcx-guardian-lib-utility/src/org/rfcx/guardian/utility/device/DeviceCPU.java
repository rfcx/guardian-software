package org.rfcx.guardian.utility.device;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceCPU {
	
	public DeviceCPU(String appRole) {
		this.logTag = "Rfcx-"+appRole+"-"+DeviceCPU.class.getSimpleName();
	}
	
	private String logTag = "Rfcx-Utils-"+DeviceCPU.class.getSimpleName();

	public static final int MEASUREMENT_DURATION_MS = 360;
//	private int avgSampleCount;
//	
//	
//	
//	
//	
//	public void setAvgSampleCount(int avgSampleCount) {
//		this.avgSampleCount = avgSampleCount;
//	}
//	
//	public int getAvgSampleCount() {
//		return this.avgSampleCount;
//	}
	
	private static int getCurrentCpuClockSpeed() throws NumberFormatException, IOException {
		int cpuClockSpeed = 0;
		RandomAccessFile readFile = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r");
		cpuClockSpeed = Integer.parseInt(readFile.readLine());
		readFile.close();
		return cpuClockSpeed;
	}
	
	private static float getCurrentCpuUsagePercentage() throws InterruptedException, IOException {
		float cpuUsagePercentage = 0;
			
        RandomAccessFile readFile = new RandomAccessFile("/proc/stat", "r");
        long[] idle_cpu_a = parseCpuUsageLine(readFile.readLine());
        
        Thread.sleep(MEASUREMENT_DURATION_MS);
        
        readFile.seek(0);
        long[] idle_cpu_b = parseCpuUsageLine(readFile.readLine());
        
        readFile.close();
        
        cpuUsagePercentage = (float) (idle_cpu_b[1] - idle_cpu_a[1]) / ( (idle_cpu_b[1] + idle_cpu_b[0]) - (idle_cpu_a[1] + idle_cpu_a[0]) );
	        
		return cpuUsagePercentage;
	}
	
	private static long[] parseCpuUsageLine(String cpuLine) {
		String[] lineElements = cpuLine.split(" ");
		long idle = Long.parseLong(lineElements[5]);
		long cpu = Long.parseLong(lineElements[2]) + Long.parseLong(lineElements[3]) + Long.parseLong(lineElements[4]) + Long.parseLong(lineElements[6]) + Long.parseLong(lineElements[7]) + Long.parseLong(lineElements[8]);
		return new long[] { idle, cpu };
	}
	
}

