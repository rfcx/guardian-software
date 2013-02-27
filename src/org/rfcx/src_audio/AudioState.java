package org.rfcx.src_audio;

import org.rfcx.rfcx_src_android.RfcxSource;

import com.badlogic.gdx.audio.analysis.*;

import android.util.Log;

public class AudioState {

	private static final String TAG = AudioState.class.getSimpleName();

	private static final boolean AUDIO_ENABLED = true;

	public static final int CAPTURE_SAMPLE_RATE = 22050;
	public static final int FFT_RESOLUTION = 4096;

	// private double[] fftSpectrumSingle = new double[BUFFER_LENGTH];
	private double[] fftSpectrumSum = new double[BUFFER_LENGTH];
	private int fftSpectrumSumIncrement = 0;
	private static final int fftSpectrumSumLength = 10;
	private static final int fftSpectrumDivisor = 1000;
	public static final int BUFFER_LENGTH = FFT_RESOLUTION * 2;
	
	private double[] fftWindowingCoeff = new double[BUFFER_LENGTH];

	public void addSpectrum(short[] pcmData, RfcxSource rfcxSource) {
		if (pcmData.length == BUFFER_LENGTH) {
			addSpectrumSum(calcFFT(pcmData));
		} else {
			Log.d(TAG, "Skipping FFT, PCM data not correct length.");
		}
	}

	private void addSpectrumSum(double[] fftSpectrum) {
		fftSpectrumSumIncrement++;

		for (int i = 0; i < fftSpectrum.length; i++) {
			fftSpectrumSum[i] = fftSpectrumSum[i] + fftSpectrum[i];
		}

		if (fftSpectrumSumIncrement == fftSpectrumSumLength) {
//			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < fftSpectrumSum.length; i++) {
				long lvl = Math.round(fftSpectrumSum[i] / fftSpectrumSumLength
						/ fftSpectrumDivisor);
//				sb.append("\t");
//				for (int j = 0; j < lvl; j++) {
//					sb.append("|");
//				}
			}
//			Log.d(TAG, sb.toString());
			fftSpectrumSum = new double[BUFFER_LENGTH];
			fftSpectrumSumIncrement = 0;
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
			if (i < array.length) {
				new_array[i] = (float) array[i];
			} else {
				new_array[i] = 0;
			}
		}

		FFT fft = new FFT(BUFFER_LENGTH, CAPTURE_SAMPLE_RATE);
		fft.forward(new_array);
		float[] fft_cpx = fft.getSpectrum();
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
		return mag;
	}

	public static boolean isAudioEnabled() {
		return AUDIO_ENABLED;
	}

	private void calcWindowing() {
		for (int i = 0; i < BUFFER_LENGTH; i++) {
			fftWindowingCoeff[i] = 0.5 * (1 - Math.cos((2 * Math.PI * i) / (BUFFER_LENGTH - 1)));
		}
	}
}
