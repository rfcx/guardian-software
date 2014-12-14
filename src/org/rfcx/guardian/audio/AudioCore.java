package org.rfcx.guardian.audio;


public class AudioCore {

	private static final String TAG = AudioCore.class.getSimpleName();

	public String cacheDir = null;
	public String wavDir = null;
	public String preEncodeWavDir = null;
	public String postEncodeWavDir = null;
	public String flacDir = null;
	
	public boolean KEEP_CAPTURE_FILES = false;
	
	public final int CAPTURE_LOOP_PERIOD_MS = 60000;
	public final static int CAPTURE_SAMPLE_RATE_HZ = 4000;

}
