package org.rfcx.guardian.audio.wav;

import java.io.FileInputStream;
import java.io.IOException;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;

class WavReader {

    public static byte[] fromFile(String path, int sampleRate) throws IOException {

        // Load Wav File
        // not sure on sample in bit size : 16
        TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(sampleRate,16,1,true,false);
        UniversalAudioInputStream audioInputStream = new UniversalAudioInputStream( new FileInputStream(path), audioFormat);

        byte[] buffer = new byte[(int) audioInputStream.getFrameLength()];
        audioInputStream.read(buffer, 0, 4096);
        
        return buffer;
    }
}
