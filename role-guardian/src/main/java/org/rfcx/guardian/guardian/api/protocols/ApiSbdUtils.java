package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

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




	public boolean sendSbdPing(String pingJson) {

		boolean isSent = false;

		try {
			// String pingJson = app.apiPingJsonUtils.buildPingJson(includeAllExtraFields, includeExtraFields, false);
			//Log.d(logTag, pingJson);
			//		publishMessageOnConfirmedConnection(this.mqttTopic_Publish_Ping, 1,false, packageMqttPingPayload());
			isSent = true;

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e, "sendSbdPing");
			handleSbdPingPublicationExceptions(e);

		}

		return isSent;
	}

	private void handleSbdPingPublicationExceptions(Exception inputExc) {

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
