package org.rfcx.guardian.system.content;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class SystemContentContract {

	public static final String AUTHORITY = "org.rfcx.guardian."+org.rfcx.guardian.utility.Constants.ROLE_NAME.toLowerCase();

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final class Meta implements CommonColumns {

		public static final Uri CONTENT_URI =  Uri.withAppendedPath(SystemContentContract.CONTENT_URI, "meta");

		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.org.rfcx.guardian.system.meta_items";
	
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.org.rfcx.guardian.system.meta_items";

		public static final String[] PROJECTION_ALL = { _ID, META_JSON };
		
	}

	public static interface CommonColumns extends BaseColumns {

      public static final String META_JSON = "meta_json";

	}
}
