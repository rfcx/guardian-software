package org.rfcx.guardian.guardian.file;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

public class FileSocketUtils {

    public FileSocketUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.socketUtils = new SocketUtils();
        this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.FILE);
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "FileSocketUtils");

    private final RfcxGuardian app;
    public SocketUtils socketUtils;

    private boolean isReading = false;
    private JSONObject pingObj = null;

    public boolean sendDownloadResult(String result) {
        return this.socketUtils.sendJson(result, areSocketInteractionsAllowed());
    }

    public boolean sendPingCheckingConnection() {
        return this.socketUtils.sendJson(getPingObject().toString(), areSocketInteractionsAllowed());
    }

    public void setPingObject() {
        pingObj = new JSONObject();
        try {
            pingObj.put("admin", false);
            pingObj.put("classify", false);
            pingObj.put("guardian", false);
            pingObj.put("updater", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void resetPingObject() {
        this.pingObj = null;
    }

    public JSONObject getPingObject() {
        return this.pingObj;
    }

    private boolean areSocketInteractionsAllowed() {

        if ((app != null) && socketUtils.isServerRunning) {
            return true;
        }
        Log.d(logTag, "FileSocket interaction blocked.");
        return false;
    }

    public boolean isSocketServerEnablable(boolean verboseLogging, RfcxPrefs rfcxPrefs) {

        boolean prefsEnableSocketServer = rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_FILE_SOCKET);

        String prefsWifiFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
        boolean isWifiEnabled = prefsWifiFunction.equals("hotspot") || prefsWifiFunction.equals("client");

        String prefsBluetoothFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION);
        boolean isBluetoothEnabled = prefsBluetoothFunction.equals("pan");

        if (verboseLogging && prefsEnableSocketServer && !isWifiEnabled && !isBluetoothEnabled) {
            Log.e(logTag, "Socket Server could not be enabled because '" + RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION + "' and '" + RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION + "' are set to off.");
        }

        return prefsEnableSocketServer && (isWifiEnabled || isBluetoothEnabled);
    }

    public void startServer() {

        socketUtils.serverThread = new Thread(() -> {
            Looper.prepare();
            try {
                socketUtils.serverSetup();
                while (true) {
                    InputStream socketInput = socketUtils.socketSetup();
                    if (socketInput != null) {
                        InputStream fileInput = socketUtils.streamFileSetup(socketInput);
                        StringBuilder fileName = new StringBuilder();

                        //read until reach '|'
                        if (!isReading) {
                            isReading = true;

                            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

                            int bRead;
                            byte[] buffer = new byte[8192];

                            while ((bRead = fileInput.read(buffer, 0, buffer.length)) != -1) {
                                if (Character.toString((char) (buffer[0] & 0xFF)).equals("*")) {
                                    break;
                                }
                                byteOut.write(buffer, 0, bRead);
                            }

                            byte[] fullRead = byteOut.toByteArray();

                            int derimeter = -1;
                            int count = 0;
                            while (true) {
                                char chr = (char) (fullRead[count] & 0xFF);
                                if (Character.toString(chr).equals("|")) {
                                    break;
                                }
                                count++;
                                derimeter = count;
                                fileName.append(chr);
                            }

                            if (derimeter != -1) {
                                InputStream fullInput = new ByteArrayInputStream(Arrays.copyOfRange(fullRead, count + 1, fullRead.length));
                                boolean result = writeStreamToDisk(fullInput, fileName.toString());
                                isReading = false;
                                if (result) {
                                    String role = fileName.toString().split("-")[0];
                                    this.pingObj.remove(role);
                                    this.pingObj.put(role, true);
                                }
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                if (!e.getMessage().equalsIgnoreCase("Socket closed")) {
                    RfcxLog.logExc(logTag, e);
                }
            }
            Looper.loop();
        });
        socketUtils.serverThread.start();
        socketUtils.isServerRunning = true;
    }

    private boolean writeStreamToDisk(InputStream body, String fullFileName) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory().toString() + "/rfcx/apk", "softwares");
            if (!dir.exists()) {
                dir.mkdir();
            }
            FileOutputStream output = null;
            File file = new File(dir, fullFileName);

            try  {
                output = new FileOutputStream(file);
                byte[] buffer = new byte[8192]; // or other buffer size
                int read;

                while ((read = body.read(buffer)) != -1) {
                    if (read > 0) {
                        output.write(buffer, 0, read);
                    }
                }

                output.flush();
                return true;
            } catch (IOException e) {
                RfcxLog.logExc(logTag, e);
                return false;
            } finally {
                if (body != null) {
                    body.close();
                }
                if (output != null) {
                    output.close();
                }
            }
        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
            return false;
        }
    }
}
