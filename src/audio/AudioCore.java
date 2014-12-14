package audio;


public class AudioCore {

	private static final String TAG = AudioCore.class.getSimpleName();

	public String cacheDir = null;
	public String wavDir = null;
	public String preEncodeWavDir = null;
	public String postEncodeWavDir = null;
	public String flacDir = null;
	
	public static final boolean KEEP_CAPTURE_FILES = false;
	
	public static final int CAPTURE_SAMPLE_RATE = 4000;

}
