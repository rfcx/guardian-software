package org.rfcx.src_receiver;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

	private static final String TAG = SmsReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
    		Log.d(TAG, "Sms onReceive");
    		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
    		Bundle bundle = intent.getExtras();
    		SmsMessage[] msgs = null;
            String origin;
            if (bundle != null) {
            	try {
            		Object[] pdus = (Object[]) bundle.get("pdus");
            		msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        origin = msgs[i].getOriginatingAddress();
                        String message = msgs[i].getMessageBody();
                        Log.d(TAG, "SMS from "+origin+": "+message);
                        rfcxSource.smsDb.dbSms.insert(origin, message);
                    }
                } catch (Exception e) {
                	Log.e(TAG, e.getMessage());
                }
            }
        }
    }
}


//rfcxSource.deviceState.setLightLevel(Math.round(event.values[0]));
//rfcxSource.deviceStateDb.dbLight.insert(rfcxSource.deviceState.getLightLevel());
