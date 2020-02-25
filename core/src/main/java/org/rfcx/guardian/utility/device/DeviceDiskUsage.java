package org.rfcx.guardian.utility.device;

import android.os.Environment;
import android.os.StatFs;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceDiskUsage {
	
	public DeviceDiskUsage(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceDiskUsage.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceDiskUsage.class);

	private static StatFs getStatFs(String absolutePath){
		return new StatFs(absolutePath);
	}

	public static long[] getCurrentDiskUsageStats() {
		StatFs intStat = getStatFs(Environment.getRootDirectory().getAbsolutePath());
		StatFs extStat = getStatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		return new long[] { System.currentTimeMillis(), diskUsedBytes(intStat), diskFreeBytes(intStat), diskUsedBytes(extStat), diskFreeBytes(extStat) };
	}
	
	private static long diskTotalBytes(StatFs statFs) {
		return (((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize()));
	}

	private static long diskFreeBytes(StatFs statFs) {
		return (((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize()));
	}

	private static long diskUsedBytes(StatFs statFs) {
		return ( ((long) (statFs.getBlockCount()) * ((long) statFs.getBlockSize())) - (((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize())) );
	}

	
//	public static int getInternalDiskFreeMegaBytes() {
//		return Math.round(diskFreeBytes(getStats(false)) / (1024 * 1024));
//	}
//	
//	public static int getExternalDiskFreeMegaBytes() {
//		return Math.round(diskFreeBytes(getStats(true)) / (1024 * 1024));
//	}


}
