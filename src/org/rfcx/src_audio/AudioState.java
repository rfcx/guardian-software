package org.rfcx.src_audio;

import android.util.Log;
import ca.uol.aig.fftpack.RealDoubleFFT;

public class AudioState {
	
	private static final String TAG = AudioState.class.getSimpleName();
	
	public int audioCaptureSampleRate = 22050;
	public int fftBlockSize = 4096;
	private RealDoubleFFT fftObj = new RealDoubleFFT(fftBlockSize);
	private double[] audioSpectrum = new double[fftBlockSize];
	private double[] audioSpectrumAvg = new double[fftBlockSize];
	private double[] audioSpectrumFreq = new double[fftBlockSize];
	
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
	
	public void addFrame(double[] audioFrame) {
		this.audioSpectrum = audioFrame;
		fftObj.ft(audioSpectrum);
		repAvg++;
		spectrumAverageIncrement();
	}
	
	private void spectrumAverageIncrement() {
		repAvg++;
		divAvg = (repAvg == cntAvg) ? (cntAvg / fftBlockSize) : 1;
		for (int i = 0; i < fftBlockSize; i++) {
			audioSpectrumAvg[i] = ( audioSpectrumAvg[i] + Math.abs(audioSpectrum[i]) ) / divAvg;
		}
		if (repAvg == cntAvg) {
//			Log.d(TAG, ""+audioSpectrumAvg[1024]);
			repAvg = 0;
			audioSpectrumAvg = new double[fftBlockSize];
		}
	}

	
}
