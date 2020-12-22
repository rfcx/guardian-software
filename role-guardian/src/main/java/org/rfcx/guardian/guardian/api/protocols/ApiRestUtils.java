package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.text.TextUtils;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.network.HttpPostMultipart;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class ApiRestUtils {

	public ApiRestUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		this.httpGet = new HttpGet(context, RfcxGuardian.APP_ROLE);
		this.httpPost = new HttpPostMultipart();
		setHttpHeaders();
		setHttpTimeouts();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiRestUtils");

	private RfcxGuardian app;

	HttpGet httpGet;
	HttpPostMultipart httpPost;

	private void setHttpTimeouts() {
//		this.httpGet.setTimeOuts();
//		this.httpPost.setTimeOuts();
	}

	private void setHttpHeaders() {
		List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/"+app.rfcxGuardianIdentity.getGuid() });
		rfcxAuthHeaders.add(new String[] { "x-auth-token", app.rfcxGuardianIdentity.getAuthToken() });
		this.httpGet.setCustomHttpHeaders(rfcxAuthHeaders);
		this.httpPost.setCustomHttpHeaders(rfcxAuthHeaders);
	}

	private String apiRequestUrl(boolean includeTimestampQueryParam) {

		StringBuilder requestUrl = new StringBuilder();

		requestUrl.append(app.rfcxPrefs.getPrefAsString("api_rest_protocol")).append("://");
		requestUrl.append(app.rfcxPrefs.getPrefAsString("api_rest_host"));

		// placeholder for path
		String requestPath = "/v2/guardians/"+app.rfcxGuardianIdentity.getGuid()+"/software/all";

		requestUrl.append(requestPath);

		List<String> queryParams = new ArrayList<String>();
		if (includeTimestampQueryParam) { queryParams.add("timestamp="+System.currentTimeMillis()); }

		if (queryParams.size() > 0) {
			requestUrl.append("?").append(TextUtils.join("&", queryParams));
		}

		return requestUrl.toString();

	}


}
