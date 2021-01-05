package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.network.HttpPostMultipart;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

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
		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/"+app.rfcxGuardianIdentity.getGuid() });
		rfcxAuthHeaders.add(new String[] { "x-auth-token", app.rfcxGuardianIdentity.getAuthToken() });
		this.httpGet.setCustomHttpHeaders(rfcxAuthHeaders);
		this.httpPost.setCustomHttpHeaders(rfcxAuthHeaders);
	}

	private String apiRequestUrl(String requestPath, boolean includeTimestampQueryParam) {

		StringBuilder requestUrl = new StringBuilder();

		requestUrl.append(app.rfcxPrefs.getPrefAsString("api_rest_protocol")).append("://");
		requestUrl.append(app.rfcxPrefs.getPrefAsString("api_rest_host"));

		requestUrl.append(requestPath);

		List<String> queryParams = new ArrayList<String>();
		if (includeTimestampQueryParam) { queryParams.add("timestamp="+System.currentTimeMillis()); }

		if (queryParams.size() > 0) {
			requestUrl.append("?").append(TextUtils.join("&", queryParams));
		}

		return requestUrl.toString();

	}

	private boolean areRestApiRequestsAllowed(boolean printLoggingFeedbackIfNotAllowed) {
		if (app != null) {
			if (ArrayUtils.doesStringArrayContainString(app.rfcxPrefs.getPrefAsString("api_protocol_escalation_order").split(","), "rest")) {
				if (app.deviceConnectivity.isConnected()) {

//				long timeElapsedSinceLastUpdateRequest = System.currentTimeMillis() - this.lastUpdateRequestTriggered;
//				if (timeElapsedSinceLastUpdateRequest > (minimumAllowedIntervalBetweenUpdateRequests * (60 * 1000))) {
//					this.lastUpdateRequestTriggered = System.currentTimeMillis();
//					return true;
//				} else if (printLoggingFeedbackIfNotAllowed) {
//					Log.e(logTag, "Update Request blocked b/c minimum allowed interval has not yet elapsed"
//							+" - Elapsed: " + DateTimeUtils.milliSecondDurationAsReadableString(timeElapsedSinceLastUpdateRequest)
//							+" - Required: " + minimumAllowedIntervalBetweenUpdateRequests + " minutes");
//				}
					return true;

				} else {
					Log.d(logTag, "Rest Api request blocked because there is no internet connectivity.");
				}
			} else {
				Log.d(logTag, "Rest Api request blocked because this protocol is explicitly disabled in preferences..");
			}
		}
		return false;
	}


	public boolean sendRestPing(String pingJson) {

		boolean isSent = false;

		if (areRestApiRequestsAllowed(true)) {

			try {
				List<String[]> postParams = new ArrayList<>();
				postParams.add(new String[] { "meta", StringUtils.stringToGZippedBase64( pingJson ) });

				String pingResponse = httpPost.doMultipartPost( apiRequestUrl(restUrlPath_Ping, false), postParams, null);

				if (pingResponse != null) {
					app.apiCommandUtils.processApiCommandJson(pingResponse);
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
			RfcxLog.logExc(logTag, e, "handleRestPingPublicationExceptions");
		}
	}



}
