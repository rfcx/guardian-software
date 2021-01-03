package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ApiSmsUtils {

	public ApiSmsUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSmsUtils");

	private RfcxGuardian app;

	public void queueSmsToApiToSendImmediately(String msgBody) {
		queueSmsToSend(null, null, msgBody);
	}

	public void queueSmsToSendImmediately(String sendTo, String msgBody) {
		queueSmsToSend(null, sendTo, msgBody);
	}

	public void queueSmsToSend(String sendAt, String sendTo, String msgBody) {

		try {
			String smsSendAt = ((sendAt != null) && (sendAt.length() > 0) && (!sendAt.equalsIgnoreCase("0"))) ? ""+Long.parseLong(sendAt) : ""+System.currentTimeMillis();
			String smsSendTo = ((sendTo != null) && (sendTo.length() > 0)) ? sendTo : app.rfcxPrefs.getPrefAsString("api_sms_address");
			String smsMsgBody = (msgBody != null) ? msgBody : "";
			String smsMsgUrlBlob = TextUtils.join("|", new String[]{ smsSendAt, RfcxComm.urlEncode(smsSendTo), RfcxComm.urlEncode(smsMsgBody) });

			Cursor smsQueueContentProviderResponse =
					app.getApplicationContext().getContentResolver().query(
							RfcxComm.getUri("admin", "sms_queue", smsMsgUrlBlob),
							RfcxComm.getProjection("admin", "sms_queue"),
							null, null, null);
			smsQueueContentProviderResponse.close();

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	}



	public boolean sendSmsPing(String pingJson) {

		boolean isSent = false;

		try {

			String groupId = app.apiSegmentUtils.constructSegmentsGroupForQueue("png", "sms", pingJson, null);

			app.apiSegmentUtils.queueSegmentsForDispatch(groupId);

			isSent = true;

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e, "sendSmsPing");
			handleSmsPingPublicationExceptions(e);

		}

		return isSent;
	}

	private void handleSmsPingPublicationExceptions(Exception inputExc) {

		try {
			String excStr = RfcxLog.getExceptionContentAsString(inputExc);

//			if (excStr.contains("Too many publishes in progress")) {
////                app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
////                app.rfcxServiceHandler.triggerService("ApiCheckInJob", true);
//
//			} else if (	excStr.contains("UnknownHostException")
//					||	excStr.contains("Broken pipe")
//					||	excStr.contains("Timed out waiting for a response from the server")
//					||	excStr.contains("No route to host")
//					||	excStr.contains("Host is unresolved")
//			) {
////                Log.i(logTag, "Connection has failed "+this.inFlightCheckInAttemptCounter +" times (max: "+this.inFlightCheckInAttemptCounterLimit +")");
////                app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
////                if (this.inFlightCheckInAttemptCounter >= this.inFlightCheckInAttemptCounterLimit) {
////                    Log.d(logTag, "Max Connection Failure Loop Reached: Airplane Mode will be toggled.");
////                    app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle", app.getApplicationContext().getContentResolver());
////                    this.inFlightCheckInAttemptCounter = 0;
////                }
//			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "handleSmsPingPublicationExceptions");
		}
	}

}
