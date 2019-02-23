package admin.device.sentinel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import admin.RfcxGuardian;
import android.content.Context;
import android.util.Log;
import rfcx.utility.device.DeviceI2cUtils;
import rfcx.utility.rfcx.RfcxLog;

public class SentinelPowerUtils {

	public SentinelPowerUtils(Context context) {
		
		this.deviceI2cUtils = new DeviceI2cUtils(context, sentinelPowerI2cMainAddress);
		initSentinelPowerI2cOptions();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SentinelPowerUtils.class);
	
	private DeviceI2cUtils deviceI2cUtils = null;
	private static final String sentinelPowerI2cMainAddress = "0x68";

	private String[] i2cValueIndex = new String[] {};
	private Map<String, double[]> i2cValues = new HashMap<String, double[]>();
	private Map<String, String[]> i2cAddresses = new HashMap<String, String[]>();
	private Map<String, long[]> i2cLastReadAt = new HashMap<String, long[]>();
	
	public boolean confirmConnection() {
		String isConnected = this.deviceI2cUtils.i2cGetAsString("0x4a", true);
		return ((isConnected != null) && (Long.parseLong(isConnected) > 0));
	}
	
	private void initSentinelPowerI2cOptions() {
		
		this.i2cValueIndex = new String[] { "voltage", "current", "temperature", "power" };
		
		//													 voltage   current   temp
		this.i2cAddresses.put("battery", 	new String[] {   "0x3a",   "0x3d",    null    } );
		this.i2cAddresses.put("input", 		new String[] {   "0x3b",   "0x3e",    null    } );
		this.i2cAddresses.put("system", 		new String[] {   "0x3c",    null,    "0x3f"   } );
		
		//												  voltage   current		temp	
		this.i2cLastReadAt.put("battery", 	new long[] {   0,   		0,    		0    	} );
		this.i2cLastReadAt.put("input", 		new long[] {   0,   		0,    		0    	} );
		this.i2cLastReadAt.put("system", 	new long[] {   0,    	0,    		0		} );

		//												voltage		current 		temp			power
		this.i2cValues.put("battery", 	new double[] {   0,			0,			0,			0 		} );
		this.i2cValues.put("input", 		new double[] {   0, 			0,			0,			0 		} );
		this.i2cValues.put("system", 	new double[] {   0, 			0,			0,			0 		} );

	}
	
	public void updateSentinelPowerValues() {
		
		try {
			
			List<String[]> i2cLabelsAndSubAddresses = new ArrayList<String[]>();
			for (String sentinelLabel : this.i2cAddresses.keySet()) {
				for (int i = 0; i < this.i2cAddresses.get(sentinelLabel).length; i++) {
					if (this.i2cAddresses.get(sentinelLabel)[i] != null) {
						i2cLabelsAndSubAddresses.add(new String[] { 
								(new StringBuilder()).append(sentinelLabel).append("-").append(this.i2cValueIndex[i]).toString(),
								this.i2cAddresses.get(sentinelLabel)[i]
							});
					}
				}
			}
			
			for (String[] i2cLabelAndOutput : this.deviceI2cUtils.i2cGet(i2cLabelsAndSubAddresses, true)) {
				
				String groupName = i2cLabelAndOutput[0].substring(0,i2cLabelAndOutput[0].indexOf("-"));
				String valueType = i2cLabelAndOutput[0].substring(1+i2cLabelAndOutput[0].indexOf("-"));
				double[] valueSet = this.i2cValues.get(groupName);
				int valueTypeIndex = 0; for (int i = 0; i < this.i2cValueIndex.length; i++) { if (this.i2cValueIndex[i].equals(valueType)) { valueTypeIndex = i; break; } }
				valueSet[valueTypeIndex] = applyValueModifier(i2cLabelAndOutput[0], Long.parseLong(i2cLabelAndOutput[1]));
				this.i2cLastReadAt.get(groupName)[valueTypeIndex] = System.currentTimeMillis();
				valueSet[3] = valueSet[0] * valueSet[1] / 1000;
				this.i2cValues.put(groupName, valueSet);
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private static double applyValueModifier(String i2cLabel, long i2cRawValue) {
		double modifiedValue = 0;
		if (i2cLabel.equals("battery-voltage")) {
			modifiedValue = i2cRawValue * 0.192264;
		} else if (i2cLabel.equals("battery-current")) {
			modifiedValue = i2cRawValue * 0.00146487 / 0.003; // hardcoded resistor value R[SNSB] = 0.003 ohms
		} else if (i2cLabel.equals("input-voltage")) {
			modifiedValue = i2cRawValue * 1.648;
		} else if (i2cLabel.equals("input-current")) {
			modifiedValue = i2cRawValue * 0.00146487 / 0.005; // hardcoded resistor value R[SNSI] = 0.005 ohms
		} else if (i2cLabel.equals("system-voltage")) {
			modifiedValue = i2cRawValue * 1.648;
		} else if (i2cLabel.equals("system-temperature")) {
			modifiedValue = (i2cRawValue - 12010) / 45.6;
		} else {
			Log.d(logTag, "No known value modifier for i2c label '"+i2cLabel+"'.");
		}
		return modifiedValue;
	}
	
	public String[] getCurrentValues(String groupName) {
		
		double[] powerVals = i2cValues.get(groupName);
		double valSum = 0; for (double val : powerVals) { valSum += val; }
		return (valSum == 0) ? null : new String[] { ""+Math.round(powerVals[0]), ""+Math.round(powerVals[1]), ""+Math.round(powerVals[2]), ""+Math.round(powerVals[3]) };
	}
	
	public static JSONArray getSentinelPowerValuesAsJsonArray(Context context) {
		
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		JSONArray powerJsonArray = new JSONArray();
		try {
			JSONObject powerJson = new JSONObject();
			powerJson.put("battery", app.sentinelPowerDb.dbSentinelPowerBattery.getConcatRowsWithLabelPrepended("battery"));
			powerJson.put("input", app.sentinelPowerDb.dbSentinelPowerInput.getConcatRowsWithLabelPrepended("input"));
			powerJson.put("system", app.sentinelPowerDb.dbSentinelPowerSystem.getConcatRowsWithLabelPrepended("system"));
			powerJsonArray.put(powerJson);
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			return powerJsonArray;
		}
	}
	
	public static int deleteSentinelPowerValuesBeforeTimestamp(String timeStamp, Context context) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		
		Date clearBefore = new Date(Long.parseLong(timeStamp));
	
		app.sentinelPowerDb.dbSentinelPowerBattery.clearRowsBefore(clearBefore);
		app.sentinelPowerDb.dbSentinelPowerInput.clearRowsBefore(clearBefore);
		app.sentinelPowerDb.dbSentinelPowerSystem.clearRowsBefore(clearBefore);
		
		return 1;
	}
	
	public void saveSentinelPowerValuesToDatabase(Context context, boolean printValuesToLog) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		
		if (printValuesToLog) { Log.d(logTag, "-");  }
		
		for (String groupName : this.i2cValues.keySet()) {
			String[] vals = getCurrentValues(groupName);
			if (vals != null) {
				if (groupName.equals("battery")) { 
					app.sentinelPowerDb.dbSentinelPowerBattery.insert(new Date(), vals[0], vals[1], vals[2], vals[3]);
				} else if (groupName.equals("input")) { 
					app.sentinelPowerDb.dbSentinelPowerInput.insert(new Date(), vals[0], vals[1], vals[2], vals[3]);
				} else if (groupName.equals("system")) { 
					app.sentinelPowerDb.dbSentinelPowerSystem.insert(new Date(), vals[0], vals[1], vals[2], vals[3]);
				}
				
				if (printValuesToLog) {
					Log.d(logTag, groupName+": "+vals[0]+"mV - "+vals[1]+"mA - "+vals[2]+"deg - "+vals[3]+"mW");
				}
			}
		}
		
	}
	
	
}
