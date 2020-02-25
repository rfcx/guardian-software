package org.rfcx.guardian.updater.api;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class UpdaterContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+UpdaterContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.updater.AUTHORITY;
	private static final String ENDPOINT_1 = RfcxRole.updater.ENDPOINT_1;
	private static final String[] PROJECTION_1 = RfcxRole.updater.PROJECTION_1;

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
			
			for (String softwareRole : RfcxRole.ALL_ROLES) {
				cursor.addRow(new Object[] { 
					softwareRole,
					(RfcxRole.isRoleInstalled(app.getApplicationContext(), softwareRole)) ? app.rfcxPrefs.getVersionFromFile(softwareRole) : null
				});
			}
			
			return cursor;
			
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
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
