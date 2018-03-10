package rfcx.utility.device;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceGeoLocation {
	
	public DeviceGeoLocation(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceGeoLocation.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceGeoLocation.class);
	
	private LocationManager locationManager;
	private double geoLocationLatitude = 3.6141375; //0;
	private double geoLocationLongitude = 14.2108033; //0;
	private double geoLocationPrecision = 1.000001; //0;
	
	public double[] getGeoLocation() {
		return new double[] { this.geoLocationLatitude, this.geoLocationLongitude, this.geoLocationPrecision };
	}
	
	public String getSerializedGeoLocation() {
		double[] geoLocation = getGeoLocation();
		return (new StringBuilder())
			.append(System.currentTimeMillis())
			.append("*").append(geoLocation[0])
			.append("*").append(geoLocation[1])
			.append("*").append(geoLocation[2])
			.toString();
	}
	
//	private void updateGeoLocation() {
//		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//		Criteria criteria = new Criteria();
//		String bestProvider = locationManager.getBestProvider(criteria, false);
//		Location location = locationManager.getLastKnownLocation(bestProvider);
//		try {
//			this.geoLocationLatitude = (double) location.getLatitude();
//			this.geoLocationLongitude = (double) location.getLongitude();
//			this.geoLocationPrecision = 0;
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
//		Log.d(logTag, getSerializedGeoLocation());
//	}

	
	
	
}
