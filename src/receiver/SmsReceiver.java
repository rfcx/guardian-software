package receiver;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

	private static final String TAG = SmsReceiver.class.getSimpleName();
	private RfcxSource app = null;
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (app == null) app = (RfcxSource) context.getApplicationContext();
    	if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
    		Bundle bundle = intent.getExtras();
    		SmsMessage[] smsMessages = null;
            if (bundle != null) {
            	try {
            		Object[] pdus = (Object[]) bundle.get("pdus");
            		smsMessages = new SmsMessage[pdus.length];
                    for (int i = 0; i < smsMessages.length; i++) {
                    	smsMessages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);;
                        app.smsDb.dbSms.insert(smsMessages[i].getOriginatingAddress(), smsMessages[i].getMessageBody());
                        if (app.verboseLogging) Log.d(TAG, "Saved SMS from '"+smsMessages[i].getOriginatingAddress()+"': "+smsMessages[i].getMessageBody());
                    }
                } catch (Exception e) {
                	Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
                }
            }
        }
    }
}
