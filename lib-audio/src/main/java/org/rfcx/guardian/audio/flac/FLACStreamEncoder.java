package org.rfcx.guardian.audio.flac;

import org.rfcx.guardian.audio.EncodeStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FLACStreamEncoder {

    public EncodeStatus encode(File inputFile, File outputFile, int sampleRate, int channels, int bitsPerSample) throws IOException {
        String outputFilePath = outputFile.getAbsolutePath();
        init(outputFilePath, sampleRate, channels, bitsPerSample);

        ByteBuffer byteBuffer = audioToByteBuffer(inputFile);
        int bufferSize = byteBuffer.position();

        int result = write(byteBuffer, bufferSize);
        return getEncodeStatus(result, bufferSize);
    }

    public EncodeStatus encode(String inputPath, String outputPath, int sampleRate, int channels, int bitsPerSample) throws IOException {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        return encode(inputFile, outputFile, sampleRate, channels, bitsPerSample);
    }

    private EncodeStatus getEncodeStatus(int result, int bufferSize) {
        deinit();
        return result == bufferSize ? EncodeStatus.OK : EncodeStatus.ERROR;
    }

    private ByteBuffer audioToByteBuffer(File audio) throws IOException {
        FileInputStream stream = new FileInputStream(audio);

        byte[] buff = new byte[(int)audio.length()];
        stream.read(buff, 0, buff.length);
        byte[] modBuff = Arrays.copyOfRange(buff, 44, buff.length);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(modBuff.length);
        byteBuffer.put(modBuff);

        return byteBuffer;
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
