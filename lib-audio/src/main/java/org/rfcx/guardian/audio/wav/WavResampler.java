package org.rfcx.guardian.audio.wav;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.resample.RateTransposer;
import be.tarsos.dsp.writer.WriterProcessor;

public class WavResampler {



	public static boolean resampleWavWithGain(String inputFilePath, String outputFilePath, int inputSampleRate, int outputSampleRate, double outputGain) throws IOException {

		// Load Wav File
		TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(inputSampleRate,16,1,true,false);
		FileInputStream fileInputStream = new FileInputStream(inputFilePath);
		UniversalAudioInputStream audioInputStream = new UniversalAudioInputStream(fileInputStream, audioFormat);
		AudioDispatcher dispatcher = new AudioDispatcher(audioInputStream, 1024, 0);

		// Run Audio Gain
		// ...

		// Run Re-Sampler
		AudioProcessor rateTransposer = new RateTransposer(Double.parseDouble(outputSampleRate+"") / Double.parseDouble(inputSampleRate+"") );
		dispatcher.addAudioProcessor(rateTransposer);

		// Write to Wav File
		RandomAccessFile outputFile = new RandomAccessFile(outputFilePath, "rw");
		TarsosDSPAudioFormat outputFormat = new TarsosDSPAudioFormat(outputSampleRate, 16, 1, true, false);
		WriterProcessor wavWriter = new WriterProcessor(outputFormat, outputFile);
		dispatcher.addAudioProcessor(wavWriter);
		dispatcher.run();

		return (new File(outputFilePath)).exists();

	}
	
	
}
