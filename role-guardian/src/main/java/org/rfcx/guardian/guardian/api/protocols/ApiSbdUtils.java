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

public class ApiSbdUtils {

	public ApiSbdUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSbdUtils");

	private RfcxGuardian app;

	public boolean queueSbdToApiToSendImmediately(String msgBody) {
		return queueSbdToSend(null, msgBody);
	}

	public boolean queueSbdToSend(String sendAt, String msgBody) {

		try {
			String sbdSendAt = ((sendAt != null) && (sendAt.length() > 0) && (!sendAt.equalsIgnoreCase("0"))) ? ""+Long.parseLong(sendAt) : ""+System.currentTimeMillis();
			String sbdMsgBody = (msgBody != null) ? msgBody : "";
			String sbdMsgUrlBlob = TextUtils.join("|", new String[]{ sbdSendAt, RfcxComm.urlEncode(sbdMsgBody) });

			Cursor sbdQueueResponse = app.getResolver().query(
							RfcxComm.getUri("admin", "sbd_queue", sbdMsgUrlBlob),
							RfcxComm.getProjection("admin", "sbd_queue"),
							null, null, null);
			if (sbdQueueResponse != null) {
				sbdQueueResponse.close();
				return true;
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}




	public boolean sendSbdPing(String pingJson) {

		boolean isSent = false;

		if (areSbdApiMessagesAllowed()) {
			try {

				String groupId = app.apiSegmentUtils.constructSegmentsGroupForQueue("png", "sbd", pingJson, null);

				app.apiSegmentUtils.queueSegmentsForDispatch(groupId);

				isSent = true;

			} catch (Exception e) {

				RfcxLog.logExc(logTag, e, "sendSbdPing");
				handleSbdPingPublicationExceptions(e);

			}
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
////                    app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle", app.getResolver());
////                    this.inFlightCheckInAttemptCounter = 0;
////                }
//			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "handleSbdPingPublicationExceptions");
		}
	}


	private boolean areSbdApiMessagesAllowed() {

		if (	(app != null)
			&& 	ArrayUtils.doesStringArrayContainString(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(","), "sbd")
		) {
			return true;
		}
		Log.d(logTag, "SBD API interaction blocked.");
		return false;
	}


}
