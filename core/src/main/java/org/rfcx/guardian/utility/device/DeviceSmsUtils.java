package org.rfcx.guardian.utility.device;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class DeviceSmsUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceSmsUtils.class);
	
	public static JSONArray getSmsMessagesFromSystemAsJsonArray(ContentResolver contentResolver) {
		
		JSONArray msgJsonArray = new JSONArray();

		// fetch from SMS system database
		Cursor cursor = contentResolver.query(Uri.parse("content://sms/"), null, null, null, null);
		if ((cursor != null) && (cursor.getCount() > 0) && cursor.moveToFirst()) { do {
			try {
				JSONObject msgJson = new JSONObject();
				msgJson.put("received_at", cursor.getLong(cursor.getColumnIndex("date")));
				msgJson.put("address", cursor.getString(cursor.getColumnIndex("address")));
				msgJson.put("body", cursor.getString(cursor.getColumnIndex("body")));
				msgJson.put("android_id", cursor.getString(cursor.getColumnIndex("_id")));
				msgJsonArray.put(msgJson);
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		} while (cursor.moveToNext()); } cursor.close();

		return msgJsonArray;
	}

	public static JSONArray formatSmsMessagesFromDatabaseAsJsonArray(List<String[]> smsDbRows) {

		JSONArray msgJsonArray = new JSONArray();
		for (String[] smsDbRow : smsDbRows) {
			try {
				JSONObject msgJson = new JSONObject();
				msgJson.put("received_at", smsDbRow[1]);
				msgJson.put("address", smsDbRow[2]);
				msgJson.put("body", smsDbRow[3]);
				msgJson.put("android_id", smsDbRow[4]);
				msgJsonArray.put(msgJson);
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return msgJsonArray;
	}

	public static JSONArray combineSmsMessageJsonArrays(List<JSONArray> jsonArrays) {
		JSONArray combinedJsonArrays = new JSONArray();
		for (JSONArray thisArr : jsonArrays) {
			for (int i = 0; i < thisArr.length(); i++) {
				try {
					combinedJsonArrays.put(thisArr.getJSONObject(i));
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}
		return combinedJsonArrays;
	}
	
	public static int deleteSmsMessageFromSystem(String messageAndroidId, ContentResolver contentResolver) {
		int deleteFromSystem = contentResolver.delete(Uri.parse("content://sms/"+ messageAndroidId), null, null);
		return deleteFromSystem;
	}
	
	public static int sendSmsMessage(String address, String body) {
		int rtrnInt = 0;
		try {
			(SmsManager.getDefault()).sendTextMessage(address, null, body, null, null);
			rtrnInt = 1;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return rtrnInt;
	}

	public static String generateMessageId() {
		return ((int) (Math.random() * 100000 + 1)) + "";
	}
	
	public static JSONArray processIncomingSmsMessageAsJson(Intent intent) {
		
		JSONArray msgsJsonArray = new JSONArray();
		
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			try {
				for (Object smsPdu : (Object[]) bundle.get("pdus")) {
					SmsMessage smsMsg = SmsMessage.createFromPdu( (byte[]) smsPdu );
					JSONObject msgJson = new JSONObject();
					msgJson.put("address", smsMsg.getOriginatingAddress());
					msgJson.put("body", smsMsg.getMessageBody());
					msgJson.put("received_at", smsMsg.getTimestampMillis());
					msgsJsonArray.put(msgJson);
				}
            } catch (Exception e) {
            		RfcxLog.logExc(logTag, e);
            }
		}
		
		return msgsJsonArray;
    }
		
//	public static String getDefaultSmsApp(Context context) {
//		return Telephony.Sms.getDefaultSmsPackage(context);
//	}
//
//	public static boolean isThisTheDefaultSmsApp(Context context) {
//		return (Telephony.Sms.getDefaultSmsPackage(context).equalsIgnoreCase(context.getPackageName()));
//	}

//	public static void checkSetThisAsTheDefaultSmsApp(Context context) {
//		if (!Telephony.Sms.getDefaultSmsPackage(context).equalsIgnoreCase(context.getPackageName())) {
//			Intent intent = new Intent(context, Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
//			intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
//			context.startActivity(intent);
//		}
//	}
	
	
}
