package org.rfcx.src_audio;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.util.Log;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class AudioState {
	
	private static final String TAG = AudioState.class.getSimpleName();
	
	private static final boolean AUDIO_ENABLED = true;
	
	public int audioCaptureSampleRate = 8000;
	public static final int fftBlockSize = 256;
	private RealDoubleFFT fftObj = new RealDoubleFFT(fftBlockSize);
	private double[] fftSpectrum = new double[fftBlockSize];
	private double[] audioSpectrumAvg = new double[fftBlockSize/2];
	private double[] audioSpectrumFreq = new double[fftBlockSize/2];
	
	private double cntAvg = 10;
	private double divAvg = 1;
	private double repAvg = 0;
	
	public double getFrequencyByIndex(int index) {
		if (audioSpectrumFreq[0] == 0) {
			for (int i = 0; i < fftBlockSize; i++) {
				audioSpectrumFreq[i] = (audioCaptureSampleRate / 2) * ((i+1) / (double) fftBlockSize);
			}
		}
		return audioSpectrumFreq[index];
	}
	
	public void addFrame(double[] audioFrame, RfcxSource rfcxSource) {
		this.fftSpectrum = audioFrame;
		fftObj.ft(fftSpectrum);
		repAvg++;
		spectrumAverageIncrement(rfcxSource);
	}
	
	private void spectrumAverageIncrement(RfcxSource rfcxSource) {
		repAvg++;
//		divAvg = (repAvg == cntAvg) ? (cntAvg / fftBlockSize) : 1;
		
//		for (int i = 0; i < fftBlockSize; i++) {
//			audioSpectrumAvg[i] = ( audioSpectrumAvg[i] + Math.abs(audioSpectrum[i]) ) / divAvg;
//		}
//		if (repAvg == cntAvg) {
//			Log.d(TAG, "spectrumAverageIncrement");
//			rfcxSource.audioDb.dbSpectrum.insert(audioSpectrumAvg);
//			repAvg = 0;
//			audioSpectrumAvg = new double[fftBlockSize];
//		}
	}

	public static boolean isAudioEnabled() {
		return AUDIO_ENABLED;
	}
	
}
