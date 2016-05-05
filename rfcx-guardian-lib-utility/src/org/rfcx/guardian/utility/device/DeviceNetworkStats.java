package org.rfcx.guardian.utility.device;

import java.util.Date;

import android.location.LocationManager;
import android.net.TrafficStats;

public class DeviceNetworkStats {
	
	private static final String TAG = "Rfcx-Utils-"+DeviceNetworkStats.class.getSimpleName();
	
	private Date networkStatsStart = new Date();
	private Date networkStatsEnd = new Date();
	private long networkStatsReceived = 0;
	private long networkStatsSent = 0;
	private long networkStatsReceivedTotal = 0;
	private long networkStatsSentTotal = 0;
	
	public long[] updateDataTransferStats() {
		
		long mobileRxBytes = TrafficStats.getMobileRxBytes();
		long mobileTxBytes = TrafficStats.getMobileTxBytes();
		
		this.networkStatsStart = this.networkStatsEnd;
		this.networkStatsEnd = new Date();
		this.networkStatsReceived = mobileRxBytes - this.networkStatsReceivedTotal;
		this.networkStatsSent = mobileTxBytes - this.networkStatsSentTotal;
		this.networkStatsReceivedTotal = mobileRxBytes;
		this.networkStatsSentTotal = mobileTxBytes;
				
		return new long[] { this.networkStatsStart.getTime(), this.networkStatsEnd.getTime(), this.networkStatsReceived, this.networkStatsSent, this.networkStatsReceivedTotal, this.networkStatsSentTotal };
	}
	
}
