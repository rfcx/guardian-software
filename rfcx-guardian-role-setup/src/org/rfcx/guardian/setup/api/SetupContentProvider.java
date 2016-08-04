package org.rfcx.guardian.setup.api;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.setup.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class SetupContentProvider extends ContentProvider {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+SetupContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.setup.AUTHORITY;
	private static final String ENDPOINT_PREFS = RfcxRole.ContentProvider.setup.ENDPOINT_PREFS;
	private static final String[] PROJECTION_PREFS = RfcxRole.ContentProvider.setup.PROJECTION_PREFS;
	
	private static final int ENDPOINT_PREFS_LIST = 1;
	private static final int ENDPOINT_PREFS_ID = 2;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_PREFS, ENDPOINT_PREFS_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_PREFS+"/#", ENDPOINT_PREFS_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			MatrixCursor cursor = new MatrixCursor(PROJECTION_PREFS);
			
			List<String> prefValues = new ArrayList<String>();
			for (String prefKey : PROJECTION_PREFS) {
				prefValues.add(app.rfcxPrefs.getPrefAsString(prefKey));
			}
			
			cursor.addRow( prefValues.toArray(new Object[prefValues.size()]) );
			
			return cursor;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
		
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_PREFS_ID) {
				String prefKey = uri.getLastPathSegment();
				app.setPref(prefKey, values.getAsString(prefKey));

				return 1;
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return 0;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}
	
}
