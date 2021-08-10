package org.rfcx.guardian.guardian.api.protocols;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ApiSocketUtils {

	public ApiSocketUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSocketUtils");

	private RfcxGuardian app;

	private Socket socket = null;
	private ServerSocket serverSocket = null;
	private Thread serverThread = null;
	private DataInputStream streamInput = null;
	private DataOutputStream streamOutput = null;

	private static final int socketServerPort = 9999;

	public boolean isServerRunning = false;

	private JSONObject pingJson = new JSONObject();


	public void updatePingJson() {
		try {
			pingJson = new JSONObject( app.apiPingJsonUtils.buildPingJson(true, new String[]{}, 0) );
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e, "updatePingJson");
		}
	}

	private boolean sendSocketPing(String pingJson) {

		boolean isSent = false;

		if (areSocketApiInteractionsAllowed()) {
			try {

				streamOutput.writeUTF(pingJson);
				streamOutput.flush();
				isSent = true;

			} catch (Exception e) {

				RfcxLog.logExc(logTag, e, "sendSocketPing");
				handleSocketPingPublicationExceptions(e);
			}
		}

		return isSent;
	}

	public boolean sendSocketPing() {
		return sendSocketPing(pingJson.toString());
	}

	private void handleSocketPingPublicationExceptions(Exception inputExc) {

		try {
			String excStr = RfcxLog.getExceptionContentAsString(inputExc);

			// This is where we would put contingencies and reactions for various exceptions. See ApiMqttUtils for reference.

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "handleSocketPingPublicationExceptions");
		}
	}

	private boolean areSocketApiInteractionsAllowed() {

		if (	(app != null)
				&&	isServerRunning

//				&&	ArrayUtils.doesStringArrayContainString(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).split(","), "rest")
//				&&	app.deviceConnectivity.isConnected()
		) {
			return true;
		}
		Log.d(logTag, "Socket API interaction blocked.");
		return false;
	}



	public void startServer() {

		if (!isServerRunning) {
			serverThread = new Thread(() -> {
				Looper.prepare();

				try {
					serverSocket = new ServerSocket(socketServerPort);
					serverSocket.setReuseAddress(true);

					while (true) {

						socket = serverSocket.accept();
						socket.setTcpNoDelay(true);

						InputStream socketInput = socket.getInputStream();
						if (socketInput != null) {

							streamInput = new DataInputStream(socketInput);
							String jsonCmdStr = streamInput.readUTF();

							streamOutput = new DataOutputStream(socket.getOutputStream());

							if (jsonCmdStr != null) {
								app.apiCommandUtils.processApiCommandJson(jsonCmdStr, "socket");
							}
						}
					}

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}

				Looper.loop();
			});

			serverThread.start();
			isServerRunning = true;
		}
	}


	public void stopServer() throws IOException {

		if (isServerRunning) {

			if (serverThread != null) { serverThread.interrupt(); }
			serverThread = null;

			if (streamInput != null) { streamInput.close(); }
			streamInput = null;

			if (streamOutput != null) {
				streamOutput.flush();
				streamOutput.close();
			}
			streamOutput = null;

			if (socket != null) { socket.close(); }
			socket = null;

			if (serverSocket != null) { serverSocket.close(); }
			serverSocket = null;

			isServerRunning = false;
		}
	}



	public boolean isSocketServerEnabled(boolean verboseLogging) {

		boolean prefsAdminEnableSocketServer = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SOCKET_SERVER);
		String prefsAdminWifiFunction = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
		boolean isWifiEnabled = prefsAdminWifiFunction.equals("hotspot") || prefsAdminWifiFunction.equals("client");

		if (verboseLogging && prefsAdminEnableSocketServer && !isWifiEnabled) {
			Log.e( logTag, "WiFi Socket Server could not be enabled because 'admin_wifi_function' is set to off.");
		}

		return prefsAdminEnableSocketServer && isWifiEnabled;
	}

}