package org.rfcx.guardian.contentprovider;

import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class GuardianContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, GuardianContentProvider.class);
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.guardian.AUTHORITY;
	
	private static final String ENDPOINT_PREFS = RfcxRole.ContentProvider.guardian.ENDPOINT_PREFS;
	private static final String[] PROJECTION_PREFS = RfcxRole.ContentProvider.guardian.PROJECTION_PREFS;
	private static final int ENDPOINT_PREFS_LIST = 1;
	private static final int ENDPOINT_PREFS_ID = 2;

	private static final String ENDPOINT_VERSION = RfcxRole.ContentProvider.guardian.ENDPOINT_VERSION;
	private static final String[] PROJECTION_VERSION = RfcxRole.ContentProvider.guardian.PROJECTION_VERSION;
	private static final int ENDPOINT_VERSION_LIST = 3;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_PREFS, ENDPOINT_PREFS_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_PREFS+"/#", ENDPOINT_PREFS_ID);
		
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_VERSION, ENDPOINT_VERSION_LIST);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			
			if (URI_MATCHER.match(uri) == ENDPOINT_PREFS_LIST) {
				MatrixCursor cursor = new MatrixCursor(PROJECTION_PREFS);
				for ( Map.Entry<String,?> pref : app.sharedPrefs.getAll().entrySet() ) {
					cursor.addRow(new Object[] {
						pref.getKey(), app.rfcxPrefs.getPrefAsString(pref.getKey())
					});
				}
				return cursor;
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_PREFS_ID) {
				String prefKey = uri.getLastPathSegment();
				MatrixCursor cursor = new MatrixCursor(PROJECTION_PREFS);
				cursor.addRow(new Object[] {
					prefKey, app.rfcxPrefs.getPrefAsString(prefKey)
				});
				return cursor;
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_VERSION_LIST) {
				MatrixCursor cursor = new MatrixCursor(PROJECTION_VERSION);
				cursor.addRow(new Object[] { RfcxGuardian.APP_ROLE.toLowerCase(Locale.US), RfcxRole.getRoleVersion(app.getApplicationContext(), logTag) });
				return cursor;
	
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
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
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}


	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
	
}
