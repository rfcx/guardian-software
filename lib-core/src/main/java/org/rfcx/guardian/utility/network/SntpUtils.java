package org.rfcx.guardian.utility.network;

import android.os.SystemClock;
import android.util.Log;

import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.vendor.SntpClient;

public class SntpUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "SntpUtils");

	public static long[] getSntpClockValues(boolean isConnected, String ntpHost) {

		long[] sntpClockValues = new long[] {};

		if (!isConnected) {

			Log.v(logTag, "SNTP sync is not possible because there is currently no network connectivity.");

		} else {

			SntpClient sntpClient = new SntpClient();

			if (sntpClient.requestTime(ntpHost, 15000) && sntpClient.requestTime(ntpHost, 15000)) {
				long nowSystem = System.currentTimeMillis();
				long nowSntp = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();

				sntpClockValues = new long[] { nowSntp, nowSystem };

				String nowSystemStr = DateTimeUtils.getDateTime(nowSystem) +"."+ (""+(1000+nowSystem-Math.round(1000*Math.floor(nowSystem/1000)))).substring(1);

				Log.v(logTag, "DateTime Sync: System time is "+nowSystemStr.substring(1+nowSystemStr.indexOf(" "))
						+" —— "+Math.abs(nowSystem-nowSntp)+"ms "+((nowSystem >= nowSntp) ? "ahead of" : "behind")+" SNTP value.");
			}
		}

		return sntpClockValues;

	}



}
