package org.rfcx.guardian.api.api;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class ApiContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.api.AUTHORITY;
	private static final String ENDPOINT_CHECKIN = RfcxRole.ContentProvider.api.ENDPOINT_CHECKIN;
	private static final String[] PROJECTION_CHECKIN = RfcxRole.ContentProvider.api.PROJECTION_CHECKIN;
	
	private static final int ENDPOINT_CHECKIN_LIST = 1;
	private static final int ENDPOINT_CHECKIN_ID = 2;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_CHECKIN, ENDPOINT_CHECKIN_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_CHECKIN+"/#", ENDPOINT_CHECKIN_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			MatrixCursor cursor = new MatrixCursor(PROJECTION_CHECKIN);
			
			cursor.addRow(new Object[] { 
					System.currentTimeMillis()
				});
			
			return cursor;
			
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
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
		
		if (URI_MATCHER.match(uri) == ENDPOINT_CHECKIN_LIST) {
			try {
				
				String[] audioInfo = new String[] {
						values.getAsString("created_at"),
						values.getAsString("timestamp"),
						values.getAsString("format"),
						values.getAsString("digest"),
						values.getAsString("samplerate"),
						values.getAsString("bitrate"),
						values.getAsString("codec"),
						values.getAsString("duration"),
						values.getAsString("encode_duration")
				};
				
				if (app.apiWebCheckIn.addCheckInToQueue(audioInfo, values.getAsString("filepath"))) {
					return Uri.parse(RfcxRole.ContentProvider.api.URI_CHECKIN+"/"+values.getAsString("timestamp"));
				}
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			}
		}
		return null;
	}
	
}
