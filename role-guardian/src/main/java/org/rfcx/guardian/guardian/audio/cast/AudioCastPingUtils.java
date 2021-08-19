package org.rfcx.guardian.guardian.audio.cast;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioCastPingUtils {

    public AudioCastPingUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
    }

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCastPingUtils");

    private RfcxGuardian app;

    public List<String> buildPingJson(boolean printJsonToLogs) throws JSONException {
        ArrayList<String> jsonList = new ArrayList<>();
        Pair<byte[], Integer> audioPair = app.audioCaptureUtils.getAudioBuffer();
        if (audioPair != null) {
            String audioString = Base64.encodeToString(audioPair.first, Base64.URL_SAFE);
            Integer audioReadSize = audioPair.second;
            JSONObject audioJsonObject = new JSONObject()
                    .put("amount", 1)
                    .put("number", 1)
                    .put("buffer", audioString)
                    .put("read_size", audioReadSize);
            if (audioJsonObject.toString().length() <= 65535) {
                jsonList.add(audioJsonObject.toString());
            } else {
                List<byte[]> audioChunks = toSmallChunk(audioPair.first, 10);
                for (int i = 0; i < audioChunks.size(); i++) {
                    int readSize = audioChunks.get(i).length;
                    String audioChunkString = Base64.encodeToString(audioChunks.get(i), Base64.URL_SAFE);
                    JSONObject audioChunkJsonObject = new JSONObject()
                            .put("amount", audioChunks.size())
                            .put("number", i + 1)
                            .put("buffer", audioChunkString)
                            .put("read_size", readSize);
                    jsonList.add(audioChunkJsonObject.toString());
                }
            }
        }
        if (printJsonToLogs) {
            int limitLogsTo = 1800;
            StringBuilder result = new StringBuilder("[");
            for (String str : jsonList) {
                result.append(str).append(",");
            }
            result.append("]");
            Log.d(logTag, (result.length() <= limitLogsTo) ? result.toString() : result.substring(0, limitLogsTo) + "...");
        }
        return jsonList;
    }

    private List<byte[]> toSmallChunk(byte[] buffer, int number) {
        int sizeOfChunk = buffer.length / number;
        ArrayList<byte[]> resultChunk = new ArrayList<>();
        int i = 0;
        while (i < buffer.length) {
            resultChunk.add(Arrays.copyOfRange(buffer, i, Math.min(buffer.length, i + sizeOfChunk)));
            i += sizeOfChunk;
        }
        return resultChunk;
    }
}
