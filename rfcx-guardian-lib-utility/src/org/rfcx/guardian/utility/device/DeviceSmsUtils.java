package org.rfcx.guardian.utility.device;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class DeviceSmsUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceSmsUtils.class);
	
	public static String getSmsMessagesAsJson(ContentResolver contentResolver) {
		
		JSONArray msgJsonArray = new JSONArray();
		Cursor cursor = contentResolver.query(Uri.parse("content://sms/"), null, null, null, null);
		
		if ((cursor.getCount() > 0) && cursor.moveToFirst()) { do {
			try {
				JSONObject msgJson = new JSONObject();
				msgJson.put("android_id", cursor.getString(cursor.getColumnIndex("_id")));
				msgJson.put("received_at", cursor.getLong(cursor.getColumnIndex("date")));
				msgJson.put("address", cursor.getString(cursor.getColumnIndex("address")));
				msgJson.put("body", cursor.getString(cursor.getColumnIndex("body")));
				msgJsonArray.put(msgJson);
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		} while (cursor.moveToNext()); } cursor.close();
		return msgJsonArray.toString();
	}
	
	public static int deleteSmsMessage(String messageAndroidId, ContentResolver contentResolver) {
		return contentResolver.delete(Uri.parse("content://sms/"+ messageAndroidId), null, null);
	}
	
	
	
}
