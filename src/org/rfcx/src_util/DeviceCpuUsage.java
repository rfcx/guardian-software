package org.rfcx.src_util;

import java.io.IOException;
import java.io.RandomAccessFile;

public class DeviceCpuUsage {
	
	public static final int AVERAGE_LENGTH = 60;
	private float cpuUsageNow = 0;
	private float cpuUsageAvg = 0;
	private float[] prevCpuUsage = new float[AVERAGE_LENGTH];
	
	public int getCpuUsageNow() {
		return Math.round(100*cpuUsageNow);
	}
	
	public int getCpuUsageAvg() {
		return Math.round(100*cpuUsageAvg);
	}
	
	public void updateCpuUsage() {
		this.cpuUsageNow = updateUsage();
		incrementAvg();
	}
	
	private void incrementAvg() {
		float avgTotal = 0;
		for (int i = 0; i < AVERAGE_LENGTH-1; i++) {
			this.prevCpuUsage[i] = this.prevCpuUsage[i+1];
			avgTotal = avgTotal + this.prevCpuUsage[i+1];
		}
		this.prevCpuUsage[AVERAGE_LENGTH-1] = this.cpuUsageNow;
		this.cpuUsageAvg = (avgTotal + this.cpuUsageNow) / AVERAGE_LENGTH;
	}
	
	private float updateUsage() {
		try {
	        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
	        String load = reader.readLine();
	        String[] toks = load.split(" ");
	        long idle1 = Long.parseLong(toks[5]);
	        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
	        try {
	            Thread.sleep(360);
	        } catch (Exception e) {}
	        reader.seek(0);
	        load = reader.readLine();
	        reader.close();
	        toks = load.split(" ");
	        long idle2 = Long.parseLong(toks[5]);
	        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
	        return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
		return 0;
	}
}
