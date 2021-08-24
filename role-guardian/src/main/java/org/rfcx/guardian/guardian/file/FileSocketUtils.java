package org.rfcx.guardian.guardian.file;

import android.content.Context;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FileSocketUtils {

    public FileSocketUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.socketUtils = new SocketUtils();
        this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.FILE);
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "FileSocketUtils");

    private final RfcxGuardian app;
    public SocketUtils socketUtils;
    private final ArrayList<String> notCombinedBytes = new ArrayList<>();

    public boolean sendDownloadResult(String result) {
        return this.socketUtils.sendJson(result, areSocketInteractionsAllowed());
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
            Log.e( logTag, "Socket Server could not be enabled because '"+RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION+"' and '"+RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION+"' are set to off.");
        }

        return prefsEnableSocketServer && (isWifiEnabled || isBluetoothEnabled);
    }

    private void processReceivedJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String type = json.getString("type");
            if (type.equals("apk")) {
                processAPKTypeMessage(json);
            }
        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private void processAPKTypeMessage(JSONObject json) throws JSONException {
        String role = json.getString("role");
        String version = json.getString("version");
        int amount = json.getInt("amount"); // amount of role to get
        int index = json.getInt("index"); // index of byte part of APK
        int size = json.getInt("size"); // size of all index of APK
        String bytes = json.getString("bytes"); // bytes of part of APK

        JSONObject resultJson = new JSONObject();

        if (index <= size) {
            notCombinedBytes.add(bytes);
        }
        if (index == size) {
            StringBuilder sb = new StringBuilder();
            for (String str : notCombinedBytes) {
                sb.append(str);
            }
            notCombinedBytes.clear();
            ByteArrayInputStream apkInBytes = new ByteArrayInputStream(stringToByteArray(sb.toString()));
            boolean writeResult = writeStreamToDisk(apkInBytes, role, version);
            resultJson.put(role, writeResult);
        }

        if (resultJson.length() == amount) {
            socketUtils.sendJson(resultJson.toString(), areSocketInteractionsAllowed());
        }
    }

    private byte[] stringToByteArray(String str) {
        return Base64.decode(str, Base64.URL_SAFE);
    }

    public void startServer() {

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
    }

    private boolean writeStreamToDisk(InputStream body, String role, String version) {
        try {
            File dir = new File(app.getApplicationContext().getExternalFilesDir(null), "softwares");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, role + "-" + version + "-release.apk.gz");
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                inputStream = body;
                outputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                }

                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
