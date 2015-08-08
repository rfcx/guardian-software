package org.rfcx.guardian.system.content;

import org.rfcx.guardian.system.content.SystemContentContract.Meta;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SystemContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+SystemContentProvider.class.getSimpleName();
	
	private static final int META_JSON = 1;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(SystemContentContract.AUTHORITY, "meta", META_JSON);
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

		      MatrixCursor cursor = new MatrixCursor(new String[]{"_id","meta_json"});
		      for (String name : new String[] {"poodle","labrador","german shephard","boston terrier","hound"}){
		        cursor.addRow(new Object[]{0,name});
		      }
		      Log.i(TAG,"returning " + cursor);
		      return cursor;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

}
