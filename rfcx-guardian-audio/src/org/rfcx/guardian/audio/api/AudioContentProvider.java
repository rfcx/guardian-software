package org.rfcx.guardian.audio.api;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class AudioContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioContentProvider.class.getSimpleName();

	private RfcxGuardian app = null;
	private Context context = null;
	
	private static final String AUTHORITY = RfcxConstants.RfcxContentProvider.audio.AUTHORITY;
	private static final String ENDPOINT = RfcxConstants.RfcxContentProvider.audio.ENDPOINT;
	
	private static final int ENDPOINT_LIST = 1;
	private static final int ENDPOINT_ID = 2;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT, ENDPOINT_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT+"/#", ENDPOINT_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		checkSetApplicationContext();
		
		MatrixCursor cursor = new MatrixCursor(RfcxConstants.RfcxContentProvider.audio.PROJECTION);
		List<String[]> encodedEntries = app.audioDb.dbEncoded.getAllEncoded();
		for (String[] encodedEntry : encodedEntries) {
					// if it's asking for list, we return all rows...
			if (	(URI_MATCHER.match(uri) == ENDPOINT_LIST)
					// or if it's asking for one item, we check if each row matches, and return one
				|| 	((URI_MATCHER.match(uri) == ENDPOINT_ID) && encodedEntry[1].equals(uri.getLastPathSegment()))
				) {
				cursor.addRow(new Object[] { 
						encodedEntry[0], // created_at
						encodedEntry[1], // timestamp
						encodedEntry[2], // extension
						encodedEntry[3], // digest
						app.audioEncode.getAudioFileLocation_Complete_PostZip((long) Long.parseLong(encodedEntry[1]), encodedEntry[2])
					});
			}
		}
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		checkSetApplicationContext();
		
		if (URI_MATCHER.match(uri) == ENDPOINT_ID) {
			app.audioEncode.purgeSingleAudioAsset(app.audioDb, uri.getLastPathSegment());
			return 1;
		}
		return 0;
	}
	
	private void checkSetApplicationContext() {
		if (this.context == null) { this.context = getContext(); }
		if (this.app == null) { this.app = (RfcxGuardian) this.context.getApplicationContext(); }
	}
	
	@Override
	public boolean onCreate() {
		checkSetApplicationContext();
		
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
