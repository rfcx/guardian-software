package org.rfcx.guardian.audio.mp3;

public class Mp3AudioEncoder {

	// -------- publicly visible part ----------
//	transcode(
//  String inputFile,
//  String outputFile,
//  String outputFormat,
//  String outputCodecLibrary,
//  int outputBitRate,
//  int outputSampleRate
//);
	
	// quality=0..9;  0=best (very slow)..9=worst; 2 near-best quality, not too slow; 5 good quality, fast; 7 ok quality, really fast;
	public String transcode( String inFile, String outFile, int outBitRate, int outQuality) {

		int result = encodeFile( inFile, outFile, outBitRate / 1000, outQuality);

		return result == 0 ? "OK" : "ERROR";  
	}

	private native int encodeFile(String sourcePath, String targetPath, int bitRate, int quality);

	static {
		System.loadLibrary("mp3lame");
	}
}
