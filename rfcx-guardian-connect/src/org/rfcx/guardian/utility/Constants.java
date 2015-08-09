package org.rfcx.guardian.utility;


public class Constants {
	
	public static final String ROLE_NAME = "Connect";
	public static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final class RfcxContentProvider {
		
		public static final class system {
			public static final String[] PROJECTION = { "measured_at", "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer" };
			public static final String AUTHORITY = "org.rfcx.guardian.system";
			public static final String ENDPOINT = "meta";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		
		}
		
	}
	
}
