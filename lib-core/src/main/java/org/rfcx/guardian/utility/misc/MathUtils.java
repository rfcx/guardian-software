package org.rfcx.guardian.utility.misc;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.math.BigDecimal;
import java.math.MathContext;

public class MathUtils {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "MathUtils");

    public static double limitValueToSpecificDecimalPlaces(double val, int decimalPlaces) {
        double _val = 0;
        BigDecimal bd = new BigDecimal(val);
        bd = bd.round(new MathContext(decimalPlaces));
        return bd.doubleValue();
    }

//	public static String limitValueToSpecificDecimalPlacesAsStr(double val, int decimalPlaces) {
//		double _val = 0;
//		BigDecimal bd = new BigDecimal(val);
//		bd = bd.round(new MathContext(decimalPlaces));
//		return ""+bd.doubleValue();
//	}


}
