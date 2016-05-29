package org.rfcx.guardian.audio.opus;

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

	public String transcode( String inputFile, String outputFile, int bitRate, int quality) {

		int result = encodeOpusFile( inputFile, outputFile, bitRate/1000, quality);

		return result == 0 ? "OK" : "ERROR";  
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
