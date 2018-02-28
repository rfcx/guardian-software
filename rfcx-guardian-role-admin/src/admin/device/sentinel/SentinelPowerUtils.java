package admin.device.sentinel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private Map<String, int[]> sentinelPowerI2cValueIndex = new HashMap<String, int[]>();
	private Map<String, double[]> sentinelPowerI2cValues = new HashMap<String, double[]>();
	private Map<String, String[]> sentinelPowerI2cAddresses = new HashMap<String, String[]>();
	
	private void initSentinelPowerI2cOptions() {

		this.sentinelPowerI2cAddresses.put("battery-voltage", 	new String[] { 	"0x3a"	} );
		this.sentinelPowerI2cAddresses.put("battery-current", 	new String[] { 	"0x3d"	} );
		this.sentinelPowerI2cAddresses.put("input-voltage", 		new String[] { 	"0x3b"	} );
		this.sentinelPowerI2cAddresses.put("input-current", 		new String[] { 	"0x3e"	} );
		this.sentinelPowerI2cAddresses.put("load-voltage", 		new String[] { 	"0x3c"	} );
		
									// "groupName", 	new double[] { "voltage", "current",  "power" } );
		this.sentinelPowerI2cValues.put("battery", 	new double[] { 0, 			0,			0 } );
		this.sentinelPowerI2cValues.put("input", 	new double[] { 0, 			0,			0 } );
		this.sentinelPowerI2cValues.put("load", 		new double[] { 0, 			0,			0 } );
		
		this.sentinelPowerI2cValueIndex.put("voltage", 	new int[] { 0 });
		this.sentinelPowerI2cValueIndex.put("current", 	new int[] { 1 });
		this.sentinelPowerI2cValueIndex.put("power", 	new int[] { 2 });
	}
	
	public void updateSentinelPowerValues() {
		
		try {
			
			List<String[]> i2cLabelsAndSubAddresses = new ArrayList<String[]>();
			for (String sentinelLabel : this.sentinelPowerI2cAddresses.keySet()) {
				i2cLabelsAndSubAddresses.add(new String[] { sentinelLabel, this.sentinelPowerI2cAddresses.get(sentinelLabel)[0] });
			}
			
			for (String[] i2cLabelAndOutput : this.deviceI2cUtils.i2cGet(i2cLabelsAndSubAddresses)) {
				
				String groupName = i2cLabelAndOutput[0].substring(0,i2cLabelAndOutput[0].indexOf("-"));
				String valueType = i2cLabelAndOutput[0].substring(1+i2cLabelAndOutput[0].indexOf("-"));
				double[] valueSet = this.sentinelPowerI2cValues.get(groupName);
				valueSet[this.sentinelPowerI2cValueIndex.get(valueType)[0]] = applyValueModifier(i2cLabelAndOutput[0], Long.parseLong(i2cLabelAndOutput[1]));
				valueSet[2] = valueSet[0] * valueSet[1] / 1000;
				this.sentinelPowerI2cValues.put(groupName, valueSet);
			}
			
//			double[] batteryValueSet = this.sentinelPowerI2cValues.get("battery");
//			Log.d(logTag, "battery: "+Math.round(batteryValueSet[0])+"mV - "+Math.round(batteryValueSet[1])+"mA - "+Math.round(batteryValueSet[2])+"mW");
//			double[] inputValueSet = this.sentinelPowerI2cValues.get("input");
//			Log.d(logTag, "input: "+Math.round(inputValueSet[0])+"mV - "+Math.round(inputValueSet[1])+"mA - "+Math.round(inputValueSet[2])+"mW");
//			double[] loadValueSet = this.sentinelPowerI2cValues.get("load");
//			Log.d(logTag, "load: "+Math.round(loadValueSet[0])+"mV - "+Math.round(loadValueSet[1])+"mA - "+Math.round(loadValueSet[2])+"mW");

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private static double applyValueModifier(String i2cLabel, long i2cRawValue) {
		double modifiedValue = 0;
		if (i2cLabel.equals("battery-voltage")) {
			modifiedValue = i2cRawValue * 0.192264;
		} else if (i2cLabel.equals("battery-current")) {
			modifiedValue = i2cRawValue * 1.46487 / 6; // need real resistor value
		} else if (i2cLabel.equals("input-voltage")) {
			modifiedValue = i2cRawValue * 1.648;
		} else if (i2cLabel.equals("input-current")) {
			modifiedValue = i2cRawValue * 1.46487 / 6; // need real resistor value
		} else if (i2cLabel.equals("load-voltage")) {
			modifiedValue = i2cRawValue * 1.648;
		} else {
			Log.d(logTag, "No known value modifier for i2c label '"+i2cLabel+"'.");
		}
		return modifiedValue;
	}
	
	public String[] getCurrentValues(String groupName) {
		
		double[] powerVals = sentinelPowerI2cValues.get(groupName);
		return new String[] { ""+Math.round(powerVals[0]), ""+Math.round(powerVals[1]), ""+Math.round(powerVals[2]) };
	}
	
	
}
