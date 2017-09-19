package org.rfcx.guardian.admin.contentprovider;

import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class AdminContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AdminContentProvider.class);
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.admin.AUTHORITY;
	
	private static final String ENDPOINT_VERSION = RfcxRole.ContentProvider.admin.ENDPOINT_VERSION;
	private static final String[] PROJECTION_VERSION = RfcxRole.ContentProvider.admin.PROJECTION_VERSION;
	private static final int ENDPOINT_VERSION_LIST = 1;
	
	private static final String ENDPOINT_EXECUTE = RfcxRole.ContentProvider.admin.ENDPOINT_EXECUTE;
	private static final String[] PROJECTION_EXECUTE = RfcxRole.ContentProvider.admin.PROJECTION_EXECUTE;
	private static final int ENDPOINT_EXECUTE_ID = 2;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_VERSION, ENDPOINT_VERSION_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_EXECUTE+"/#", ENDPOINT_EXECUTE_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			
			if (URI_MATCHER.match(uri) == ENDPOINT_VERSION_LIST) {
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
			if (URI_MATCHER.match(uri) == ENDPOINT_EXECUTE_ID) {

				String commandName = uri.getLastPathSegment();
				
				if (commandName.equals("reboot")) {
					app.rfcxServiceHandler.triggerService("RebootTrigger", true);
					return 1;
					
				} else if (commandName.equals("something")) {
					
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return 0;
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return null;
	}
	
}
