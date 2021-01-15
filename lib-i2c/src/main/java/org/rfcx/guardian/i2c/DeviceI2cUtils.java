package org.rfcx.guardian.i2c;

import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DeviceI2cUtils {

	public DeviceI2cUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceI2cUtils");
	}

	private String logTag;

	private I2cTools i2cTools;
	private int i2cAdapterReceipt;

	// i2cInterface should be a low integer, including zero, as in /dev/i2c-0 or /dev/i2c-1 or /dev/i2c-2
	private int i2cInterface = 0;

	public void setInterface(int i2cInterface) {
		this.i2cInterface = i2cInterface;
	}

	public void initializeOrReInitialize() {

		Log.i(logTag, "Attempting to initialize I2C interface '/dev/i2c-"+this.i2cInterface+"'");

		if (isI2cHandlerAccessible()) {

			this.i2cTools = new I2cTools();
			this.i2cTools.i2cDeInit(this.i2cInterface);
			this.i2cAdapterReceipt = i2cTools.i2cInit(this.i2cInterface);

			if (isInitialized(true)) {
				Log.i(logTag, "I2C interface '/dev/i2c-" + this.i2cInterface + "' successfully initialized.");
			}

		} else {
			Log.e(logTag, "I2C handler '/dev/i2c-"+this.i2cInterface+"' is NOT accessible. Initialization failed.");
		}
	}

	public boolean isInitialized(boolean printFeedbackInLog) {

		boolean isInitialized = (this.i2cAdapterReceipt >= 0) && isI2cHandlerAccessible();

		if (printFeedbackInLog && !isInitialized) {
			Log.e(logTag, "I2C interface '/dev/i2c-"+this.i2cInterface+"' is NOT initialized.");
		}

		return isInitialized;
	}

	private void throwExceptionIfNotInitialized() throws Exception {
		if (!isInitialized(false)) {
			throw new Exception("I2C Initialization Failed");
		}
	}

	public boolean isI2cHandlerAccessible() {
		return (new File("/dev/i2c-"+this.i2cInterface)).canRead();
	}

	// i2cSET

	public boolean i2cSet(List<String[]> i2cLabelsAddressesValues, String mainAddr/*, boolean parseAsHex*/) {

		boolean isSet = false;

		for (String[] i2cRow : i2cLabelsAddressesValues) {

			try {

				throwExceptionIfNotInitialized();

				isSet = this.i2cTools.i2cSet(i2cAdapterReceipt, mainAddr, i2cRow[1], i2cRow[2], true);

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		return isSet;
	}

	// i2cGET

	public long i2cGetAsLong(String subAddr, String mainAddr, boolean parseAsHex) {
		String rtrnValAsString = i2cGetAsString(subAddr, mainAddr, parseAsHex);
		long rtrnVal = 0;
		if (rtrnValAsString != null) {
			try { rtrnVal = Long.parseLong(rtrnValAsString); } catch (Exception e) { RfcxLog.logExc(logTag, e); }
		}
		return rtrnVal;
	}

	public String i2cGetAsString(String subAddr, String mainAddr, boolean parseAsHex) {
		List<String[]> i2cLabelsAndSubAddresses = new ArrayList<String[]>();
		i2cLabelsAndSubAddresses.add(new String[] { "no-label", subAddr });
		List<String[]> i2cReturn = i2cGet(i2cLabelsAndSubAddresses, mainAddr, parseAsHex, new String[] { } );
		String rtrnVal = null;
		if (i2cReturn.size() > 0) { try { rtrnVal = i2cReturn.get(0)[1]; } catch (Exception e) { RfcxLog.logExc(logTag, e); } }
		return rtrnVal;
	}

	public List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, String mainAddr, boolean parseAsHex) {
		return i2cGet(i2cLabelsAndSubAddresses, mainAddr, parseAsHex, new String[] { } );
	}

	public List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, String mainAddr, boolean parseAsHex, String[] rtrnValsWithoutTwosComplement) {

		List<String[]> i2cLabelsAndOutputValues = new ArrayList<String[]>();
		List<String> i2cValues = new ArrayList<String>();

		try {

			throwExceptionIfNotInitialized();

			for (String[] i2cRow : i2cLabelsAndSubAddresses) {
				String i2cValue = i2cTools.i2cGet(i2cAdapterReceipt, mainAddr, i2cRow[1], false, true);
				i2cValues.add(i2cValue);
			}

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
