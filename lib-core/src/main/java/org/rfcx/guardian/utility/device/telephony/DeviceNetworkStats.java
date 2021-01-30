package org.rfcx.guardian.utility.device.telephony;

import android.net.TrafficStats;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceNetworkStats {
	
	public DeviceNetworkStats(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceNetworkStats");
	}
	
	private String logTag;

	private long networkStatsStart = System.currentTimeMillis();
	private long networkStatsEnd = System.currentTimeMillis();
	private long networkStatsReceived = 0;
	private long networkStatsSent = 0;
	private long networkStatsReceivedTotal = 0;
	private long networkStatsSentTotal = 0;
	
	public long[] getDataTransferStatsSnapshot() {

		boolean isFirstRun = ((this.networkStatsReceivedTotal == 0) && (this.networkStatsSentTotal == 0));
		
		long mobileRxBytes = TrafficStats.getMobileRxBytes();
		long mobileTxBytes = TrafficStats.getMobileTxBytes();

		this.networkStatsStart = this.networkStatsEnd;
		this.networkStatsEnd = System.currentTimeMillis();
		this.networkStatsReceived = mobileRxBytes - this.networkStatsReceivedTotal;
		this.networkStatsSent = mobileTxBytes - this.networkStatsSentTotal;
		this.networkStatsReceivedTotal = mobileRxBytes;
		this.networkStatsSentTotal = mobileTxBytes;
				
		return new long[] {
				this.networkStatsStart,
				this.networkStatsEnd, 
				this.networkStatsReceived, 
				this.networkStatsSent, 
				this.networkStatsReceivedTotal, 
				this.networkStatsSentTotal,
				(isFirstRun) ? 1 : 0
			};
	}
	
}
