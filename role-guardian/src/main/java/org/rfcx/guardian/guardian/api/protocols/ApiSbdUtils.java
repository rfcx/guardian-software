package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ApiSbdUtils {

	public ApiSbdUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSbdUtils");

	private RfcxGuardian app;

	public void queueSbdToApiToSendImmediately(String msgBody) {
		queueSbdToSend(null, null, msgBody);
	}

	public void queueSbdToSendImmediately(String sendTo, String msgBody) {
		queueSbdToSend(null, sendTo, msgBody);
	}

	public void queueSbdToSend(String sendAt, String sendTo, String msgBody) {

		try {
			String sbdSendAt = ((sendAt != null) && (sendAt.length() > 0) && (!sendAt.equalsIgnoreCase("0"))) ? ""+Long.parseLong(sendAt) : ""+System.currentTimeMillis();
			String sbdSendTo = ((sendTo != null) && (sendTo.length() > 0)) ? sendTo : app.rfcxPrefs.getPrefAsString("api_sms_address");
			String sbdMsgBody = (msgBody != null) ? msgBody : "";
			String sbdMsgUrlBlob = TextUtils.join("|", new String[]{ sbdSendAt, RfcxComm.urlEncode(sbdSendTo), RfcxComm.urlEncode(sbdMsgBody) });

			Cursor sbdQueueContentProviderResponse =
					app.getApplicationContext().getContentResolver().query(
							RfcxComm.getUri("satellite", "sbd_queue", sbdMsgUrlBlob),
							RfcxComm.getProjection("satellite", "sbd_queue"),
							null, null, null);
			sbdQueueContentProviderResponse.close();

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	}





}
