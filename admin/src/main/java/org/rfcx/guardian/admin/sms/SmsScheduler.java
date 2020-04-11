package org.rfcx.guardian.admin.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceCPU;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SmsScheduler {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SmsScheduler.class);

	public static boolean addScheduledSmsToQueue(long sendAtOrAfter, String sendTo, String msgBody, Context context) {

		boolean isQueued = false;

		if ((sendTo != null) && (msgBody != null) && !sendTo.equalsIgnoreCase("") && !msgBody.equalsIgnoreCase("")) {

			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

			String msgId = DeviceSmsUtils.generateMessageId();

			app.smsMessageDb.dbSmsQueued.insert(sendAtOrAfter, sendTo, msgBody, msgId);

			Log.w(logTag, "SMS Queued (ID " + msgId + "): To " + sendTo + " at " + sendAtOrAfter + ": \"" + msgBody + "\"");
		}

		return isQueued;
	}

	public static boolean addImmediateSmsToQueue(String sendTo, String msgBody, Context context) {

		return addScheduledSmsToQueue(System.currentTimeMillis(), sendTo, msgBody, context);

	}

}
