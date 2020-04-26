package org.rfcx.guardian.utility.misc;

import java.math.BigDecimal;
import java.math.MathContext;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class MathUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "MathUtils");
	
	public static double limitValueToSpecificDecimalPlaces(double val, int decimalPlaces) {
		double _val = 0;
		BigDecimal bd = new BigDecimal(val);
		bd = bd.round(new MathContext(decimalPlaces));
		return bd.doubleValue();
	}
	
	
}
