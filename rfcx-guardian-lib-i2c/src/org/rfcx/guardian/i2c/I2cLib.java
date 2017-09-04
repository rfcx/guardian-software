package org.rfcx.guardian.i2c;

import java.io.File;

public class I2cLib {
	
	    public native static int writeI2C();
	    
	    static {
            System.loadLibrary("org_rfcx_guardian_I2cLib");
	    } 
	
	
}
