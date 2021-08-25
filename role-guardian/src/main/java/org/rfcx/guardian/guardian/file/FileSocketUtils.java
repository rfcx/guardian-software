package org.rfcx.guardian.guardian.file;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class FileSocketUtils {

    public FileSocketUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.socketUtils = new SocketUtils();
        this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.FILE);
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "FileSocketUtils");

    private final RfcxGuardian app;
    public SocketUtils socketUtils;

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

    public void startServer() {

        socketUtils.serverThread = new Thread(() -> {
            Looper.prepare();
            try {
                socketUtils.serverSetup();
                while (true) {
                    InputStream socketInput = socketUtils.socketSetup();
                    if (socketInput.available() != 0) {
                        StringBuilder fileName = new StringBuilder();

                        BufferedReader br = new BufferedReader(new InputStreamReader(socketInput));
                        String read;
                        int derimeter = 0;
                        //read until reach '|'
                        while ((read = br.readLine()) != null) {
                            derimeter = read.indexOf("|");
                            if (derimeter == -1) {
                                break;
                            }
                            fileName.append(read.substring(0, derimeter));
                        }

                        socketInput.skip(derimeter);
                        // TODO: Send socket message for it
                        boolean result = writeStreamToDisk(socketInput, fileName.toString());
                        if (result) {
//                            sendDownloadResult(result);
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

    private boolean writeStreamToDisk(InputStream body, String fullFileName) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory().toString()+"/rfcx/apk", "softwares");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fullFileName);
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
