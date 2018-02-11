package admin.device.sentinel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.content.Context;
import android.util.Log;

public class I2cUtils {

	public I2cUtils(Context context, String i2cMainAddress) {
		this.i2cMainAddress = i2cMainAddress;
		checkSetI2cBinaries(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, I2cUtils.class);

	private String i2cInterface = "0";
	private String i2cMainAddress = null;
	
	private String execI2cGet = null;
	private String execI2cSet = null;
	
	public String i2cGet(String i2cSubAddress) {
		try {
			Process i2cGetProc = Runtime.getRuntime().exec(new String[] { this.execI2cGet, "-y", this.i2cInterface, this.i2cMainAddress, i2cSubAddress, "w" });
			BufferedReader lineReader = new BufferedReader (new InputStreamReader(i2cGetProc.getInputStream()));
			String eachLine; while ((eachLine = lineReader.readLine()) != null) {
				if (eachLine.trim().length() > 0) { 
					return eachLine;
				}
			}
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	private void checkSetI2cBinaries(Context context) {
		
		String binaryDir = (new StringBuilder()).append(context.getFilesDir().toString()).append("/bin").toString();
		(new File(binaryDir)).mkdirs(); 
		FileUtils.chmod(binaryDir, 0777);
		
		this.execI2cGet = (new StringBuilder()).append(binaryDir).append("/i2cget").toString();
		this.execI2cSet = (new StringBuilder()).append(binaryDir).append("/i2cset").toString();
		
		if (!(new File(this.execI2cGet)).exists()) {
    			try {
    				InputStream inputStream = context.getAssets().open("i2cget");
    				OutputStream outputStream = new FileOutputStream(this.execI2cGet);
    				byte[] buf = new byte[1024]; int len; while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
    				inputStream.close(); outputStream.close();
    				FileUtils.chmod(this.execI2cGet, 0755);
    			} catch (IOException e) {
    				RfcxLog.logExc(logTag, e);
    			}
		}

		if (!(new File(this.execI2cSet)).exists()) {
			try {
				InputStream inputStream = context.getAssets().open("i2cset");
				OutputStream outputStream = new FileOutputStream(this.execI2cSet);
				byte[] buf = new byte[1024]; int len; while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
				inputStream.close(); outputStream.close();
				FileUtils.chmod(this.execI2cSet, 0755);
			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
	}
		
	}

	
	
	
}
