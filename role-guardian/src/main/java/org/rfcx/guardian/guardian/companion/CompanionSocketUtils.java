package org.rfcx.guardian.guardian.companion;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.io.InputStream;

public class CompanionSocketUtils {

    private static final String[] includePingFields = new String[]{
            "battery", "instructions", "prefs_full", "software", "library", "device", "companion"
    };
    private static final String[] excludeFromLogs = new String[]{"prefs"};
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionSocketUtils");
    public SocketUtils socketUtils;
    private RfcxGuardian app;
    private String pingJson = (new JSONObject()).toString();
    public CompanionSocketUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.socketUtils = new SocketUtils();
        this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.JSON);
    }

    public JSONObject getCompanionPingJsonObj() {
        JSONObject companionObj = new JSONObject();
        try {

            JSONObject guardianObj = new JSONObject();
            guardianObj.put("guid", app.rfcxGuardianIdentity.getGuid());
            guardianObj.put("name", app.rfcxGuardianIdentity.getName());

            companionObj.put("is_registered", app.isGuardianRegistered());

            companionObj.put("guardian", guardianObj);

        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);
        }
        return companionObj;
    }

    public void updatePingJson(boolean printJsonToLogs) {
        try {
            pingJson = app.apiPingJsonUtils.buildPingJson(false, includePingFields, 0, printJsonToLogs, excludeFromLogs);
        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e, "updatePingJson");
        }
    }

    public boolean sendSocketPing() {
        return this.socketUtils.sendJson(pingJson, areSocketInteractionsAllowed());
    }


    private boolean areSocketInteractionsAllowed() {

        if ((app != null)
                && socketUtils.isServerRunning
        ) {
            return true;
        }
        Log.d(logTag, "Socket interaction blocked.");
        return false;
    }

    private void processReceivedJson(String jsonStr) {
        app.apiCommandUtils.processApiCommandJson(jsonStr, "socket");
    }


    public void startServer() {

//		if (!socketUtils.isServerRunning) {
        socketUtils.serverThread = new Thread(() -> {
            Looper.prepare();
            try {
                socketUtils.serverSetup();
                while (true) {
                    InputStream socketInput = socketUtils.socketSetup();
                    if (socketInput != null) {
                        String jsonStr = socketUtils.streamSetup(socketInput);
                        if (jsonStr != null) {
                            processReceivedJson(jsonStr);
                        }
                    }
                }
            } catch (IOException e) {
                if (!e.getMessage().equalsIgnoreCase("Socket closed")) {
                    RfcxLog.logExc(logTag, e);
                }
            }
            Looper.loop();
        });
        socketUtils.serverThread.start();
        socketUtils.isServerRunning = true;
        //	}
    }

}
