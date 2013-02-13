package org.rfcx.src_audio;

import org.rfcx.rfcx_src_android.RfcxSource;

import com.badlogic.gdx.audio.analysis.FFT;


import android.util.Log;

public class AudioStateAlt {
	
	private static final String TAG = AudioStateAlt.class.getSimpleName();
	
	private static final boolean AUDIO_ENABLED = true;
	
	public static final int CAPTURE_SAMPLE_RATE = 8000;
	public static final int FFT_RESOLUTION = 16;

	
	float[] array = { 1, 6, 1, 4, 5, 0, 8, 7, 8, 6, 1, 0, 5, 6, 1, 8 };
	float[] buff;
	float[] buff_audio;
	float[] new_sig;
	
	
	public void addFrame(double[] samples, RfcxSource rfcxSource) {
		
//		new_sig = fft(FFT_RESOLUTION, CAPTURE_SAMPLE_RATE, array);
		
		Log.d(TAG, "asdf:" +samples.length);
		
//		if (samples.length == (FFT_RESOLUTION * 2)) {
//			KissFFT kissFFT = new KissFFT( FFT_RESOLUTION * 2 );
//			shortSamples.get(audioFrame);
//			kissFFT.spectrum(shortSamples, floatSpectrum);
//			float[] thisFloat = floatSpectrum.array();
//
//			Log.d(TAG, ""+thisFloat.length);
//			
//			kissFFT.dispose();
//		}
		
		
		
	}
	
//	private float[] fft(int N, int fs, float[] array) {
//		float[] fft_cpx, tmpr, tmpi;
//		float[] res = new float[N / 2];
//		// float[] mod_spec =new float[array.length/2];
//		float[] real_mod = new float[N];
//		float[] imag_mod = new float[N];
//		double[] real = new double[N];
//		double[] imag = new double[N];
//		double[] mag = new double[N];
//		double[] phase = new double[N];
//		float[] new_array = new float[N];
//		// Zero Pad signal
//		for (int i = 0; i < N; i++) {
//		 
//		if (i < array.length) {
//		new_array[i] = array[i];
//		} else {
//		new_array[i] = 0;
//		}
//		}
//		 
//		FFT fft = new FFT(N, 8000);
//		 
//		fft.forward(new_array);
//		fft_cpx = fft.getSpectrum();
//		tmpi = fft.getImaginaryPart();
//		tmpr = fft.getRealPart();
//		for (int i = 0; i < new_array.length; i++) {
//		real[i] = (double) tmpr[i];
//		imag[i] = (double) tmpi[i];
//		 
//		mag[i] = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
//		phase[i] = Math.atan2(imag[i], real[i]);
//		 
//		/**** Reconstruction ****/
//		real_mod[i] = (float) (mag[i] * Math.cos(phase[i]));
//		imag_mod[i] = (float) (mag[i] * Math.sin(phase[i]));
//		 
//		}
//		fft.inverse(real_mod, imag_mod, res);
//		return res;
//		 
//		}
//	
	
	public static boolean isAudioEnabled() {
		return AUDIO_ENABLED;
	}
	
}
