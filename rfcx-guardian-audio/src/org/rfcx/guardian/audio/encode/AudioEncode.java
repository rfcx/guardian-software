package org.rfcx.guardian.audio.encode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.audio.service.AudioEncodeIntentService;
import org.rfcx.guardian.audio.service.CheckInTriggerIntentService;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class AudioEncode {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioEncode.class.getSimpleName();

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM/dd-a", Locale.US);

	public String sdCardFilesDir = Environment.getExternalStorageDirectory().toString()+"/rfcx";
	public String aacDir = null;
	public String encodeDir = null;
	
	public final static int AAC_ENCODING_BIT_RATE = 16384;
	
	
	public String getAudioFileLocation_PostEncode(long timestamp, String fileExtension) {
		return this.aacDir+"/"+dateFormat.format(new Date(timestamp))+"/"+timestamp+"."+fileExtension;
	}
	
	public String getAudioFileLocation_PreEncode(long timestamp, String fileExtension) {
		return this.encodeDir+"/"+timestamp+"."+fileExtension;
	}
	
	
	public void triggerAudioEncodeAfterCapture(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent audioEncodeIntentService = PendingIntent.getService(context, -1, new Intent(context, AudioEncodeIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), audioEncodeIntentService);
	}
	
	public void triggerCheckInAfterEncode(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent checkInTriggerIntentService = PendingIntent.getService(context, -1, new Intent(context, CheckInTriggerIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), checkInTriggerIntentService);
	}
	
	public void purgeSingleAudioAsset(AudioDb audioDb, String audioTimestamp) {
		Log.d(TAG, "Purging single audio asset: "+audioTimestamp);

		List<String[]> encodedAudioEntries = audioDb.dbEncoded.getAllEncoded();
		for (String[] encodedAudioEntry : encodedAudioEntries) {
			if (encodedAudioEntry[1].equals(audioTimestamp)) {
				try {
					(new File(getAudioFileLocation_PostEncode((long) Long.parseLong(encodedAudioEntry[1]),encodedAudioEntry[2]))).delete();
				} catch (Exception e) {
					Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
				}
			}
		}

		audioDb.dbEncoded.deleteSingleEncoded(audioTimestamp);
	}
	
}
