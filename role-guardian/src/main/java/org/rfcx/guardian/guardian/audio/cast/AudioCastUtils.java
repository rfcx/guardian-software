package org.rfcx.guardian.guardian.audio.cast;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.network.SocketUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.IOException;
import java.io.InputStream;

public class AudioCastUtils {

	public AudioCastUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
//		this.socketUtils = new SocketUtils();
//		this.socketUtils.setSocketPort(RfcxComm.TCP_PORTS.GUARDIAN.SOCKET.AUDIO);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCastUtils");

	private RfcxGuardian app;






	public boolean isAudioCastEnablable(boolean verboseLogging, RfcxPrefs rfcxPrefs) {

		boolean prefsEnableAudioCast = rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CAST);

		String prefsWifiFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
		boolean isWifiEnabled = prefsWifiFunction.equals("hotspot") || prefsWifiFunction.equals("client");

		String prefsBluetoothFunction = rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION);
		boolean isBluetoothEnabled = prefsBluetoothFunction.equals("pan");

		if (verboseLogging && prefsEnableAudioCast && !isWifiEnabled && !isBluetoothEnabled) {
			Log.e( logTag, "Audio Cast Socket Server could not be enabled because '"+RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION+"' and '"+RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION+"' are set to off.");
		}

		return prefsEnableAudioCast && (isWifiEnabled || isBluetoothEnabled);
	}



}