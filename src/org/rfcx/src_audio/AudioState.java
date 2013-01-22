package org.rfcx.src_audio;

import java.text.DecimalFormat;

import android.util.Log;
import ca.uol.aig.fftpack.RealDoubleFFT;

public class AudioState {
	
	private static final String TAG = AudioState.class.getSimpleName();
	
	public int audioCaptureSampleRate = 22050;
	public int fftBlockSize = 4096;
	private RealDoubleFFT fftObj = new RealDoubleFFT(fftBlockSize);
	private double[] audioSpectrum = new double[fftBlockSize];
	private double[] audioSpectrumAvg = new double[fftBlockSize];
	public double[] audioSpectrumFreq = setAudioSpectrumFreq();
	
	DecimalFormat decimalFormat = new DecimalFormat("#");
	
	private double cntAvg = 10;
	private double divAvg = 1;
	private double repAvg = 0;
	
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
			audioSpectrumAvg[i] = ( audioSpectrumAvg[i] + audioSpectrum[i] ) / divAvg;
		}
		if (repAvg == cntAvg) {
			repAvg = 0;
			audioSpectrumAvg = new double[fftBlockSize];
		}
	}
	
	private double[] setAudioSpectrumFreq() {
		double[] audioSpecFreq = new double[fftBlockSize];
		for (int i = 0; i < fftBlockSize; i++) {
			audioSpecFreq[i] = (audioCaptureSampleRate / 2) * ((i+1) / (double) fftBlockSize);
		}
		return audioSpecFreq;
	}
	
}
