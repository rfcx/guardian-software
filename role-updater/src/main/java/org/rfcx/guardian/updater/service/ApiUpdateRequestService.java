package org.rfcx.guardian.updater.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiUpdateRequestService extends Service {

    public static final String SERVICE_NAME = "ApiUpdateRequest";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiUpdateRequestService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private ApiUpdateRequest apiUpdateRequest;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.apiUpdateRequest = new ApiUpdateRequest();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.apiUpdateRequest.start();
        } catch (IllegalThreadStateException e) {
            RfcxLog.logExc(logTag, e);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.runFlag = false;
        app.rfcxSvc.setRunState(SERVICE_NAME, false);
        this.apiUpdateRequest.interrupt();
        this.apiUpdateRequest = null;
    }

    private class ApiUpdateRequest extends Thread {

        public ApiUpdateRequest() {
            super("ApiUpdateRequestService-ApiUpdateRequest");
        }

        @Override
        public void run() {
            ApiUpdateRequestService apiUpdateRequestService = ApiUpdateRequestService.this;

            HttpGet httpGet = new HttpGet(app.getApplicationContext(), RfcxGuardian.APP_ROLE);
            // setting customized rfcx authentication headers (necessary for API access)
            List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
            rfcxAuthHeaders.add(new String[]{"x-auth-user", "guardian/" + app.rfcxGuardianIdentity.getGuid()});
            rfcxAuthHeaders.add(new String[]{"x-auth-token", app.rfcxGuardianIdentity.getAuthToken()});
            httpGet.setCustomHttpHeaders(rfcxAuthHeaders);

            try {
                if (app.deviceConnectivity.isConnected()) {
                    String getUrl = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_REST_PROTOCOL)
                            + "://"
                            + app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_REST_HOST)
                            + "/v2/guardians/" + app.rfcxGuardianIdentity.getGuid() + "/software/all"
                            + "?" + "timestamp=" + System.currentTimeMillis();

                    Log.d(logTag, "Time elapsed since last update request: " + DateTimeUtils.milliSecondDurationAsReadableString((System.currentTimeMillis() - app.apiUpdateRequestUtils.lastUpdateRequestTime)));

                    List<JSONObject> jsonResponse = null;
                    try {
                        jsonResponse = httpGet.getAsJsonList(getUrl);
                    } catch (Exception e) {
                        RfcxLog.logExc(logTag, e);
                    }

                    if (jsonResponse == null) {
                        Log.e(logTag, "Version API Update Request failed...");
                        app.apiUpdateRequestUtils.lastUpdateRequestTriggered = 0;
                    } else {
                        for (JSONObject jsonResponseItem : jsonResponse) {
                            String targetAppRole = jsonResponseItem.getString("role").toLowerCase();
                            if (!targetAppRole.equals(RfcxGuardian.APP_ROLE)) {
                                if (app.apiUpdateRequestUtils.apiUpdateRequestFollowUp(targetAppRole, jsonResponse)) {
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    Log.d(logTag, "Cancelled because there is no internet connectivity...");
                }
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            } finally {
                app.rfcxSvc.setRunState(SERVICE_NAME, false);
                app.rfcxSvc.stopService(SERVICE_NAME);
            }
        }
    }

}
