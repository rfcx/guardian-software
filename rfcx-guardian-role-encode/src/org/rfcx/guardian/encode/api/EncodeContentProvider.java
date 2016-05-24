package org.rfcx.guardian.encode.api;

import java.util.Date;

import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.audio.AudioFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class EncodeContentProvider extends ContentProvider {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+EncodeContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.ContentProvider.encode.AUTHORITY;
	
	private static final String ENDPOINT_QUEUE = RfcxRole.ContentProvider.encode.ENDPOINT_QUEUE;
	private static final String[] PROJECTION_QUEUE = RfcxRole.ContentProvider.encode.PROJECTION_QUEUE;
	private static final String ENDPOINT_ENCODED = RfcxRole.ContentProvider.encode.ENDPOINT_ENCODED;
	private static final String[] PROJECTION_ENCODED = RfcxRole.ContentProvider.encode.PROJECTION_ENCODED;
	
	private static final int ENDPOINT_QUEUE_LIST = 1;
	private static final int ENDPOINT_QUEUE_ID = 2;
	private static final int ENDPOINT_ENCODED_LIST = 3;
	private static final int ENDPOINT_ENCODED_ID = 4;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_QUEUE, ENDPOINT_QUEUE_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_QUEUE+"/#", ENDPOINT_QUEUE_ID);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_ENCODED, ENDPOINT_ENCODED_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_ENCODED+"/#", ENDPOINT_ENCODED_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_QUEUE_LIST) {
				
				MatrixCursor cursor = new MatrixCursor(PROJECTION_QUEUE);

				for (String[] queuedRow : app.audioEncodeDb.dbEncodeQueue.getAllRows()) {
					cursor.addRow(new Object[] {
							
					});
				}
				
				return cursor;
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_ENCODED_LIST) {
				
				MatrixCursor cursor = new MatrixCursor(PROJECTION_QUEUE);

				for (String[] encodedRow : app.audioEncodeDb.dbEncoded.getAllRows()) {
					cursor.addRow(new Object[] {
							
					});
				}
				
				return cursor;
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_QUEUE_ID) {
				app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(uri.getLastPathSegment());
				return 1;
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_ENCODED_ID) {
				String[] audioFromDb = app.audioEncodeDb.dbEncoded.getSingleRowByAudioId(uri.getLastPathSegment());
				app.audioEncodeDb.dbEncoded.deleteSingleRow(audioFromDb[1]);
				return 1;
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
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
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_QUEUE_LIST) {
				
				app.audioEncodeDb.dbEncodeQueue.insert(
						values.getAsString("timestamp"),
						values.getAsString("format"),
						values.getAsString("digest"),
						values.getAsInteger("samplerate"),
						values.getAsInteger("bitrate"),
						values.getAsString("codec"),
						values.getAsLong("duration"),
						values.getAsLong("encode_duration"),
						values.getAsString("filepath")
						);
				
				return Uri.parse(RfcxRole.ContentProvider.encode.URI_QUEUE+"/"+values.getAsString("timestamp"));
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_ENCODED_LIST) {
				return null;
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
}
