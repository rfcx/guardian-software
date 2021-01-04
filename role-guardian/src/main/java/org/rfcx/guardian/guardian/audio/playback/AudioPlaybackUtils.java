package org.rfcx.guardian.guardian.audio.playback;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.audio.EncodeStatus;
import org.rfcx.guardian.audio.flac.FLACStreamEncoder;
import org.rfcx.guardian.audio.opus.OpusAudioEncoder;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioPlaybackUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioPlaybackUtils");

	public static final int PLAYBACK_FAILURE_SKIP_THRESHOLD = 3;




}
