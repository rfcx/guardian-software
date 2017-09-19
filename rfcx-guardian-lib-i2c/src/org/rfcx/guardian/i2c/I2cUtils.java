package org.rfcx.guardian.i2c;

import android.util.Log;

public class I2cUtils {
	
	private static final String logTag = "Rfcx-I2c-"+I2cUtils.class.getSimpleName();

		public static void writeToI2c(String i2cSlaveAddress_hex) {
			i2cWriteNative(Integer.parseInt(i2cSlaveAddress_hex,16));
		}
	
		public static void writeToI2c(int i2cSlaveAddress_int) {
			i2cWriteNative(i2cSlaveAddress_int);
		}
	
	    public native static int i2cWriteNative(int i2cSlaveAddress);
	    
	    static {
            System.loadLibrary("i2clib");
	    } 
	
	
}
