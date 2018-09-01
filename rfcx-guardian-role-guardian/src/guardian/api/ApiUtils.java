package guardian.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.StringUtils;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceDiskUsage;
import rfcx.utility.device.DeviceMobilePhone;
import rfcx.utility.device.control.DeviceLogCat;
import rfcx.utility.device.control.DeviceScreenShot;
import rfcx.utility.mqtt.MqttUtils;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxRole;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiUtils implements MqttCallback {

	public ApiUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();

//		this.requestTimeOutLength = 2 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
//		initializeFailedCheckInThresholds();
//
//		this.mqttCheckInClient = new MqttUtils(RfcxGuardian.APP_ROLE, this.app.rfcxDeviceGuid.getDeviceGuid());
//
//		this.subscribeBaseTopic = (new StringBuilder()).append("guardians/").append(this.app.rfcxDeviceGuid.getDeviceGuid().toLowerCase(Locale.US)).append("/").append(RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)).toString();
//		this.mqttCheckInClient.addSubscribeTopic(this.subscribeBaseTopic + "/instructions");
//		this.mqttCheckInClient.addSubscribeTopic(this.subscribeBaseTopic + "/checkins");
//
//		this.mqttCheckInClient.setOrResetBroker(this.app.rfcxPrefs.getPrefAsString("api_checkin_protocol"), this.app.rfcxPrefs.getPrefAsInt("api_checkin_port"), this.app.rfcxPrefs.getPrefAsString("api_checkin_host"));
//		this.mqttCheckInClient.setCallback(this);
//		this.mqttCheckInClient.setActionTimeout(this.requestTimeOutLength);
//
//		confirmOrCreateConnectionToBroker();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiUtils.class);

	private RfcxGuardian app;
	
	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		// TODO Auto-generated method stub
		
	}



}
