package rfcx.utility.device;

import android.content.Context;
import android.text.TextUtils;
import rfcx.utility.rfcx.RfcxLog;

public class DevicePositionGeoLocation {
	
	public DevicePositionGeoLocation(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DevicePositionGeoLocation.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DevicePositionGeoLocation.class);
	
//	private long gpsLastUpdatedAt = System.currentTimeMillis();
//	private double gpsLatitude = 0;
//	private double gpsLongitude = 0;

	public static String getConcatPositionGeoLocation() {
		
		long gpsLastUpdatedAt = System.currentTimeMillis();
		double gpsLatitude = 37.78871;
		double gpsLongitude = -122.47485; 
		int gpsPrecision = 20;
		
		return (TextUtils.join("|", new String[] {
				TextUtils.join("*", new String[] { gpsLastUpdatedAt+"", gpsLatitude+"", gpsLongitude+"", gpsPrecision+"", "gps" })
			}));
	}
	
	
}
