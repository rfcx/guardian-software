package org.rfcx.guardian.utility.device;

import android.location.LocationManager;

public class DeviceGeoLocation {
	
	private static final String TAG = "Rfcx-Utils-"+DeviceGeoLocation.class.getSimpleName();
	
	private LocationManager locationManager;
	private double geoLocationLatitude = 0;
	private double geoLocationLongitude = 0;
	private double geoLocationPrecision = 0;
	
	public double[] getGeoLocation() {
		return new double[] { this.geoLocationLatitude, this.geoLocationLongitude, this.geoLocationPrecision };
	}
	
//	private void updateGeoLocation() {
//		
//		this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//		Criteria criteria = new Criteria();
//		String bestProvider = locationManager.getBestProvider(criteria, false);
//		Location location = locationManager.getLastKnownLocation(bestProvider);
//		try {
//			this.geoLocationLatitude = (double) location.getLatitude();
//			this.geoLocationLongitude = (double) location.getLongitude();
//			this.geoLocationPrecision = 0;
//		} catch (Exception e) {
//			RfcxLog.logExc(TAG, e);
//		}
//	}

	
	
	
}
