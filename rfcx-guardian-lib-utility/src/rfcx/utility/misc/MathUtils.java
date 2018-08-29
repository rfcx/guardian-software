package rfcx.utility.misc;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

	
	public static double getAverageAsDouble(double[] values) {
		return getAvg(toList(values));
	}
	
	public static long getAverageAsLong(double[] values) {
		return (long) Math.round(getAvg(toList(values)));
	}
	
	public static double getAverageAsDouble(long[] values) {
		return getAvg(toList(values));
	}
	
	public static long getAverageAsLong(long[] values) {
		return (long) Math.round(getAvg(toList(values)));
	}
	
	public static double getAverageAsDouble(int[] values) {
		return getAvg(toList(values));
	}
	
	public static long getAverageAsLong(int[] values) {
		return (long) Math.round(getAvg(toList(values)));
	}
	
	private static double getAvg(List<Double> values) {
		double valueSum = 0;
		for (double val : values) {
			valueSum = valueSum + val;
		}
		return valueSum / values.size();
	}
	
	
	private static List<Double> toList(double[] values) {
		List<Double> list = new ArrayList<Double>();
		for (double val : values) { list.add(val); }
		return list;
	}
	
	private static List<Double> toList(long[] values) {
		List<Double> list = new ArrayList<Double>();
		for (long val : values) { list.add((double) val); }
		return list;
	}
	
	private static List<Double> toList(int[] values) {
		List<Double> list = new ArrayList<Double>();
		for (int val : values) { list.add((double) val); }
		return list;
	}
	
	
}
