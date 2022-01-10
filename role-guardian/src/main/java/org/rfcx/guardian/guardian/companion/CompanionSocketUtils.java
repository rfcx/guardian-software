package org.rfcx.guardian.guardian.companion;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.io.InputStream;

public class CompanionSocketUtils {

	public CompanionSocketUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		this.socketUtils = new SocketUtils();
		this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.JSON);
	}

	private static final String[] includePingFields = new String[] {
			"battery", "instructions", "prefs_full", "software", "library", "device", "companion", "swm"
	};

	private static final String[] excludeFromLogs = new String[] { "prefs" };

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionSocketUtils");

	private RfcxGuardian app;
	public SocketUtils socketUtils;
	private String pingJson = (new JSONObject()).toString();


	public JSONObject getCompanionPingJsonObj() {
		JSONObject companionObj = new JSONObject();
		try {

			JSONObject guardianObj = new JSONObject();
			guardianObj.put("guid", app.rfcxGuardianIdentity.getGuid());
			guardianObj.put("name", app.rfcxGuardianIdentity.getName());

			companionObj.put("guardian", guardianObj);

			companionObj.put("is_registered", app.isGuardianRegistered());

			companionObj.put("checkin", getLatestAllSentCheckInType());

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return companionObj;
	}

	private JSONObject getLatestAllSentCheckInType() throws JSONException {
		JSONObject checkIn = new JSONObject();

		String mqtt = app.apiCheckInUtils.getLastCheckinDateTime();
		JSONObject mqttObj = new JSONObject();
		mqttObj.put("created_at", mqtt);
		if (mqtt.length() > 0) {
			checkIn.put("mqtt", mqttObj);
		}

		JSONArray sms = RfcxComm.getQuery(
				"admin",
				"sms_latest",
				null,
				app.getContentResolver());
		if (sms.length() > 0) {
			checkIn.put("sms", sms.getJSONObject(0));
		}

		JSONArray sbd = RfcxComm.getQuery(
				"admin",
				"sbd_latest",
				null,
				app.getContentResolver());
		if (sbd.length() > 0) {
			checkIn.put("sbd", sbd.getJSONObject(0));
		}

		JSONArray swm = RfcxComm.getQuery(
				"admin",
				"swm_latest",
				null,
				app.getContentResolver());
		if (swm.length() > 0) {
			checkIn.put("swm", sbd.getJSONObject(0));
		}

		return checkIn;
	}

	public void updatePingJson(boolean printJsonToLogs) {
		try {
			pingJson =  app.apiPingJsonUtils.buildPingJson(false, includePingFields, 0, printJsonToLogs, excludeFromLogs, false);
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e, "updatePingJson");
		}
	}

	public boolean sendSocketPing() {
		return this.socketUtils.sendJson(pingJson, areSocketInteractionsAllowed() );
	}


	private boolean areSocketInteractionsAllowed() {

		if (	(app != null)
				&&	socketUtils.isServerRunning
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
