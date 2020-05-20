package org.rfcx.guardian.utility.device;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.i2c.I2cTools;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

public class DeviceI2cUtils {

	public DeviceI2cUtils(Context context, String i2cMainAddress) {
		this.i2cMainAddress = i2cMainAddress;
//		checkSetI2cBinaries(context);
	}

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceI2cUtils");

	public static final int i2cInterface = 1; // a number, as in /dev/i2c-0 or /dev/i2c-1 or /dev/i2c-2

	private String i2cMainAddress = null;

	private String execI2cGet = null;
	private String execI2cSet = null;

	private static final long i2cCommandTimeout = 2000;


	// i2cSET

	public boolean i2cSet(List<String[]> i2cLabelsAddressesValues/*, boolean parseAsHex*/) {
		return i2cSet(i2cLabelsAddressesValues, this.i2cMainAddress/*, parseAsHex*/);
	}

	private static boolean i2cSet(List<String[]> i2cLabelsAddressesValues, String i2cMainAddress/*, boolean parseAsHex*/) {
		I2cTools i2cTools = new I2cTools();
		boolean result = false;

		for (String[] i2cRow : i2cLabelsAddressesValues) {
			try {
				int i2cAdapter = i2cTools.i2cInit(i2cInterface);
				if (i2cAdapter < 0) {
					throw new Exception("I2c Initialize failed");
				}

				String[] dataArray = {i2cRow[0], i2cRow[1]};
				result = i2cTools.i2cSetManyValues(i2cAdapter, i2cMainAddress, dataArray);

				i2cTools.i2cDeInit(i2cAdapter);

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		return result;
	}

	// i2cGET

	public long i2cGet(String subAddress, boolean parseAsHex) {
		String rtrnValAsString = i2cGetAsString(subAddress, parseAsHex);
		long rtrnVal = 0;
		if (rtrnValAsString != null) {
			try { rtrnVal = Long.parseLong(rtrnValAsString); } catch (Exception e) { RfcxLog.logExc(logTag, e); }
		}
		return rtrnVal;
	}

	public String i2cGetAsString(String subAddress, boolean parseAsHex) {
		List<String[]> i2cLabelsAndSubAddresses = new ArrayList<String[]>();
		i2cLabelsAndSubAddresses.add(new String[] { "no-label", subAddress });
		List<String[]> i2cReturn = i2cGet(i2cLabelsAndSubAddresses, this.i2cMainAddress, parseAsHex);
		String rtrnVal = null;
		if (i2cReturn.size() > 0) { try { rtrnVal = i2cReturn.get(0)[1]; } catch (Exception e) { RfcxLog.logExc(logTag, e); } }
		return rtrnVal;
	}

	public List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, boolean parseAsHex) {
		return i2cGet(i2cLabelsAndSubAddresses, this.i2cMainAddress, parseAsHex);
	}

	private static List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, String i2cMainAddress, boolean parseAsHex) {

		List<String[]> i2cLabelsAndOutputValues = new ArrayList<String[]>();
		List<String> i2cValues = new ArrayList<String>();
		I2cTools i2cTools = new I2cTools();
		try {
			int i2cAdapter = i2cTools.i2cInit(i2cInterface);
			if (i2cAdapter < 0) {
				throw new Exception("I2c Initialize failed");
			}

			for (String[] i2cRow : i2cLabelsAndSubAddresses) {
				String i2cValue = i2cTools.i2cGet(i2cAdapter, i2cMainAddress, i2cRow[1], false);
				i2cValues.add(i2cValue);
				Log.e(logTag, i2cRow[1]+" = "+i2cValue);
			}
			i2cTools.i2cDeInit(i2cAdapter);

			int lineIndex = 0;
			for (String value: i2cValues) {
				String thisLineValueAsString = value;
				if (parseAsHex && value.substring(0,2).equalsIgnoreCase("0x")) {
					thisLineValueAsString = hexToDec(value.substring(1+value.indexOf("x")))+"";/*twosComp(value.substring(1+value.indexOf("x")))+"";*/ //Long.parseLong(value.substring(1+value.indexOf("x")), 16)+"";
					Log.e(logTag, value+" = "+thisLineValueAsString);
				} else if (parseAsHex) {
					thisLineValueAsString = null;
				}
				i2cLabelsAndOutputValues.add(new String[] { i2cLabelsAndSubAddresses.get(lineIndex)[0], thisLineValueAsString } );
				lineIndex++;
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return i2cLabelsAndOutputValues;
	}

	public static Integer twosComp(String str) {
		Integer num = Integer.valueOf(str.toUpperCase(Locale.US), 16);
		return (num > 32767) ? num - 65536 : num;
	}


	private static Number hexToDec(String hex)  {
		if (hex == null) {
			throw new NullPointerException("hexToDec: hex String is null.");
		}

		// You may want to do something different with the empty string.
		if (hex.equals("")) { return Byte.valueOf("0"); }

		// If you want to pad "FFF" to "0FFF" do it here.
//		hex = hex+"FFF";

		hex = hex.toUpperCase();

		// Check if high bit is set.
		boolean isNegative =
				hex.startsWith("8") || hex.startsWith("9") ||
				hex.startsWith("A") || hex.startsWith("B") ||
				hex.startsWith("C") || hex.startsWith("D") ||
				hex.startsWith("E") || hex.startsWith("F");

		BigInteger temp;

		if (isNegative) {
			// Negative number
			temp = new BigInteger(hex, 16);
			BigInteger subtrahend = BigInteger.ONE.shiftLeft(hex.length() * 4);
			temp = temp.subtract(subtrahend);
		} else {
			// Positive number
			temp = new BigInteger(hex, 16);
		}

		// Cut BigInteger down to size.
		if (hex.length() <= 2) { return (Byte)temp.byteValue(); }
		if (hex.length() <= 4) { return (Short)temp.shortValue(); }
		if (hex.length() <= 8) { return (Integer)temp.intValue(); }
		if (hex.length() <= 16) { return (Long)temp.longValue(); }
		return temp;
	}

}
