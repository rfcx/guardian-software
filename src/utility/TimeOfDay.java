package utility;

import java.util.Calendar;

import org.rfcx.src_android.RfcxGuardian;

import android.content.Context;

public class TimeOfDay {

	private static final String TAG = TimeOfDay.class.getSimpleName();
	
	private Calendar calStart = null;
	private Calendar calEnd = null;
	private long msStart = 0;
	private long msEnd = 0;
	
	private void setStartEnd(int start, int end) {
		calStart = Calendar.getInstance();
		calEnd = Calendar.getInstance();
		calStart.set(Calendar.HOUR_OF_DAY, start);
		calEnd.set(Calendar.HOUR_OF_DAY, end);
		calStart.set(Calendar.MINUTE, 0);
		calEnd.set(Calendar.MINUTE, 0);
		calStart.set(Calendar.SECOND, 0);
		calEnd.set(Calendar.SECOND, 0);
		calStart.set(Calendar.MILLISECOND, 0);
		calEnd.set(Calendar.MILLISECOND, 0);
		msStart = calStart.getTimeInMillis();
		msEnd = calEnd.getTimeInMillis();
	}
	
	public boolean isDataGenerationEnabled(Context context) {
		RfcxGuardian rfcxSource = (RfcxGuardian) context.getApplicationContext();
		setStartEnd(rfcxSource.dayBeginsAt, rfcxSource.dayEndsAt);
		long msNow = Calendar.getInstance().getTimeInMillis();
		return ((msNow < msEnd) && (msNow > msStart));
	}
	
}
