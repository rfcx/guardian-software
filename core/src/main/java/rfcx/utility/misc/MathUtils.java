package rfcx.utility.misc;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.util.Base64;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class MathUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", MathUtils.class);
	
	public static double limitValueToSpecificDecimalPlaces(double val, int decimalPlaces) {
		double _val = 0;
		BigDecimal bd = new BigDecimal(val);
		bd = bd.round(new MathContext(decimalPlaces));
		return bd.doubleValue();
	}
	
	
}
