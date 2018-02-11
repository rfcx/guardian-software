package admin.device.sentinel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.content.Context;
import android.util.Log;

public class SentinelPowerUtils {

	public SentinelPowerUtils(Context context) {
		
		this.app = (RfcxGuardian) context.getApplicationContext();
		this.i2cUtils = new I2cUtils(context, sentinelPowerI2cMainAddress);

	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SentinelPowerUtils.class);

	private RfcxGuardian app = null;
	
	private I2cUtils i2cUtils = null;
	private static final String sentinelPowerI2cMainAddress = "0x68";
	
	private Map<String, String[]> sentinelPowerI2cOptions = new HashMap<String, String[]>();
	
	private void setSentinelPowerI2cOptions() {
										// "id"								"i2c sub addr",	"unit",	"multiplier"
		this.sentinelPowerI2cOptions.put("battery-voltage", new String[] { 	"0x3a", 			"mV", 	"0.192264" } );
		this.sentinelPowerI2cOptions.put("battery-current", new String[] { 	"0x3d", 			"TBD", 	"1" } );

		this.sentinelPowerI2cOptions.put("input-voltage", new String[] { 		"0x3b", 			"mV", 	"1.648" } );
		
		this.sentinelPowerI2cOptions.put("system-voltage", new String[] { 	"0x3c", 			"mV", 	"1.648" } );
		
	}
	
	private long i2cGet(String address) {
		try {
			String i2cReturn = this.i2cUtils.i2cGet( address );
			return Long.parseLong(i2cReturn.substring(i2cReturn.indexOf("x")+1),16);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return 0;
	}
	
	public void getBatteryVoltage() {
		
		long i2cValue = Math.round( i2cGet("0x3a") * 0.192264);
		Log.d(logTag, "getBatteryVoltage: " + i2cValue +"mV");
	}
	
	public void getBatteryCurrent() {
		
		long i2cValue = Math.round( i2cGet("0x3d") * 1);
		Log.d(logTag, "getBatteryCurrent: " + i2cValue +"");
	}
	
	public void getInputVoltage() {

		long i2cValue = Math.round( i2cGet("0x3b") * 1.648);
		Log.d(logTag, "getInputVoltage: " + i2cValue +"mV");
	}
	
	public void getSystemVoltage() {

		long i2cValue = Math.round( i2cGet("0x3c") * 1.648);
		Log.d(logTag, "getSystemVoltage: " + i2cValue +"mV");
	}

	
	
	
}
