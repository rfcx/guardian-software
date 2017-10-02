package org.rfcx.guardian.i2c;

import android.util.Log;

public class I2cTools {
	
	private static final String logTag = "Rfcx-I2c-"+I2cTools.class.getSimpleName();
		
		public static String i2cGet(int i2cInterface, String i2cMainAddress, String i2cSubAddress) {
			
			i2cGetNative(Integer.parseInt(i2cMainAddress,16));
			
			return "string";
		}
	
//		public static void writeToI2c(int i2cSlaveAddress_int) {
//			i2cWriteNative(i2cSlaveAddress_int);
//		}
	
	    public native static int i2cGetNative(int i2cSlaveAddress);
	    
	    static {
            System.loadLibrary("i2c-tools");
            System.loadLibrary("i2cget");
            System.loadLibrary("i2cset");
	    } 
	
	
}
