package org.rfcx.guardian.i2c;

import android.content.Context;
import android.util.Log;

public class I2cTools {
	
	public I2cTools(int i2cInterface) {
		this.i2cInterface = i2cInterface;
	}
	
	private static final String logTag = "Rfcx-I2c-"+I2cTools.class.getSimpleName();
	private int i2cInterface = 0;
		
	public String i2cGet(String i2cMainAddress, String i2cSubAddress) {
		
		return "i2c-return-"+i2cGetNative(this.i2cInterface, Integer.parseInt(i2cMainAddress,16), Integer.parseInt(i2cSubAddress,16));
		
	}

    public native static int i2cGetNative(int i2cInterface, int i2cMainAddress, int i2cSubAddress);
    
    static {
//            System.loadLibrary("i2c-tools");
        System.loadLibrary("i2cget");
        System.loadLibrary("i2cset");
    } 
	
	
}
