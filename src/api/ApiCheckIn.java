package api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import org.rfcx.src_android.RfcxSource;

import android.content.Context;
import android.util.Log;

public class ApiCheckIn {

	private String apiEndpointCheckIn;

	private int transmitAttempts = 0;
	
	private RfcxSource app = null;
	
	public void sendCheckIn(Context context) {
		if (app == null) {
			app = (RfcxSource) context.getApplicationContext();
			app.apiCore.setApp(app);
		}
	//	if (httpPostCheckIn != null && !app.airplaneMode.isEnabled(context)) {
			app.apiCore.setTransmitting(true);
			this.transmitAttempts++;
			if (app.apiCore.getJsonZipped() == null) { app.apiCore.prepareDiagnostics(); }
			app.apiCore.setRequestSendStart(Calendar.getInstance().getTimeInMillis());
			
			app.apiCore.setRequestEndpoint(apiEndpointCheckIn);
			
			
//			String httpResponseString = executePostCheckIn();
			
//			if ((httpResponseString == null) && (specCount > 0)) {
//				if (this.transmitAttempts < 3) { sendCheckIn(context); }
//			} else {
//				cleanupAfterResponseCheckIn(httpResponseString);
//				this.transmitAttempts = 0;
//				app.airplaneMode.setOn(app.getApplicationContext());
//				if (app.verboseLogging) { Log.d(TAG, "Turning off antenna..."); }
//			}
			
		}
//	}
	

	public void setApiEndpointCheckIn(String apiEndpointCheckIn) {
		this.apiEndpointCheckIn = apiEndpointCheckIn;
	}
	
}
