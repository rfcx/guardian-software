package org.rfcx.src_audio;

import java.util.ArrayList;
import java.util.Calendar;

import com.badlogic.gdx.audio.analysis.FFT;

import android.util.Log;

public class AudioState {

	private static final String TAG = AudioState.class.getSimpleName();

	public static final boolean CAPTURE_SERVICE_ENABLED = true;
	public static final boolean PROCESS_SERVICE_ENABLED = true;

	public static final int CAPTURE_SAMPLE_RATE = 4000;
	public static final int FFT_RESOLUTION = 2048;
	public static final int BUFFER_LENGTH = FFT_RESOLUTION * 2;

	private static final int FFT_SPEC_SUM_SAMPLES = 10;
	private int fftSpecSumIncr = 0;
	private double[] fftSpecSum = new double[FFT_RESOLUTION];
	
	private float fftFreqRatio = (CAPTURE_SAMPLE_RATE / 2) / FFT_RESOLUTION;
	private int[] fftSpecSendRange = { 60, 1900 };
	
	private float[] fftWindowingCoeff = calcWindowingCoeff();

	private static final int PCM_DATA_BUFFER_LIMIT = 1200;
	private ArrayList<short[]> pcmDataBuffer = new ArrayList<short[]>();

	private static final int FFT_SEND_BUFFER_LIMIT = 200;
	private ArrayList<String[]> fftSendBuffer = new ArrayList<String[]>();
	private ArrayList<Calendar> fftSendBufferTimestamps = new ArrayList<Calendar>();
	
	
	public void addSpectrum() {
		if (pcmBufferLength() > 1) {
//			short[] pcmData = new short[BUFFER_LENGTH];
//			System.arraycopy(pcmDataBuffer.get(0), 0, pcmData, 0, BUFFER_LENGTH/2);
//			System.arraycopy(pcmDataBuffer.get(1), 0, pcmData, BUFFER_LENGTH/2, BUFFER_LENGTH/2);
//			addSpectrumSum(calcFFT(pcmData));
//			pcmDataBuffer.remove(0);
//			pcmDataBuffer.remove(1);
			
			addSpecSum(calcFFT(this.pcmDataBuffer.get(0)));
			pcmDataBuffer.remove(0);

			checkResetPcmDataBuffer();
		}
	}
	
	private void addSpecSum(double[] fftSpec) {
		fftSpecSumIncr++;
		
		for (int i = 0; i < fftSpecSum.length; i++) {
			fftSpecSum[i] = fftSpecSum[i] + fftSpec[i];
		}

		if (fftSpecSumIncr == FFT_SPEC_SUM_SAMPLES) {
			double[] fftSpecTmp = new double[FFT_RESOLUTION];
			for (int i = 0; i < fftSpecSum.length; i++) {
				fftSpecTmp[i] = Math.round(fftSpecSum[i] / FFT_SPEC_SUM_SAMPLES);
			}
			cacheFFT(fftSpecTmp);
//			Log.d(TAG,"Added spectrum to buffer (rfcx)");
			fftSpecSum = new double[FFT_RESOLUTION];
			fftSpecSumIncr = 0;
		}
	}

	private double[] calcFFT(short[] array) {

		double[] real = new double[BUFFER_LENGTH];
		double[] imag = new double[BUFFER_LENGTH];
		double[] mag = new double[BUFFER_LENGTH];
		float[] new_array = new float[BUFFER_LENGTH];
		
		// For reconstruction
		// float[] real_mod = new float[BUFFER_LENGTH];
		// float[] imag_mod = new float[BUFFER_LENGTH];
		// double[] phase = new double[BUFFER_LENGTH];
		// float[] res = new float[BUFFER_LENGTH / 2];

		// Zero pad signal
		for (int i = 0; i < BUFFER_LENGTH; i++) {
			new_array[i] = (i < array.length) ? (fftWindowingCoeff[i]*array[i]) : 0;
		}
		
		try {
			FFT fft = new FFT(BUFFER_LENGTH, CAPTURE_SAMPLE_RATE);
			fft.forward(new_array);
	//		float[] fft_cpx = fft.getSpectrum();
			float[] tmpi = fft.getImaginaryPart();
			float[] tmpr = fft.getRealPart();
			for (int i = 0; i < new_array.length; i++) {
				real[i] = (double) tmpr[i];
				imag[i] = (double) tmpi[i];
				mag[i] = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
				/**** Reconstruction ****/
				// phase[i] = Math.atan2(imag[i], real[i]);
				// real_mod[i] = (float) (mag[i] * Math.cos(phase[i]));
				// imag_mod[i] = (float) (mag[i] * Math.sin(phase[i]));
			}
			// fft.inverse(real_mod, imag_mod, res);
		} catch (NoClassDefFoundError e) {
			Log.e(TAG, e.getMessage());
		}
		return mag;
	}

	private float[] calcWindowingCoeff() {
		float[] windowingCoeff = new float[BUFFER_LENGTH];
		for (int i = 0; i < BUFFER_LENGTH; i++) {
			double coeff = (0.5 * (1 - Math.cos((2 * Math.PI * i) / (BUFFER_LENGTH - 1))));
			windowingCoeff[i] = (float) coeff;
		}
		return windowingCoeff;
	}
	
	private void checkResetPcmDataBuffer() {
		if (pcmBufferLength() >= PCM_DATA_BUFFER_LIMIT) {
			this.pcmDataBuffer = new ArrayList<short[]>();
			Log.d(TAG,"PCM Data Buffer at limit. Buffer cleared.");
		}
	}
	
	private void checkResetFFTSendBuffer() {
		if (fftSendBufferLength() >= FFT_SEND_BUFFER_LIMIT) {
			this.fftSendBuffer = new ArrayList<String[]>();
			Log.d(TAG,"FFT Send Buffer at limit. Buffer cleared.");
		}
	}
	
	public void cachePcmBuffer(short[] pcmData) {
		if ((pcmData.length == BUFFER_LENGTH) && (pcmBufferLength() < PCM_DATA_BUFFER_LIMIT)) {
//			short[] halfBuffer = new short[BUFFER_LENGTH/2];
//			System.arraycopy(pcmData, 0, halfBuffer, 0, BUFFER_LENGTH/2);
//			this.pcmDataBuffer.add(halfBuffer);
//			System.arraycopy(pcmData, BUFFER_LENGTH/2, halfBuffer, 0, BUFFER_LENGTH/2);
//			this.pcmDataBuffer.add(halfBuffer);
			
			this.pcmDataBuffer.add(pcmData);
		} else {
			checkResetPcmDataBuffer();
		}
	}
	
	public void cacheFFT(double[] fftSpec) {
		if (fftSendBufferLength() < FFT_SEND_BUFFER_LIMIT) {
			String[] specHex = new String[fftSpec.length];
			for (int i = 0; i < fftSpec.length; i++) {
				specHex[i] = Long.toHexString(Math.round(fftSpec[i]));
			}
			this.fftSendBuffer.add(specHex);
			this.fftSendBufferTimestamps.add(Calendar.getInstance());
		} else {
			checkResetFFTSendBuffer();
		}
	}
	
	public int pcmBufferLength() {
		return this.pcmDataBuffer.size();
	}
	
	public int fftSendBufferLength() {
		return this.fftSendBuffer.size();
	}
	
	public ArrayList<String[]> getFftSendBufferUpTo(int getUpTo) {
		ArrayList<String[]> returnArray = new ArrayList<String[]>();
		for (int i = 0; i < getUpTo; i++) {
			try {
				returnArray.add(fftSendBuffer.get(i));
			} catch (IndexOutOfBoundsException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return returnArray;
	}
	
	public ArrayList<Calendar> getFftSendBufferTimestampsUpTo(int getUpTo) {
		ArrayList<Calendar> returnArray = new ArrayList<Calendar>();
		for (int i = 0; i < getUpTo; i++) {
			try {
				returnArray.add(fftSendBufferTimestamps.get(i));
			} catch (IndexOutOfBoundsException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return returnArray;
	}
	
	public void clearFFTSendBufferUpTo(int clearUpTo) {
		for (int i = 0; i < clearUpTo; i++) {
			try {
				this.fftSendBuffer.remove(0);
				this.fftSendBufferTimestamps.remove(0);
			} catch (IndexOutOfBoundsException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
}
