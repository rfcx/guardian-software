package org.rfcx.guardian.reboot.api;

import org.rfcx.guardian.reboot.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class RebootContentProvider extends ContentProvider {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+RebootContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.reboot.AUTHORITY;
	
	private static final String ENDPOINT_1 = RfcxRole.ContentProvider.reboot.ENDPOINT_1;
	private static final String[] PROJECTION_1 = RfcxRole.ContentProvider.reboot.PROJECTION_1;
	
	private static final int ENDPOINT_1_LIST = 1;
	private static final int ENDPOINT_1_ID = 2;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_1, ENDPOINT_1_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_1+"/#", ENDPOINT_1_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			MatrixCursor cursor = new MatrixCursor(PROJECTION_1);
			
			cursor.addRow(new Object[] { System.currentTimeMillis() });
			
			return cursor;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
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
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return 0;
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
