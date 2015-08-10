package org.rfcx.guardian.utility;

public class RfcxConstants {
	
	public static final String ROLE_NAME = "Api";
	public static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final class RfcxContentProvider {
		
		public static final class system {
			public static final String[] PROJECTION = { "battery", "cpu", "power", "network", "offline", "lightmeter", "data_transfer" };
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
		
		public static final class connect {
			public static final String[] PROJECTION = { "last_connected_at" };
			public static final String AUTHORITY = "org.rfcx.guardian.connect";
			public static final String ENDPOINT = "events";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
		public static final class api {
			public static final String[] PROJECTION = { "checkin_id" };
			public static final String AUTHORITY = "org.rfcx.guardian.api";
			public static final String ENDPOINT = "checkins";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
		public static final class audio {
			public static final String[] PROJECTION = { "created_at", "timestamp", "format", "digest", "filepath" };
			public static final String AUTHORITY = "org.rfcx.guardian.audio";
			public static final String ENDPOINT = "audio";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
		public static final class installer {
			public static final String[] PROJECTION = { "current_time" };
			public static final String AUTHORITY = "org.rfcx.guardian.installer";
			public static final String ENDPOINT = "events";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
		public static final class updater {
			public static final String[] PROJECTION = { "current_time" };
			public static final String AUTHORITY = "org.rfcx.guardian.updater";
			public static final String ENDPOINT = "events";
			public static final String URI = "content://"+AUTHORITY+"/"+ENDPOINT;
		}
		
	}
	
}
