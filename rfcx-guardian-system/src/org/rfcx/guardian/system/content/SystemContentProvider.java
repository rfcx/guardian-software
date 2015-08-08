package org.rfcx.guardian.system.content;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.system.content.SystemContentContract.Meta;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SystemContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+SystemContentProvider.class.getSimpleName();

	private RfcxGuardian app = null;
	private Context context = null;
	
	private static final int META_JSON = 1;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(SystemContentContract.AUTHORITY, "meta", META_JSON);
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
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case META_JSON:
			return Meta.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		checkSetApplicationContext();
		
		MatrixCursor cursor = new MatrixCursor(Meta.PROJECTION_ALL);
		
		String[] dataTransferInfo = app.dataTransferDb.dbTransferred.getConcatRows();
		cursor.addRow(new Object[] { dataTransferInfo[0], dataTransferInfo[1] });
//		for (String name : new String[] {"poodle","labrador","german shephard","boston terrier","hound"}){
//			cursor.addRow(new Object[]{0,name});
//		}
		
		return cursor;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}
	
}
