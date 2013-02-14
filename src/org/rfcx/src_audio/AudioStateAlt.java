package org.rfcx.src_audio;

import java.util.Arrays;

import org.rfcx.rfcx_src_android.RfcxSource;

import com.badlogic.gdx.audio.analysis.*;

import android.util.Log;

public class AudioStateAlt {
	
	private static final String TAG = AudioStateAlt.class.getSimpleName();
	
	private static final boolean AUDIO_ENABLED = true;
	
	public static final int CAPTURE_SAMPLE_RATE = 22050;
	public static final int FFT_RESOLUTION = 4096;

	public static final int BUFFER_LENGTH = FFT_RESOLUTION*2;
	
	private float[] buffer = new float[BUFFER_LENGTH];
	private float[] spectrum = new float[BUFFER_LENGTH / 2];
	
	public void addFrame(short[] samples, RfcxSource rfcxSource) {
		if (samples.length == BUFFER_LENGTH) {
			spectrum = fft(samples);
			
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < spectrum.length; i++) {
				sb.append("\t").append((int) Math.round(spectrum[i]));
			}
			Log.d(TAG, sb.toString());
		}
	}
	
	private float[] fft(short[] array) {
		float[] fft_cpx, tmpr, tmpi;
		float[] res = new float[BUFFER_LENGTH / 2];
		// float[] mod_spec =new float[array.length/2];
		float[] real_mod = new float[BUFFER_LENGTH];
		float[] imag_mod = new float[BUFFER_LENGTH];
		double[] real = new double[BUFFER_LENGTH];
		double[] imag = new double[BUFFER_LENGTH];
		double[] mag = new double[BUFFER_LENGTH];
		double[] phase = new double[BUFFER_LENGTH];
		float[] new_array = new float[BUFFER_LENGTH];
		
		// Zero Pad signal
		for (int i = 0; i < BUFFER_LENGTH; i++) {
			if (i < array.length) {
				new_array[i] = (float) array[i];
			} else {
				new_array[i] = 0;
			}
		}
		 
		FFT fft = new FFT(BUFFER_LENGTH, CAPTURE_SAMPLE_RATE);
		 
		fft.forward(new_array);
		fft_cpx = fft.getSpectrum();
		tmpi = fft.getImaginaryPart();
		tmpr = fft.getRealPart();
		for (int i = 0; i < new_array.length; i++) {
			real[i] = (double) tmpr[i];
			imag[i] = (double) tmpi[i];
			 
			mag[i] = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
			phase[i] = Math.atan2(imag[i], real[i]);
			 
			/**** Reconstruction ****/
			real_mod[i] = (float) (mag[i] * Math.cos(phase[i]));
			imag_mod[i] = (float) (mag[i] * Math.sin(phase[i]));
		}
		fft.inverse(real_mod, imag_mod, res);
		return res;
	}
	
	
	public static boolean isAudioEnabled() {
		return AUDIO_ENABLED;
	}
	
}
