package org.rfcx.guardian.utility.mqtt;

import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.util.Log;

public class MqttUtils {

	public MqttUtils(String appRole, String guardianGuid) {
		this.logTag = RfcxLog.generateLogTag(appRole, MqttUtils.class);
		this.mqttClientId = ((new StringBuilder()).append("rfcx-guardian-").append(guardianGuid.toLowerCase(Locale.US))).toString();
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", MqttUtils.class);
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	
	private String mqttClientId = null;
	private int mqttQos = 2;
	
//	private int mqttBrokerPort = 1883;
	private String mqttBrokerProtocol = "tcp";
	private String mqqtBrokerUri = null;
	
	public void setBroker(String mqttBrokerAddress, int mqttBrokerPort) {
		this.mqqtBrokerUri = ((new StringBuilder()).append(this.mqttBrokerProtocol).append("://").append(mqttBrokerAddress).append(":").append(mqttBrokerPort)).toString();
	}
	
	public void sendMessage(String messageTopic, String messageContent) {
		
		try {
			MqttClient mqqtClient = new MqttClient(this.mqqtBrokerUri, this.mqttClientId, new MemoryPersistence());
			MqttConnectOptions mqqtConnectOptions = new MqttConnectOptions();
			mqqtConnectOptions.setCleanSession(true);
			
			Log.v(logTag, "paho-client connecting to broker: "+mqqtBrokerUri);
			mqqtClient.connect(mqqtConnectOptions);
			Log.v(logTag, "paho-client connected to broker");

			Log.v(logTag, "paho-client publishing message: "+messageContent);
			
			MqttMessage mqqtMessage = new MqttMessage(messageContent.getBytes());
			mqqtMessage.setQos(this.mqttQos);
			mqqtClient.publish(messageTopic, mqqtMessage);
			Log.v(logTag, "paho-client message published");
			
			mqqtClient.disconnect();
			Log.v(logTag, "paho-client disconnected");
			
		} catch (MqttException e) {
			RfcxLog.logExc(logTag, e);
		}
		
	}
	

	
}
