package org.rfcx.guardian.audio.opus;

import org.rfcx.guardian.audio.EncodeStatus;

import java.io.File;

public class OpusAudioEncoder {

    // -------- publicly visible part ----------
//	transcode(
//  String inputFile,
//  String outputFile,
//  String outputFormat,
//  String outputCodecLibrary,
//  int outputBitRate,
//  int outputSampleRate
//);

    public EncodeStatus transcode(String inputPath, String outputPath, int bitRate, int quality) {

        int result = encodeOpusFile(inputPath, outputPath, bitRate / 1000, quality);

        return result == 0 ? EncodeStatus.OK : EncodeStatus.ERROR;
    }

    public EncodeStatus transcode(File inputPath, File outputPath, int bitRate, int quality) {

        return transcode(inputPath.getAbsolutePath(), outputPath.getAbsolutePath(), bitRate, quality);
    }

    private native int encodeOpusFile(String sourcePath, String targetPath, int bitRate, int quality);

    // Which libraries to include and their dependencies.
    // Note that the order is very important. Place libraries that are not dependent
    // on anything first, and only place dependent libraries after the dependencies have
    // already been included. 
    static {
        System.loadLibrary("ogg");
        System.loadLibrary("opus");
        System.loadLibrary("opusenc");
    }
}
