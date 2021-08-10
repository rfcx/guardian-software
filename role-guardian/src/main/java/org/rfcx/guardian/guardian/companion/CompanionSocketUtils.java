package org.rfcx.guardian.guardian.companion;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.io.InputStream;

public class CompanionSocketUtils {

	public CompanionSocketUtils(Context context, int socketServerPort) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		this.socketUtils = new SocketUtils();
		this.socketUtils.setSocketPort(socketServerPort);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionSocketUtils");

	private RfcxGuardian app;
	public SocketUtils socketUtils;

	private static final String[] includePingFields = new String[] {
			"battery", "instructions", "prefs", "software", "library", "device", "guardian"
	};

	private String pingJson = (new JSONObject()).toString();

	public void updatePingJson() {
		try {
			pingJson =  app.apiPingJsonUtils.buildPingJson(false, includePingFields, 0);
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e, "updatePingJson");
		}
	}

	public boolean sendSocketPing() {
		return this.socketUtils.sendSocketJson(pingJson, areSocketInteractionsAllowed() );
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