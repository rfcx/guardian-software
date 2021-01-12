package org.rfcx.guardian.audio.wav;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import be.tarsos.dsp.resample.Resampler;

public class WavResampler {



	public static void resampleWav(String inputFilePath, String outputFilePath, int inputSampleRate, int outputSampleRate, double outputGain) throws IOException {

//		if (!(new File(inputFilePath)).exists()) { return false; } else if ((new File(outputFilePath)).exists()) { return true; }
//
//		FileInputStream inputStream = new FileInputStream(inputFilePath);
//		OutputStream outputStream = new FileOutputStream(outputFilePath);


		if (inputSampleRate >= outputSampleRate) {

			Resampler resample = new Resampler(true, 1, 1);


	//		ssrc.downsample(inputStream, outputStream, 1, 16, 16, inputSampleRate, outputSampleRate, outputGain, 0, true, 0);

		} else {

	//		ssrc.upsample(inputStream, outputStream, 1, 16, 16, inputSampleRate, outputSampleRate, outputGain, 0, true, 0);
		}

//		return (new File(outputFilePath)).exists();

	}
	
	
}
