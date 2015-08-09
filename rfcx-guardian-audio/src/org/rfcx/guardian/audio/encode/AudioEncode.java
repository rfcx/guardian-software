package org.rfcx.guardian.audio.encode;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sourceforge.javaFlacEncoder.FLAC_FileEncoder;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.audio.service.AudioEncodeIntentService;
import org.rfcx.guardian.audio.service.CheckInTriggerIntentService;
import org.rfcx.guardian.utility.FileUtils;
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
	
}
