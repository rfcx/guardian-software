package org.rfcx.guardian.utility;

public class RfcxConstants {
	
	public static final String ROLE_NAME = "Carrier";
	public static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final class RfcxContentProvider {
		
		public static final class system {
			public static final String AUTHORITY = "org.rfcx.guardian.system";
			public static final String[] PROJECTION_META = { "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer" };
			public static final String ENDPOINT_META = "meta";
			public static final String URI_META = "content://"+AUTHORITY+"/"+ENDPOINT_META;
			public static final String[] PROJECTION_SCREENSHOT = { "created_at", "timestamp", "format", "digest", "filepath" };
			public static final String ENDPOINT_SCREENSHOT = "screenshots";
			public static final String URI_SCREENSHOT = "content://"+AUTHORITY+"/"+ENDPOINT_SCREENSHOT;
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
		
		public static final class carrier {
			public static final String AUTHORITY = "org.rfcx.guardian.carrier";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "events";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
		public static final class sentinel {
			public static final String AUTHORITY = "org.rfcx.guardian.sentinel";
			public static final String[] PROJECTION_1 = { "current_time" };
			public static final String ENDPOINT_1 = "meta";
			public static final String URI_1 = "content://"+AUTHORITY+"/"+ENDPOINT_1;
		}
		
	}
	
}
