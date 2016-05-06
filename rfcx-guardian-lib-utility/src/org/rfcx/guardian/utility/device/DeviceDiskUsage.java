package org.rfcx.guardian.utility.device;

import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.os.StatFs;

public class DeviceDiskUsage {
 
  public static List<String[]> allDiskStats() {
	  StatFs internalStatFs = getStats(false);
	  StatFs sdCardStatFs = getStats(true);
	  List<String[]> allStats = new ArrayList<String[]>();
	  allStats.add(new String[] { "internal", ""+System.currentTimeMillis(), ""+diskUsedBytes(internalStatFs), ""+diskFreeBytes(internalStatFs) });
	  allStats.add(new String[] { "external", ""+System.currentTimeMillis(), ""+diskUsedBytes(sdCardStatFs), ""+diskFreeBytes(sdCardStatFs) });
	  return allStats;
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
