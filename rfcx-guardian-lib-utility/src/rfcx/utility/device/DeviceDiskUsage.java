package rfcx.utility.device;

import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceDiskUsage {
	
	public DeviceDiskUsage(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceDiskUsage.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceDiskUsage.class);
 
	public static String concatDiskStats() {
		List<String> diskUsage = new ArrayList<String>();
		for (String[] usageStat : allDiskStats()) {
			diskUsage.add(TextUtils.join("*", usageStat));
		}
		return TextUtils.join("|", diskUsage);
	}
	
	public static List<String[]> allDiskStats() {
		StatFs internalStatFs = getStats(false);
		StatFs sdCardStatFs = getStats(true);
		List<String[]> allStats = new ArrayList<String[]>();
		allStats.add(new String[] { "internal", ""+System.currentTimeMillis(), ""+diskUsedBytes(internalStatFs), ""+diskFreeBytes(internalStatFs) });
		allStats.add(new String[] { "external", ""+System.currentTimeMillis(), ""+diskUsedBytes(sdCardStatFs), ""+diskFreeBytes(sdCardStatFs) });
		return allStats;
	}
	
//	public static long getInternalDiskFreeBytes() {
//		return diskFreeBytes(getStats(false));
//	}
//	
//	public static long getExternalDiskFreeBytes() {
//		return diskFreeBytes(getStats(true));
//	}
	
	public static int getInternalDiskFreeMegaBytes() {
		return Math.round(diskFreeBytes(getStats(false)) / (1024 * 1024));
	}
	
	public static int getExternalDiskFreeMegaBytes() {
		return Math.round(diskFreeBytes(getStats(true)) / (1024 * 1024));
	}

  public static long diskTotalBytes(StatFs statFs) {
    return (((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize()));
  }

  public static long diskFreeBytes(StatFs statFs) {
    return (((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize()));
  }

  public static long diskUsedBytes(StatFs statFs) {
    return ( ((long) (statFs.getBlockCount()) * ((long) statFs.getBlockSize())) - (((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize())) );
  }

  private static StatFs getStats(boolean external){
    if (external) {
    	return new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
    } else {
    	return new StatFs(Environment.getRootDirectory().getAbsolutePath());
    }
  }

}
