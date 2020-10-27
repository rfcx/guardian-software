package org.rfcx.guardian.i2c;

import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DeviceI2cUtils {

	public DeviceI2cUtils(String i2cMainAddress) {
		this.i2cMainAddress = i2cMainAddress;
	}

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceI2cUtils");

	// i2cInterface should be a number, as in /dev/i2c-0 or /dev/i2c-1 or /dev/i2c-2
	public static final int i2cInterface = 1;

	private String i2cMainAddress = null;

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
					throw new Exception("I2c Initialization Failed");
				}

				result = i2cTools.i2cSet(i2cAdapter, i2cMainAddress, i2cRow[1], i2cRow[2], true);

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
		List<String[]> i2cReturn = i2cGet(i2cLabelsAndSubAddresses, this.i2cMainAddress, parseAsHex, new String[] { } );
		String rtrnVal = null;
		if (i2cReturn.size() > 0) { try { rtrnVal = i2cReturn.get(0)[1]; } catch (Exception e) { RfcxLog.logExc(logTag, e); } }
		return rtrnVal;
	}

	public List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, boolean parseAsHex) {
		return i2cGet(i2cLabelsAndSubAddresses, this.i2cMainAddress, parseAsHex, new String[] { } );
	}

	public List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, boolean parseAsHex, String[] rtrnValsWithoutTwosComplement) {
		return i2cGet(i2cLabelsAndSubAddresses, this.i2cMainAddress, parseAsHex, rtrnValsWithoutTwosComplement );
	}

	private static List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, String i2cMainAddress, boolean parseAsHex, String[] rtrnValsWithoutTwosComplement) {

		List<String[]> i2cLabelsAndOutputValues = new ArrayList<String[]>();
		List<String> i2cValues = new ArrayList<String>();
		I2cTools i2cTools = new I2cTools();
		try {
			int i2cAdapter = i2cTools.i2cInit(i2cInterface);
			if (i2cAdapter < 0) {
				throw new Exception("I2c Initialization Failed");
			}

			for (String[] i2cRow : i2cLabelsAndSubAddresses) {
				String i2cValue = i2cTools.i2cGet(i2cAdapter, i2cMainAddress, i2cRow[1], false, true);
				i2cValues.add(i2cValue);
			}
			i2cTools.i2cDeInit(i2cAdapter);

			int lineIndex = 0;
			for (String i2cValue: i2cValues) {

				String i2cStrValue = i2cValue;

				if (parseAsHex && i2cValue.substring(0,2).equalsIgnoreCase("0x")) {

					if (!ArrayUtils.doesStringArrayContainString(rtrnValsWithoutTwosComplement, i2cLabelsAndSubAddresses.get(lineIndex)[0])) {
						i2cStrValue = twosComplementHexToDec(i2cValue.substring(1 + i2cValue.indexOf("x"))) + "";
					} else {
						i2cStrValue = ""+Integer.parseInt(i2cValue.substring(1 + i2cValue.indexOf("x")),16);;
					}

				} else if (parseAsHex) {
					i2cStrValue = null;
				}

				i2cLabelsAndOutputValues.add(new String[] { i2cLabelsAndSubAddresses.get(lineIndex)[0], i2cStrValue } );
				lineIndex++;
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return i2cLabelsAndOutputValues;
	}

	private static Number twosComplementHexToDec(String hexStr)  {

		if (hexStr == null) {
			throw new NullPointerException("twosComplementHexToDec: hex String is null.");
		}

		if (hexStr.equals("")) { return Byte.valueOf("0"); }

		// If you want to pad "FFF" to "0FFF" do it here.
//		hex = hex+"FFF";

		hexStr = hexStr.toUpperCase();

		BigInteger numVal;

		//	Check if high bit is set.
//		if (	hexStr.startsWith("8") || hexStr.startsWith("9")
//			||	hexStr.startsWith("A") || hexStr.startsWith("B")
//			||	hexStr.startsWith("C") || hexStr.startsWith("D")
//			||	hexStr.startsWith("E") || hexStr.startsWith("F")
//		) {
//			// Negative number
//			numVal = new BigInteger(hexStr, 16);
//			BigInteger subtrahend = BigInteger.ONE.shiftLeft(hexStr.length() * 4);
//			numVal = numVal.subtract(subtrahend);
//		} else {
			// Positive number
			numVal = new BigInteger(hexStr, 16);
//		}
//
		// Cut BigInteger down to size and return value
		if (hexStr.length() <= 2) { return numVal.byteValue(); }
		if (hexStr.length() <= 4) { return numVal.shortValue(); }
		if (hexStr.length() <= 8) { return numVal.intValue(); }
		if (hexStr.length() <= 16) { return numVal.longValue(); }
		return numVal;
	}

	public static long twosComplementHexToDecAsLong(String hexStr)  {
		return Long.parseLong(twosComplementHexToDec(hexStr)+"");
	}




}
