package org.rfcx.guardian.utility.device.capture;


import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

public class DeviceMemory {

//	public static long[] getCurrentMemoryStats() {
//
//		long nativeHeapSize = Debug.getNativeHeapSize();
//		long nativeHeapFreeSize = Debug.getNativeHeapFreeSize();
//		long usedMemInBytes = nativeHeapSize - nativeHeapFreeSize;
//		long usedMemInPercentage = usedMemInBytes * 100 / nativeHeapSize;
//
//		return new long[] { System.currentTimeMillis(), nativeHeapSize, nativeHeapFreeSize, usedMemInBytes, usedMemInPercentage };
//	}


	public static long[] getCurrentMemoryStats(Context context) {

		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);

		long nativeHeapSize = memoryInfo.totalMem;
		long nativeHeapFreeSize = memoryInfo.availMem;
		long usedMemInBytes = nativeHeapSize - nativeHeapFreeSize;
		long nativeHeapThreshold = memoryInfo.threshold;

		return new long[] { System.currentTimeMillis(), usedMemInBytes, nativeHeapFreeSize, nativeHeapThreshold };
	}







}
