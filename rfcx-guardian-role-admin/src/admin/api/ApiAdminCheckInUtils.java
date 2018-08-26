package admin.api;

import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import admin.RfcxGuardian;
import android.content.Context;
import android.util.Log;
import rfcx.utility.misc.StringUtils;
import rfcx.utility.mqtt.MqttUtils;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;

public class ApiAdminCheckInUtils implements MqttCallback {

	public ApiAdminCheckInUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();

		this.mqttCheckInClient = new MqttUtils(RfcxGuardian.APP_ROLE, this.app.rfcxDeviceGuid.getDeviceGuid());

		this.subscribeBaseTopic = (new StringBuilder()).append("guardians/").append(this.app.rfcxDeviceGuid.getDeviceGuid().toLowerCase(Locale.US)).append("/").append(RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)).toString();
		this.mqttCheckInClient.addSubscribeTopic(this.subscribeBaseTopic + "/instructions");

		this.mqttCheckInClient.setOrResetBroker(this.app.rfcxPrefs.getPrefAsString("api_checkin_protocol"), this.app.rfcxPrefs.getPrefAsInt("api_checkin_port"), this.app.rfcxPrefs.getPrefAsString("api_checkin_host"));
		this.mqttCheckInClient.setCallback(this);

		confirmOrCreateConnectionToBroker();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiAdminCheckInUtils.class);

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	private String subscribeBaseTopic = null;



	private void processInstructionMessage(byte[] messagePayload) {

		String jsonStr = StringUtils.UnGZipByteArrayToString(messagePayload);
		Log.i(logTag, "Instruction: " + jsonStr);

		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			// get api-assigned instruction guid
			if (jsonObj.has("instruction_id")) {
				String instructionId = jsonObj.getString("instruction_id");
				if (instructionId.length() > 0) {
					Log.i(logTag, "Instruction ID: " + instructionId);
					// DO SOMETHING HERE
				}
			}

			// instructions: messages
			if (jsonObj.has("messages")) {
				JSONArray instructionMsgsJson = jsonObj.getJSONArray("messages");
				for (int i = 0; i < instructionMsgsJson.length(); i++) {
					JSONObject instructionMsgJson = instructionMsgsJson.getJSONObject(i);
					RfcxComm.getQueryContentProvider("admin", "sms_send",
							instructionMsgJson.getString("address") + "|" + instructionMsgJson.getString("body"),
							app.getApplicationContext().getContentResolver());
				}
			}

			// instructions: prefs
			if (jsonObj.has("prefs")) {
				JSONArray instructionPrefsJson = jsonObj.getJSONArray("prefs");
				for (int i = 0; i < instructionPrefsJson.length(); i++) {
					JSONObject instructionPrefJson = instructionPrefsJson.getJSONObject(i);
					// Here we would set preferences...
				}
			}

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}



	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) throws Exception {

		// this is an instruction message
		if (messageTopic.equalsIgnoreCase(this.subscribeBaseTopic + "/instructions")) {
			processInstructionMessage(mqttMessage.getPayload());

		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {

//		moveCheckInEntryToSentDatabase("0");
//		this.requestSendDuration = Math
//				.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.requestSendStart));
//		Log.i(logTag, (new StringBuilder()).append("CheckIn delivery time: ")
//				.append(DateTimeUtils.milliSecondDurationAsReadableString(this.requestSendDuration, true)).toString());

	}

	@Override
	public void connectionLost(Throwable cause) {

		RfcxLog.logThrowable(logTag, cause);

		confirmOrCreateConnectionToBroker();

	}

	public void confirmOrCreateConnectionToBroker() {

		long minDelayBetweenConnectionAttempts = 5000;

		if (mqttCheckInClient.mqttBrokerConnectionLastAttemptedAt < (app.deviceConnectivity.lastConnectedAt()
				- minDelayBetweenConnectionAttempts)) {
			try {
				mqttCheckInClient.confirmOrCreateConnectionToBroker(this.app.deviceConnectivity.isConnected());
			} catch (MqttException e) {
				RfcxLog.logExc(logTag, e);
			}
		} else {
//			Log.e(logTag, "Last connection attempt was less than " + minDelayBetweenConnectionAttempts + "ms ago");
		}
	}

}
