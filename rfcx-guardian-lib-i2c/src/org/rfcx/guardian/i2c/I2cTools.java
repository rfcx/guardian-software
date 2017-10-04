package org.rfcx.guardian.i2c;

import android.util.Log;

public class I2cTools {
	
	private static final String logTag = "Rfcx-I2c-"+I2cTools.class.getSimpleName();
		
		public static String i2cGet(int i2cInterface, String i2cMainAddress, String i2cSubAddress) {
			
			i2cGetNative(i2cInterface, Integer.parseInt(i2cMainAddress,16), Integer.parseInt(i2cSubAddress,16));
			
			return "string";
		}
	
	    public native static int i2cGetNative(int i2cInterface, int i2cMainAddress, int i2cSubAddress);
	    
	    static {
//            System.loadLibrary("i2c-tools");
            System.loadLibrary("i2cget");
            System.loadLibrary("i2cset");
	    } 
	
	
}
