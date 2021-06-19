package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.database.Cursor;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class ApiSmsUtils {

	public ApiSmsUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSmsUtils");

	private RfcxGuardian app;

	public boolean queueSmsToApiToSendImmediately(String msgBody) {
		return queueSmsToSend(null, null, msgBody);
	}

	public boolean queueSmsToSendImmediately(String sendTo, String msgBody) {
		return queueSmsToSend(null, sendTo, msgBody);
	}

	public boolean queueSmsToSend(String sendAt, String sendTo, String msgBody) {

		try {
			String smsSendAt = ((sendAt != null) && (sendAt.length() > 0) && (!sendAt.equalsIgnoreCase("0"))) ? "" + Long.parseLong(sendAt) : "" + System.currentTimeMillis();
			String smsSendTo = ((sendTo != null) && (sendTo.length() > 0)) ? sendTo : app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SMS_ADDRESS);
			String smsMsgBody = (msgBody != null) ? msgBody : "";
			String smsMsgUrlBlob = TextUtils.join("|", new String[]{smsSendAt, RfcxComm.urlEncode(smsSendTo), RfcxComm.urlEncode(smsMsgBody)});

			Cursor smsQueueResponse = app.getResolver().query(
					RfcxComm.getUri("admin", "sms_queue", smsMsgUrlBlob),
					RfcxComm.getProjection("admin", "sms_queue"),
					null, null, null);
			if (smsQueueResponse != null) {
				smsQueueResponse.close();
				return true;
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}


	public boolean sendSmsPing(String pingJson) {

		boolean isSent = false;

		if (areSmsApiMessagesAllowed()) {
			try {

				String groupId = app.apiSegmentUtils.constructSegmentsGroupForQueue("png", "sms", pingJson, null);

				app.apiSegmentUtils.queueSegmentsForDispatch(groupId);

				isSent = true;

			} catch (Exception e) {

				RfcxLog.logExc(logTag, e, "sendSmsPing");
				handleSmsPingPublicationExceptions(e);

			}
		}

		return isSent;
	}

	private void handleSmsPingPublicationExceptions(Exception inputExc) {

		try {
			String excStr = RfcxLog.getExceptionContentAsString(inputExc);

			// This is where we would put contingencies and reactions for various exceptions. See ApiMqttUtils for reference.

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "handleSmsPingPublicationExceptions");
		}
	}


	private boolean areSmsApiMessagesAllowed() {

		if (	(app != null)
			&&	ArrayUtils.doesStringArrayContainString(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(","), "sms")
		) {
			TelephonyManager telephonyManager = (TelephonyManager) app.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			assert telephonyManager != null;
			if (telephonyManager.getNetworkOperator() == null || !telephonyManager.getNetworkOperator().equals("")) {
				return true;
			}
		}
		Log.d(logTag, "SMS API interaction blocked.");
		return false;
	}

}