package org.rfcx.guardian.guardian.audio.capture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;
import android.util.Pair;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.io.RandomAccessFile;

public class AudioCaptureWavRecorder {

    public static final boolean RECORDING_UNCOMPRESSED = true;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureWavRecorder");

    // 	INITIALIZING : recorder is initializing; 
    //	READY : recorder has been initialized, recorder not yet started
    //	RECORDING : recording 
    //	ERROR : reconstruction needed 
    //	STOPPED: reset needed
    // The interval in which the recorded samples are output to the file used only in uncompressed mode
//	private static final int TIMER_INTERVAL_UNCOMPRESSED = 480;
    private static final int TIMER_INTERVAL_UNCOMPRESSED = 333;

    ;
    private static State recorderState;
    public byte[] uncompressedOutputBuffer; // Buffer for output (only in uncompressed mode)
    public int readSize;
    private AudioRecord audioRecorder = null;
    private boolean isRecordingUncompressed; //on/off; RECORDING_UNCOMPRESSED / RECORDING_COMPRESSED
    private String recorderOutputFilePath = null;
    private RandomAccessFile recorderOutputFileRandomAccessWriter;
    private int recorderFileOutputFramePeriod; // Number of frames written to file on each output (only in uncompressed mode)
    private Boolean isAudioChanged = false;
    // Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size (see AudioFormat)
    private short captureChannelCount;
    private int captureSampleRate;
    private short captureSampleSizeInBits;
    private int captureBufferSize;
    private int captureAudioSource;
    private int captureAudioFormat;
    // Number of bytes written to file after header(only in uncompressed mode) after stop() is called, this size is written to the header/data chunk in the wave file
    private int captureFilePayloadSizeInBytes;
    /*
     *
     * Method used for recording.
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {

        public void onPeriodicNotification(AudioRecord recorder) {
            readSize = audioRecorder.read(uncompressedOutputBuffer, 0, uncompressedOutputBuffer.length); // Fill buffer
            try {
                recorderOutputFileRandomAccessWriter.write(uncompressedOutputBuffer); // Write buffer to file
                isAudioChanged = true;
                captureFilePayloadSizeInBytes += uncompressedOutputBuffer.length;
            } catch (IOException e) {
                RfcxLog.logExc(logTag, e);
                stopRecorder();
                recorderState = State.ERROR;
            }
        }

        public void onMarkerReached(AudioRecord recorder) {
            // NOT USED
        }
    };

    /**
     * Default constructor
     * <p>
     * Instantiates a new recorder, in case of compressed recording the
     * parameters can be left as 0. In case of errors, no exception is thrown,
     * but the state is set to ERROR
     *
     * @throws Exception
     */
    public AudioCaptureWavRecorder(boolean isUncompressed, int audioSource, int sampleRate, int channelConfig, int audioFormat) throws Exception {

        isRecordingUncompressed = isUncompressed;
        if (isRecordingUncompressed) { // RECORDING_UNCOMPRESSED

            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                captureSampleSizeInBits = 16;
            } else {
                captureSampleSizeInBits = 8;
            }

            if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) {
                captureChannelCount = 1;
            } else {
                captureChannelCount = 2;
            }

            captureAudioSource = audioSource;
            captureSampleRate = sampleRate;
            captureAudioFormat = audioFormat;

            recorderFileOutputFramePeriod = sampleRate * TIMER_INTERVAL_UNCOMPRESSED / 1000;
            captureBufferSize = recorderFileOutputFramePeriod * 2 * captureSampleSizeInBits * captureChannelCount / 8;
            if (captureBufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {

                // Check to make sure buffer size is not smaller than the smallest allowed one
                captureBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                // Set frame period and timer interval accordingly
                recorderFileOutputFramePeriod = captureBufferSize / (2 * captureSampleSizeInBits * captureChannelCount / 8);

                Log.w(logTag, "Increasing buffer size to " + Integer.toString(captureBufferSize));
            }

            audioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, captureBufferSize);

            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                recorderState = State.ERROR;
                throw new Exception("AudioRecord initialization failed");
            }

            audioRecorder.setRecordPositionUpdateListener(updateListener);
            audioRecorder.setPositionNotificationPeriod(recorderFileOutputFramePeriod);
        }
        recorderOutputFilePath = null;
        recorderState = State.INITIALIZING;
    }

    public static AudioCaptureWavRecorder getInstance(int audioSampleRate) throws Exception {
        AudioCaptureWavRecorder captureWavRecorderResult = null;
        do {
            captureWavRecorderResult = new AudioCaptureWavRecorder(true, AudioSource.MIC, audioSampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        } while (!(captureWavRecorderResult.getRecorderState() == AudioCaptureWavRecorder.State.INITIALIZING));
        return captureWavRecorderResult;
    }

    //	Returns the state of the recorder in a RehearsalAudioRecord.State typed object. Useful, as no exceptions are thrown.
    public static State getRecorderState() {
        return recorderState;
    }

    /*
     * Method for getting audio buffer for companion
     */
    public Pair<byte[], Integer> getAudioBuffer() {
        isAudioChanged = false;
        return new Pair<>(uncompressedOutputBuffer, readSize);
    }

    public Boolean isAudioChanged() {
        return isAudioChanged;
    }

    public void setOutputFile(String outputFilePath) {
        try {
            if (recorderState == State.INITIALIZING) {
                recorderOutputFilePath = outputFilePath;
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
            recorderState = State.ERROR;
        }
    }

    /**
     * Prepares the recorder for recording, in case the recorder is not in the
     * INITIALIZING state and the file path was not set the recorder is set to
     * the ERROR state, which makes a reconstruction necessary. In case
     * uncompressed recording is toggled, the header of the wave file is
     * written. In case of an exception, the state is changed to ERROR
     *
     * @throws IOException
     */
    public void prepareRecorder() throws IOException {
//		try {
        if (recorderState == State.INITIALIZING) {

            if (audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {

                prepareRecorderOutputFileWriter(recorderOutputFilePath);

                uncompressedOutputBuffer = new byte[recorderFileOutputFramePeriod * captureSampleSizeInBits / 8 * captureChannelCount];

                recorderState = State.READY;

            } else {
                Log.e(logTag, "prepareRecorder() method called on uninitialized recorder");
                recorderState = State.ERROR;
            }

        } else {
            Log.e(logTag, "prepareRecorder() method called on illegal state");
            releaseRecorder();
            recorderState = State.ERROR;
        }
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//			recorderState = State.ERROR;
//		}
    }

    private void prepareRecorderOutputFileWriter(String outputFilePath) throws IOException {

        if (outputFilePath != null) {

            recorderOutputFileRandomAccessWriter = new RandomAccessFile(recorderOutputFilePath, "rw");

            // Set file length to 0, to prevent unexpected behavior in case the file already existed
            recorderOutputFileRandomAccessWriter.setLength(0);

            recorderOutputFileRandomAccessWriter.writeBytes("RIFF");

            // Final file size not known yet, write 0
            recorderOutputFileRandomAccessWriter.writeInt(0);

            recorderOutputFileRandomAccessWriter.writeBytes("WAVE");
            recorderOutputFileRandomAccessWriter.writeBytes("fmt ");

            // Sub-chunk size, 16 for PCM
            recorderOutputFileRandomAccessWriter.writeInt(Integer.reverseBytes(16));

            // AudioFormat, 1 for PCM
            recorderOutputFileRandomAccessWriter.writeShort(Short.reverseBytes((short) 1));

            // Number of channels, 1 for mono, 2 for stereo
            recorderOutputFileRandomAccessWriter.writeShort(Short.reverseBytes(captureChannelCount));

            // Sample rate
            recorderOutputFileRandomAccessWriter.writeInt(Integer.reverseBytes(captureSampleRate));

            // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
            recorderOutputFileRandomAccessWriter.writeInt(Integer.reverseBytes(captureSampleRate * captureSampleSizeInBits * captureChannelCount / 8));

            // Block align, NumberOfChannels*BitsPerSample/8
            recorderOutputFileRandomAccessWriter.writeShort(Short.reverseBytes((short) (captureChannelCount * captureSampleSizeInBits / 8)));

            // Bits per sample
            recorderOutputFileRandomAccessWriter.writeShort(Short.reverseBytes(captureSampleSizeInBits));
            recorderOutputFileRandomAccessWriter.writeBytes("data");

            // Data chunk size not known yet, write 0
            recorderOutputFileRandomAccessWriter.writeInt(0);

        } else {
            recorderState = State.ERROR;
        }

    }

    /**
     * Releases the resources associated with this class, and removes the
     * unnecessary files, when necessary
     */
    public void releaseRecorder() {

        if (recorderState == State.RECORDING) {
            stopRecorder();

        } else if (isRecordingUncompressed && (recorderState == State.READY)) {

            try {
                recorderOutputFileRandomAccessWriter.close(); // Remove prepared file

            } catch (IOException e) {
                RfcxLog.logExc(logTag, e);
            }
//			(new File(recorderOutputFilePath)).delete();
        }

        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    /**
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped. In
     * case of exceptions the class is set to the ERROR state.
     */
    public void resetRecorder() {
        try {
            if (recorderState != State.ERROR) {
                releaseRecorder();
                recorderOutputFilePath = null; // Reset file path

                audioRecorder = new AudioRecord(captureAudioSource, captureSampleRate, captureChannelCount + 1, captureAudioFormat, captureBufferSize);

                recorderState = State.INITIALIZING;
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
            recorderState = State.ERROR;
        }
    }

    /**
     * Starts the recording, and sets the state to RECORDING. Call after
     * prepare().
     */
    public void startRecorder() {

        if (recorderState == State.READY) {

            captureFilePayloadSizeInBytes = 0;
            audioRecorder.startRecording();
            recorderState = State.RECORDING;

        } else {
            Log.e(logTag, "startRecorder() called on illegal recorderState");
            recorderState = State.ERROR;
        }
    }

    /**
     * Stops the recording, and sets the state to STOPPED. In case of further
     * usage, a reset is needed. Also finalizes the wave file in case of
     * uncompressed recording.
     */
    public void stopRecorder() {
        Log.d(logTag, recorderState.toString());
        //TODO: For now it just silly fixed , need to fix it better than this
//		if (recorderState == State.RECORDING) {

        audioRecorder.stop();

        try {
            recorderOutputFileRandomAccessWriter.seek(4);        // Write size to RIFF header
            recorderOutputFileRandomAccessWriter.writeInt(Integer.reverseBytes(36 + captureFilePayloadSizeInBytes));
            recorderOutputFileRandomAccessWriter.seek(40);        // Write size to Subchunk2Size field
            recorderOutputFileRandomAccessWriter.writeInt(Integer.reverseBytes(captureFilePayloadSizeInBytes));
            recorderOutputFileRandomAccessWriter.close();

        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
            recorderState = State.ERROR;
        }

        recorderState = State.STOPPED;

//		} else {
//			Log.e(logTag, "stopRecorder() called on illegal recorderState");
//			recorderState = State.ERROR;
//		}
    }

    /*
     *
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     */
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }

    public void swapOutputFile(String outputFilePath) throws IOException {

//		try {
        stopRecorder();
        recorderOutputFilePath = outputFilePath;
        recorderOutputFileRandomAccessWriter.close();
        recorderState = State.INITIALIZING;
        prepareRecorder();
        startRecorder();
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}

    }

    public void haltRecording() throws IOException {

//		try {
        stopRecorder();
        recorderOutputFileRandomAccessWriter.close();
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}

    }

    public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED}

}
