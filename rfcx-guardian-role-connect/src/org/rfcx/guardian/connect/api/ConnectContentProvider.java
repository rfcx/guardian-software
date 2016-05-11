package org.rfcx.guardian.connect.api;

import java.util.Calendar;
import java.util.Date;

import org.rfcx.guardian.connect.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class ConnectContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ConnectContentProvider.class.getSimpleName();

	private RfcxGuardian app = null;
	private Context context = null;
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.connect.AUTHORITY;
	private static final String ENDPOINT_1 = RfcxRole.ContentProvider.connect.ENDPOINT_1;
	
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
		checkSetApplicationContext();
		
		MatrixCursor cursor = new MatrixCursor(RfcxRole.ContentProvider.connect.PROJECTION_1);
		
		cursor.addRow(new Object[] { 
				Calendar.getInstance().getTimeInMillis()
			});
		
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		checkSetApplicationContext();
		
		return 0;	
	}
	
	private void checkSetApplicationContext() {
		if (this.context == null) { this.context = getContext(); }
		if (this.app == null) { this.app = (RfcxGuardian) this.context.getApplicationContext(); }
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		checkSetApplicationContext();
		
		return 0;
	}
	
	@Override
	public String getType(Uri uri) {
		checkSetApplicationContext();
		
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		checkSetApplicationContext();
		
		return null;
	}
	
}
