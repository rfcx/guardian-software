package org.rfcx.guardian.utility.device.telephony;

import android.net.TrafficStats;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceNetworkStats {
	
	public DeviceNetworkStats(String appRole) {
		logTag = RfcxLog.generateLogTag(appRole, "DeviceNetworkStats");
	}
	
	private String logTag;

	private long statsStartAt = System.currentTimeMillis();
	private long statsEndsAt = System.currentTimeMillis();

	private long mobileRxNow = 0;
	private long mobileTxNow = 0;
	private long mobileRxTotal = 0;
	private long mobileTxTotal = 0;

	private long networkRxNow = 0;
	private long networkTxNow = 0;
	private long networkRxTotal = 0;
	private long networkTxTotal = 0;
	
	public long[] getDataTransferStatsSnapshot() {

		boolean isFirstRun = ((networkRxTotal == 0) && (networkTxTotal == 0));

		long mobileRxBytes = TrafficStats.getMobileRxBytes();
		long mobileTxBytes = TrafficStats.getMobileTxBytes();
		mobileRxNow = mobileRxBytes - mobileRxTotal;
		mobileTxNow = mobileTxBytes - mobileTxTotal;
		mobileRxTotal = mobileRxBytes;
		mobileTxTotal = mobileTxBytes;

		long networkRxBytes = TrafficStats.getTotalRxBytes() - mobileRxBytes;
		long networkTxBytes = TrafficStats.getTotalTxBytes() - mobileTxBytes;
		networkRxNow = networkRxBytes - networkRxTotal;
		networkTxNow = networkTxBytes - networkTxTotal;
		networkRxTotal = networkRxBytes;
		networkTxTotal = networkTxBytes;

		statsStartAt = statsEndsAt;
		statsEndsAt = System.currentTimeMillis();

		return new long[] {
				statsStartAt,
				statsEndsAt,
				mobileRxNow,
				mobileTxNow,
				mobileRxTotal,
				mobileTxTotal,
				networkRxNow,
				networkTxNow,
				networkRxTotal,
				networkTxTotal,
				(isFirstRun) ? 1 : 0
			};
	}
	
}
