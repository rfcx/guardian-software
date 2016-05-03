package org.rfcx.guardian.utility.device;

import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.os.StatFs;

public class DeviceDiskUsage {
	
  public static List<int[]> allDiskStatsAsInts() {
	  StatFs internalStatFs = getStats(false);
	  StatFs sdCardStatFs = getStats(true);
	  List<int[]> allStats = new ArrayList<int[]>();
	  allStats.add(new int[] { diskUsedBytes(internalStatFs), diskFreeBytes(internalStatFs), diskTotalBytes(internalStatFs) });
	  allStats.add(new int[] { diskUsedBytes(sdCardStatFs), diskFreeBytes(sdCardStatFs), diskTotalBytes(sdCardStatFs) });
	  return allStats;
  }
 
  public static List<String[]> allDiskStatsAsStrings() {
	  StatFs internalStatFs = getStats(false);
	  StatFs sdCardStatFs = getStats(true);
	  List<String[]> allStats = new ArrayList<String[]>();
	  allStats.add(new String[] { ""+diskUsedBytes(internalStatFs), ""+diskFreeBytes(internalStatFs), ""+diskTotalBytes(internalStatFs) });
	  allStats.add(new String[] { ""+diskUsedBytes(sdCardStatFs), ""+diskFreeBytes(sdCardStatFs), ""+diskTotalBytes(sdCardStatFs) });
	  return allStats;
  }

  public static int diskTotalBytes(StatFs statFs) {
    return (statFs.getBlockCount() * statFs.getBlockSize());
  }

  public static int diskFreeBytes(StatFs statFs) {
    return (statFs.getAvailableBlocks() * statFs.getBlockSize());
  }

  public static int diskUsedBytes(StatFs statFs) {
    return ( (statFs.getBlockCount() * statFs.getBlockSize()) - (statFs.getAvailableBlocks() * statFs.getBlockSize()) );
  }

  private static StatFs getStats(boolean external){
    if (external) {
    	return new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
    } else {
    	return new StatFs(Environment.getRootDirectory().getAbsolutePath());
    }
  }

}
