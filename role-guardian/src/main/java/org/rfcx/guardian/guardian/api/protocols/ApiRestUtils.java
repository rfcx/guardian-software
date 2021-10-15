package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.network.HttpPostMultipart;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.List;

public class ApiRestUtils {

    public ApiRestUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.httpGet = new HttpGet(context, RfcxGuardian.APP_ROLE);
        this.httpPost = new HttpPostMultipart(context, RfcxGuardian.APP_ROLE);
        setHttpHeaders();
        setHttpTimeouts();

        this.restUrlPath_Ping = "/v2/guardians/" + app.rfcxGuardianIdentity.getGuid() + "/pings";
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiRestUtils");

    private RfcxGuardian app;

    HttpGet httpGet;
    HttpPostMultipart httpPost;

    private String restUrlPath_Ping;

    private void setHttpTimeouts() {
        this.httpGet.setTimeOuts(30000, 30000);
        this.httpPost.setTimeOuts(30000, 30000);
    }

    private void setHttpHeaders() {
        List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
        rfcxAuthHeaders.add(new String[]{"x-auth-user", "guardian/" + app.rfcxGuardianIdentity.getGuid()});
        rfcxAuthHeaders.add(new String[]{"x-auth-token", app.rfcxGuardianIdentity.getAuthToken()});
        this.httpGet.setCustomHttpHeaders(rfcxAuthHeaders);
        this.httpPost.setCustomHttpHeaders(rfcxAuthHeaders);
    }

    private String apiRequestUrl(String requestPath, boolean includeTimestampQueryParam) {

        StringBuilder requestUrl = new StringBuilder();

        requestUrl.append(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_REST_PROTOCOL)).append("://");
        requestUrl.append(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_REST_HOST));

        requestUrl.append(requestPath);

        List<String> queryParams = new ArrayList<String>();
        if (includeTimestampQueryParam) {
            queryParams.add("timestamp=" + System.currentTimeMillis());
        }

        if (queryParams.size() > 0) {
            requestUrl.append("?").append(TextUtils.join("&", queryParams));
        }

        return requestUrl.toString();

    }

    public boolean sendRestPing(String pingJson) {

        boolean isSent = false;

        if (areRestApiRequestsAllowed()) {
            try {
                List<String[]> postParams = new ArrayList<>();
                postParams.add(new String[]{"json", StringUtils.stringToGZipBase64(pingJson)});

                String pingResponse = httpPost.doMultipartPost(apiRequestUrl(restUrlPath_Ping, false), postParams, null);

                if (pingResponse != null) {
                    app.apiCommandUtils.processApiCommandJson(pingResponse, "rest");
                    isSent = true;
                }

            } catch (Exception e) {

                RfcxLog.logExc(logTag, e, "sendRestPing");
                handleRestPingPublicationExceptions(e);

            }
        }

        return isSent;
    }


    private void handleRestPingPublicationExceptions(Exception inputExc) {

        try {
            String excStr = RfcxLog.getExceptionContentAsString(inputExc);

            // This is where we would put contingencies and reactions for various exceptions. See ApiMqttUtils for reference.

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "handleRestPingPublicationExceptions");
        }
    }

    private boolean areRestApiRequestsAllowed() {

        if ((app != null)
                && ArrayUtils.doesStringArrayContainString(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(","), "rest")
                && app.deviceConnectivity.isConnected()
        ) {
            return true;
        }
        Log.d(logTag, "REST API interaction blocked.");
        return false;
    }


}
