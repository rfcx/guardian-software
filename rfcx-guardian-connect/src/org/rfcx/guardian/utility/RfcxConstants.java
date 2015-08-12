package org.rfcx.guardian.utility;


public class RfcxConstants {
	
	public static final String ROLE_NAME = "Connect";
	public static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final class RfcxContentProvider {
		
		public static final class system {
			public static final String AUTHORITY = "org.rfcx.guardian.system";
			public static final String[] PROJECTION_1 = { "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer" };
			public static final String ENDPOINT_1 = "meta";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
			public static final String[] PROJECTION_2 = { "created_at", "timestamp", "format", "digest", "filepath" };
			public static final String ENDPOINT_2 = "screenshots";
			public static final String URI_2 = "content://"+AUTHORITY+"/"+ENDPOINT_2;
		}
		
		public static final class reboot {
			public static final String AUTHORITY = "org.rfcx.guardian.reboot";
			public static final String[] PROJECTION_1 = { "last_rebooted_at" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class connect {
			public static final String AUTHORITY = "org.rfcx.guardian.connect";
			public static final String[] PROJECTION_1 = { "last_connected_at" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class api {
			public static final String AUTHORITY = "org.rfcx.guardian.api";
			public static final String[] PROJECTION_1 = { "checkin_id" };
			public static final String ENDPOINT_1 = "checkins";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class audio {
			public static final String AUTHORITY = "org.rfcx.guardian.audio";
			public static final String[] PROJECTION_1 = { "created_at", "timestamp", "format", "digest", "filepath" };
			public static final String ENDPOINT_1 = "audio";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class installer {
			public static final String AUTHORITY = "org.rfcx.guardian.installer";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class updater {
			public static final String AUTHORITY = "org.rfcx.guardian.updater";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
	}
	
}
