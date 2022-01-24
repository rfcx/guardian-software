package org.rfcx.guardian.utility.network;

import android.content.Context;
import android.util.Log;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class UploadSpeedTest extends HttpPostMultipart {

    public UploadSpeedTest(Context context, String appRole) {
        super(context, appRole);
        this.context = context;
        this.logTag = RfcxLog.generateLogTag(appRole, "UploadSpeedTest");
    }

    private final Context context;
    private final String logTag;

    public double getUploadSpeedTest(String fullUrl, int size) throws IOException, NoSuchAlgorithmException, KeyManagementException {

        String testPath = context.getFilesDir().getAbsolutePath() + "test_upload.txt";
        File file = new File(testPath);
        if (file.exists()) {
            file.delete();
        }
        RandomAccessFile rf = new RandomAccessFile(file, "rw");
        rf.setLength(size);

        MultipartEntity requestEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        requestEntity.addPart("file", new FileBody(file));

        long startTime = System.currentTimeMillis();
        Log.v(logTag,"Sending "+ FileUtils.bytesAsReadableString(requestEntity.getContentLength())+" to "+fullUrl);
        String result = executeMultipartPost(fullUrl, requestEntity, true);

        if (result == null) {
            return -1.0;
        }
        long uploadTime = System.currentTimeMillis() - startTime;
        double uploadSpeed = (size * 1.0) / uploadTime;
        Log.v(logTag,"Completed (" + DateTimeUtils.milliSecondDurationAsReadableString(uploadTime) +") from "+fullUrl);
        return uploadSpeed;
    }
}
