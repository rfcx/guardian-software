package org.rfcx.guardian.audio.flac;

import java.io.File;
import java.nio.ByteBuffer;

public class FLACStreamEncoder {

    public String encode(File outputFile, int sampleRate, int channels, int bitsPerSample, ByteBuffer buffer, int bufferSize) {
        String outputFilePath = outputFile.getAbsolutePath();
        init(outputFilePath, sampleRate, channels, bitsPerSample);
        int result = write(buffer, bufferSize);
        if (result == bufferSize){
            deinit();
            return "OK";
        } else {
            deinit();
            return "ERROR";
        }
    }

    private long mObject;

    //To initialize encoder
    native private void init(String outputFile, int sampleRate, int channels, int bitsPerSample);

    //To get rid of encoder
    native private void deinit();

    //To write the byte of audio
    native private int write(ByteBuffer buffer, int buffSize);

    //To flush encoder
    native private void flush();

    static {
        System.loadLibrary("ogg");
        System.loadLibrary("flac");
        System.loadLibrary("flacenc");
    }
}
