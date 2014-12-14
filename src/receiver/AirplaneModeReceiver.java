package receiver;

import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;

import utility.TimeOfDay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {

	private static final String TAG = AirplaneModeReceiver.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private WifiManager wifiManager = null;
	private LocationManager locationManager = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (wifiManager == null) wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (locationManager == null) locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		TimeOfDay timeOfDay = new TimeOfDay();

		if (app.verboseLogging) Log.d(TAG,
				"(RfcxSource) AirplaneMode " + ( app.airplaneMode.isEnabled(context) ? "Enabled" : "Disabled" )
				+ " at "+(Calendar.getInstance()).getTime().toLocaleString());
		
		if (!app.airplaneMode.isEnabled(context)) {
			if (timeOfDay.isDataGenerationEnabled(context) || app.ignoreOffHours) {
				app.apiCore.setSignalSearchStart(Calendar.getInstance());
				wifiManager.setWifiEnabled(app.airplaneMode.getAllowWifi());	
			} else {
				if (app.verboseLogging) Log.d(TAG, "API Check-In not allowed right now");
			}
		}
	}
}
