package org.rfcx.guardian.utility.misc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ArrayUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", ArrayUtils.class);
	
	public static double[] castFloatArrayToDoubleArray(float[] arr) {
		double[] _arr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) { _arr[i] = (double) arr[i]; }
		return _arr;
	}
	
	public static float[] castDoubleArrayToFloatArray(double[] arr) {
		float[] _arr = new float[arr.length];
		for (int i = 0; i < arr.length; i++) { _arr[i] = (float) arr[i]; }
		return _arr;
	}
	
	public static double[] limitArrayValuesToSpecificDecimalPlaces(double[] arr, int decimalPlaces) {
		double[] _arr = new double[arr.length];
		BigDecimal[] bd = new BigDecimal[arr.length];
		for (int i = 0; i < arr.length; i++) {
			bd[i] = new BigDecimal(arr[i]);
			bd[i] = bd[i].round(new MathContext(decimalPlaces));
			_arr[i] = bd[i].doubleValue();
		}
		return _arr;	
	}
	
	public static double[] multiplyAllArrayValuesBy(double[] arr, double multiplier) {
		double[] _arr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) { _arr[i] = arr[i] * multiplier; }
		return _arr;
	}
	
	public static double[] divideAllArrayValuesBy(double[] arr, double divisor) {
		return multiplyAllArrayValuesBy(arr, (1 / divisor));
	}
	
	public static long[] roundArrayValuesAndCastToLong(double[] arr) {
		long[] newArr = new long[arr.length];
		for (int i = 0; i < arr.length; i++) { newArr[i] = (long) Math.round(arr[i]); }
		return newArr;
	}
	
	// calculate averages of values within arrays
	
	public static double getAverageAsDouble(double[] arr) {
		return getAverage(toList(arr));
	}
	
	public static long getAverageAsLong(double[] arr) {
		return (long) Math.round(getAverage(toList(arr)));
	}
	
	public static double getAverageAsDouble(long[] arr) {
		return getAverage(toList(arr));
	}
	
	public static long getAverageAsLong(long[] arr) {
		return (long) Math.round(getAverage(toList(arr)));
	}
	
	public static double getAverageAsDouble(int[] arr) {
		return getAverage(toList(arr));
	}
	
	public static long getAverageAsLong(int[] arr) {
		return (long) Math.round(getAverage(toList(arr)));
	}
	
	public static double[] getAverageValuesAsArrayFromArrayList(List<double[]> arrLst) {
		int lstLen = arrLst.size();
		if (lstLen > 0) {
			int arrLen = arrLst.get(0).length;
			double[] arr = new double[arrLen];
			for (int i = 0; i < arrLen; i++) {
				arr[i] = getAverageOfIndexWithinArrayList(arrLst, i);
			}
			return arr;
		}
		return new double[] {};
	}
	
	private static double getAverage(List<Double> lst) {
		double sum = 0;
		for (double val : lst) { sum = sum + val; }
		return sum / lst.size();
	}
	
	private static double getAverageOfIndexWithinArrayList(List<double[]> arrLst, int index) {
		double sum = 0;
		for (double[] vals : arrLst) {
			if (vals.length > index) {
				sum = sum + vals[index];
			}
		}
		return sum / arrLst.size();
	}
	
	// convert arrays to lists
	
	private static List<Double> toList(double[] arr) {
		List<Double> lst = new ArrayList<Double>();
		for (double val : arr) { lst.add(val); }
		return lst;
	}
	
	private static List<Double> toList(long[] arr) {
		List<Double> lst = new ArrayList<Double>();
		for (long val : arr) { lst.add((double) val); }
		return lst;
	}
	
	private static List<Double> toList(int[] arr) {
		List<Double> lst = new ArrayList<Double>();
		for (int val : arr) { lst.add((double) val); }
		return lst;
	}
	
	
}
