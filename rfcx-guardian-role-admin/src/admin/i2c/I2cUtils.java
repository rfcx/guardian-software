package admin.i2c;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.content.Context;

public class I2cUtils {

	public I2cUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, I2cUtils.class);

	private RfcxGuardian app = null;
	
	public void i2cTest() {
		ShellCommands.executeCommand("/sdcard/rfcx/i2cget -y 0 0x68 0x4a w > /sdcard/rfcx/test.txt", null, true, app.getApplicationContext());
	}
	
}
