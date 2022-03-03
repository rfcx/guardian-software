package org.rfcx.guardian.utility.device.control;

import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;

public class DeviceWallpaper {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceWallpaper");



	public static void setWallpaper(Context context, int resourceId) {

		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

		try {

			Bitmap myBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
			Bitmap bitmapResized = Bitmap.createScaledBitmap(myBitmap, 600, 300, false);
			wallpaperManager.setBitmap(bitmapResized);

		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
}
