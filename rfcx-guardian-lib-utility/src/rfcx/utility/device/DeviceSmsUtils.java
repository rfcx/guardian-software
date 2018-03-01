package rfcx.utility.device;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceSmsUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceSmsUtils.class);
	
	public static JSONArray getSmsMessagesAsJsonArray(ContentResolver contentResolver) {
		
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
		return msgJsonArray;
	}
	
	public static int deleteSmsMessage(String messageAndroidId, ContentResolver contentResolver) {
		return contentResolver.delete(Uri.parse("content://sms/"+ messageAndroidId), null, null);
	}
	
	public static void sendSmsMessage(String address, String body) {
		try {
			(SmsManager.getDefault()).sendTextMessage(address, null, body, null, null);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	public static String processIncomingSmsMessageAsJson(Intent intent) {
		
		String jsonString = null;
		
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			try {
				Object[] smsPdus = (Object[]) bundle.get("pdus");
				JSONArray msgJsonArray = new JSONArray();
				for (Object smsPdu : smsPdus) {
					SmsMessage smsMsg = SmsMessage.createFromPdu( (byte[]) smsPdu );
					JSONObject msgJson = new JSONObject();
					msgJson.put("address", smsMsg.getOriginatingAddress());
					msgJson.put("body", smsMsg.getMessageBody());
					msgJson.put("received_at", smsMsg.getTimestampMillis());
					msgJsonArray.put(msgJson);
				}
				jsonString = msgJsonArray.toString();
            } catch (Exception e) {
            		RfcxLog.logExc(logTag, e);
            }
		}
		
		return jsonString;
    }
		
	
	
	
}
