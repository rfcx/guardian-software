package org.rfcx.guardian.guardian.audio.cast;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AudioCastUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCastUtils");
    public SocketUtils socketUtils;
    private final RfcxGuardian app;
    private List<String> pingJson;
    public AudioCastUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        this.socketUtils = new SocketUtils();
        this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.AUDIO);
    }

    public void updatePingJson(boolean printJsonToLogs) {
        try {
            pingJson = app.audioCastPingUtils.buildPingJson(printJsonToLogs);
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "updatePingJson");
        }
    }

    public boolean sendSocketPing() {
        return this.socketUtils.sendAudioSocketJson(pingJson, isAudioCastEnablable(false, app.rfcxPrefs));
    }

    public boolean isAudioCastEnablable(boolean verboseLogging, RfcxPrefs rfcxPrefs) {

        boolean prefsEnableAudioCast = rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CAST);

        String prefsWifiFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
        boolean isWifiEnabled = prefsWifiFunction.equals("hotspot") || prefsWifiFunction.equals("client");

        String prefsBluetoothFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION);
        boolean isBluetoothEnabled = prefsBluetoothFunction.equals("pan");

        if (verboseLogging && prefsEnableAudioCast && !isWifiEnabled && !isBluetoothEnabled) {
            Log.e(logTag, "Audio Cast Socket Server could not be enabled because '" + RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION + "' and '" + RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION + "' are set to off.");
        }

        return prefsEnableAudioCast && (isWifiEnabled || isBluetoothEnabled);
    }

    private void processReceivedJson(String jsonStr) {
        // do nothing — we don't expect to receive anything
    }

    public void startServer() {

        socketUtils.serverThread = new Thread(() -> {
            Looper.prepare();
            try {
                socketUtils.serverSetup();
                while (!socketUtils.serverThread.isInterrupted()) {
                    InputStream socketInput = socketUtils.socketSetup();
                    if (socketInput != null) {
                        String jsonStr = socketUtils.streamSetup(socketInput);
                        if (jsonStr != null) {
                            processReceivedJson(jsonStr);
                        }
                    }
                }
            } catch (IOException | NullPointerException e) {
                    RfcxLog.logExc(logTag, e);
            }
            Looper.loop();
        });
        socketUtils.serverThread.start();
        socketUtils.isServerRunning = true;
    }

}
