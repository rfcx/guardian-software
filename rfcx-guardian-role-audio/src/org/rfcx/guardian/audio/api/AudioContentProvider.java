package org.rfcx.guardian.audio.api;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class AudioContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.audio.AUTHORITY;
	private static final String ENDPOINT_1 = RfcxRole.ContentProvider.audio.ENDPOINT_1;
	private static final String[] PROJECTION_1 = RfcxRole.ContentProvider.audio.PROJECTION_1;
	
	private static final int ENDPOINT_1_LIST = 1;
	private static final int ENDPOINT_1_ID = 2;
	private static final int ENDPOINT_1_FILENAME = 3;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_1, ENDPOINT_1_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_1+"/#", ENDPOINT_1_ID);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_1+"/*", ENDPOINT_1_FILENAME);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		MatrixCursor cursor = new MatrixCursor(PROJECTION_1);
//		List<String[]> encodedEntries = app.audioCaptureDb.dbEncoded.getAllRows();
//		for (String[] encodedEntry : encodedEntries) {
//					// if it's asking for list, we return all rows...
//			if (	(URI_MATCHER.match(uri) == ENDPOINT_1_LIST)
//					// or if it's asking for one item, we check if each row matches, and return one
//				|| 	((URI_MATCHER.match(uri) == ENDPOINT_1_ID) && encodedEntry[1].equals(uri.getLastPathSegment()))
//				) {
//				cursor.addRow(new Object[] { 
//						encodedEntry[0], // created_at
//						encodedEntry[1], // timestamp
//						encodedEntry[2], // extension
//						encodedEntry[3], // digest
//						AudioFile.getAudioFileLocation_Complete_PostZip((long) Long.parseLong(encodedEntry[1]), encodedEntry[2])
//					});
//			}
//		}
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		String urlValue = uri.getLastPathSegment();
		
		return 0;
	}
	
	@Override
	public boolean onCreate() {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
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
