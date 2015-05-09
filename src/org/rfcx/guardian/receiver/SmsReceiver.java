package org.rfcx.guardian.receiver;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

	private static final String TAG = "RfcxGuardian-"+SmsReceiver.class.getSimpleName();
	private RfcxGuardian app = null;
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (app == null) app = (RfcxGuardian) context.getApplicationContext();
    	if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
    		Bundle bundle = intent.getExtras();
    		SmsMessage[] smsMessages = null;
            if (bundle != null) {
            	try {
            		Object[] pdus = (Object[]) bundle.get("pdus");
            		smsMessages = new SmsMessage[pdus.length];
                    for (int i = 0; i < smsMessages.length; i++) {
                    	smsMessages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                    	String createdAt = (new DateTimeUtils()).getDateTime();
                        app.smsDb.dbReceived.insert(createdAt, smsMessages[i].getOriginatingAddress(), smsMessages[i].getMessageBody(), generateSmsDigest(createdAt, smsMessages[i].getOriginatingAddress(), smsMessages[i].getMessageBody()));
                        Log.i(TAG, "Saved SMS from '"+smsMessages[i].getOriginatingAddress()+"': "+smsMessages[i].getMessageBody());
                    }
                } catch (Exception e) {
                	Log.e(TAG, (e != null) ? e.getMessage() : "Exception thrown, but exception itself is null.");
                }
            }
        }
    }
    
    private static String generateSmsDigest(String createdAt, String origin, String body) {
    	String msgText = createdAt+"|"+origin+"|"+body;
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        md.update(msgText.getBytes("iso-8859-1"), 0, msgText.length());
	        StringBuilder strBuffer = new StringBuilder();
	        for (byte b : md.digest()) {
	            int halfbyte = (b >>> 4) & 0x0F;
	            int two_halfs = 0;
	            do {
	            	strBuffer.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
	                halfbyte = b & 0x0F;
	            } while (two_halfs++ < 1);
	        }
	        return strBuffer.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception thrown, but exception itself is null.");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception thrown, but exception itself is null.");
		}
		return "";
    }
    
}
