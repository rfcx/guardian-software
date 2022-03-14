package org.rfcx.guardian.audio.wav;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.resample.RateTransposer;
import be.tarsos.dsp.writer.WriterProcessor;

public class WavUtils {

    private static final int READ_BUFFER = 1024;

    public static void copyWavFile(String inputFilePath, String outputFilePath, int sampleRate) throws IOException {

        if (!(new File(outputFilePath)).exists()) {

            // Load Wav File
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);
            UniversalAudioInputStream audioInputStream = new UniversalAudioInputStream(new FileInputStream(inputFilePath), audioFormat);
            audioInputStream.skip(44);
            AudioDispatcher dispatcher = new AudioDispatcher(audioInputStream, READ_BUFFER, 0);

            // Write to Wav File
            TarsosDSPAudioFormat outputFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);
            WriterProcessor wavWriter = new WriterProcessor(outputFormat, new RandomAccessFile(outputFilePath, "rw"));
            dispatcher.addAudioProcessor(wavWriter);
            dispatcher.run();

        }

    }

    public static void resampleWavWithGain(String inputFilePath, String outputFilePath, int inputSampleRate, int outputSampleRate, double outputGain) throws IOException {

        if (!(new File(outputFilePath)).exists()) {

            // Load Wav File
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(inputSampleRate, 16, 1, true, false);
            UniversalAudioInputStream audioInputStream = new UniversalAudioInputStream(new FileInputStream(inputFilePath), audioFormat);
            AudioDispatcher dispatcher = new AudioDispatcher(audioInputStream, READ_BUFFER, 0);

            // Run Audio Gain
            if (outputGain != 1) {
                dispatcher.addAudioProcessor(new GainProcessor(outputGain));
            }

            // Run Re-Sampler
            dispatcher.addAudioProcessor(new RateTransposer(((double) outputSampleRate) / ((double) inputSampleRate)));

            // Write to Wav File
            TarsosDSPAudioFormat outputFormat = new TarsosDSPAudioFormat(outputSampleRate, 16, 1, true, false);
            WriterProcessor wavWriter = new WriterProcessor(outputFormat, new RandomAccessFile(outputFilePath, "rw"));
            dispatcher.addAudioProcessor(wavWriter);
            dispatcher.run();
        }

    }


}
