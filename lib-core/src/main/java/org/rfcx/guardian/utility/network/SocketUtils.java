package org.rfcx.guardian.utility.network;

import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SocketUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "SocketUtils");

	private Socket socket = null;
	private ServerSocket serverSocket = null;
	public Thread serverThread = null;
	private DataInputStream streamInput = null;
	private DataOutputStream streamOutput = null;
	private int socketServerPort;

	public boolean isServerRunning = false;

	public void setSocketPort(int socketServerPort) {
		this.socketServerPort = socketServerPort;
	}

	private void publishText(String textToPublish) throws IOException {
		this.streamOutput.writeUTF(textToPublish);
		this.streamOutput.flush();
	}

	public void serverSetup() throws IOException {
		this.serverSocket = new ServerSocket(this.socketServerPort);
		this.serverSocket.setReuseAddress(true);
	}

	public InputStream socketSetup() throws IOException {
		this.socket = this.serverSocket.accept();
		this.socket.setTcpNoDelay(true);
		return this.socket.getInputStream();
	}

	public String streamSetup(InputStream socketInput) throws IOException {
		this.streamInput = new DataInputStream(socketInput);
		String jsonStr = this.streamInput.readUTF();
		this.streamOutput = new DataOutputStream(this.socket.getOutputStream());
		return jsonStr;
	}

	public void stopServer() {

		//	if (isServerRunning) {
		try {

			if (serverThread != null) { serverThread.interrupt(); }
			if (streamInput != null) { streamInput.close(); }
			if (streamOutput != null) { streamOutput.flush(); streamOutput.close(); }
			if (socket != null) { socket.close(); }
			if (serverSocket != null) { serverSocket.close(); }

		} catch (IOException e) {
			if (!e.getMessage().equalsIgnoreCase("Socket closed")) {
				RfcxLog.logExc(logTag, e);
			}
		}
		
		serverThread = null;
		streamInput = null;
		streamOutput = null;
		socket = null;
		serverSocket = null;

		isServerRunning = false;
//		}
	}



	public boolean isSocketServerEnabled(boolean verboseLogging, RfcxPrefs rfcxPrefs) {

		boolean prefsAdminEnableSocketServer = rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SOCKET_SERVER);
		String prefsAdminWifiFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
		boolean isWifiEnabled = prefsAdminWifiFunction.equals("hotspot") || prefsAdminWifiFunction.equals("client");

		if (verboseLogging && prefsAdminEnableSocketServer && !isWifiEnabled) {
			Log.e( logTag, "Socket Server could not be enabled because '"+RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION+"' is set to off.");
		}

		return prefsAdminEnableSocketServer && isWifiEnabled;
	}




	public boolean sendSocketJson(String jsonStr, boolean areSocketInteractionsAllowed) {

		boolean isSent = false;

		if (areSocketInteractionsAllowed) {
			try {

				publishText(jsonStr);
				isSent = true;

			} catch (Exception e) {

				handleSocketJsonPublicationExceptions(e);
			}
		}

		return isSent;
	}



	private void handleSocketJsonPublicationExceptions(Exception inputExc) {

		try {
			String excStr = RfcxLog.getExceptionContentAsString(inputExc);

			// This is where we would put contingencies and reactions for various exceptions. See ApiMqttUtils for reference.

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "handleSocketPingPublicationExceptions");
		}
	}


}