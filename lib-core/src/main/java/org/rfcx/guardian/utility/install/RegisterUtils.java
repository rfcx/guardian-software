package org.rfcx.guardian.utility.install;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.io.File;

public class RegisterUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "InstallUtils");

	public static JSONObject parseRegisterJson(String jsonStr) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj = new JSONObject(jsonStr);

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return jsonObj;
	}

	
}
