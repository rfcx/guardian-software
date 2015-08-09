package org.rfcx.guardian.utility;

public class RfcxConstants {
	
	public static final String ROLE_NAME = "Reboot";
	public static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final class RfcxContentProvider {
		
		public static final class system {
			public static final String[] PROJECTION = { "measured_at", "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer" };
			public static final String AUTHORITY = "org.rfcx.guardian.system";
			public static final String ENDPOINT = "meta";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
		public static final class reboot {
			public static final String[] PROJECTION = { "last_rebooted_at" };
			public static final String AUTHORITY = "org.rfcx.guardian.reboot";
			public static final String ENDPOINT = "events";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
	}
	
}
