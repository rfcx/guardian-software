package org.rfcx.guardian.audio.wav;

import java.io.File;
import java.io.IOException;

public class WavWriter {

    public static void createSnippet(double[] buffer, int sampleRate, String outputPath) throws IOException, WavFileException {
        WavFile writeWavFile = WavFile.newWavFile(new File(outputPath), 1, buffer.length, 16, sampleRate);
        writeWavFile.writeFrames(buffer, buffer.length);
        writeWavFile.close();
    }
}
