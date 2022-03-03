package org.rfcx.guardian.audio.flac;

import org.rfcx.guardian.audio.EncodeStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FLACStreamEncoder {

    public EncodeStatus encode(File inputFile, File outputFile, int sampleRate, int channels, int bitsPerSample) throws IOException {
        String outputFilePath = outputFile.getAbsolutePath();
        init(outputFilePath, sampleRate, channels, bitsPerSample);

        ByteBuffer byteBuffer = audioToByteBuffer(inputFile);
        int bufferSize = byteBuffer.limit();

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
        RandomAccessFile randomAccessFile = new RandomAccessFile(audio, "rw");
        FileChannel fileChannel = randomAccessFile.getChannel();

        ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 44, fileChannel.size() - 44);

        fileChannel.close();
        randomAccessFile.close();

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
