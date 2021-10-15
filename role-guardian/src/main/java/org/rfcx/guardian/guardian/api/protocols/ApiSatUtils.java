package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class ApiSatUtils {

    public ApiSatUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSatUtils");

    private RfcxGuardian app;

    public boolean queueSatMsgToApiToSendImmediately(String msgBody, String satProtocol) {
        return queueSatMsgToSend(null, msgBody, satProtocol);
    }

    public boolean queueSatMsgToSend(String sendAt, String msgBody, String satProtocol) {

        try {
            String satSendAt = ((sendAt != null) && (sendAt.length() > 0) && (!sendAt.equalsIgnoreCase("0"))) ? "" + Long.parseLong(sendAt) : "" + System.currentTimeMillis();
            String satMsgBody = (msgBody != null) ? msgBody : "";
            String satMsgUrlBlob = TextUtils.join("|", new String[]{satSendAt, RfcxComm.urlEncode(satMsgBody)});

            Cursor satQueueResponse = app.getResolver().query(
                    RfcxComm.getUri("admin", satProtocol + "_queue", satMsgUrlBlob),
                    RfcxComm.getProjection("admin", satProtocol + "_queue"),
                    null, null, null);
            if (satQueueResponse != null) {
                satQueueResponse.close();
                return true;
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return false;
    }


    public boolean sendSatPing(String pingJson) {

        boolean isSent = false;

        if (areSatApiMessagesAllowed()) {
            try {

                String apiSatelliteProtocol = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL);

                String groupId = app.apiSegmentUtils.constructSegmentsGroupForQueue("png", apiSatelliteProtocol, pingJson, null);

                app.apiSegmentUtils.queueSegmentsForDispatch(groupId);

                isSent = true;

            } catch (Exception e) {

                RfcxLog.logExc(logTag, e, "sendSatPing");
                handleSatPingPublicationExceptions(e);

            }
        }

        return isSent;
    }

    private void handleSatPingPublicationExceptions(Exception inputExc) {

        try {
            String excStr = RfcxLog.getExceptionContentAsString(inputExc);

            // This is where we would put contingencies and reactions for various exceptions. See ApiMqttUtils for reference.

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "handleSatPingPublicationExceptions");
        }
    }


    private boolean areSatApiMessagesAllowed() {

        if ((app != null)
                && ArrayUtils.doesStringArrayContainString(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(","), "sat")
                && !app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("off")
        ) {
            return true;
        }
        Log.d(logTag, "Satellite API interaction blocked.");
        return false;
    }


}
