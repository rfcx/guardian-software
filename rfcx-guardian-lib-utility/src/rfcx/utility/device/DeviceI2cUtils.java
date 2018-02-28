package rfcx.utility.device;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceI2cUtils {

	public DeviceI2cUtils(Context context, String i2cMainAddress) {
		this.i2cMainAddress = i2cMainAddress;
		checkSetI2cBinaries(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceI2cUtils.class);

	private static final int i2cInterface = 0; // as in /dev/ic2-0
	private String i2cMainAddress = null;
	
	private String execI2cGet = null;
//	private String execI2cSet = null;
	
	public long i2cGet(String[] i2cLabelAndSubAddress) {
		List<String[]> i2cLabelsAndSubAddresses = new ArrayList<String[]>();
		i2cLabelsAndSubAddresses.add(i2cLabelAndSubAddress);
		List<String[]> i2cReturn = i2cGet(i2cLabelsAndSubAddresses, this.execI2cGet, this.i2cMainAddress);
		return Long.parseLong(i2cReturn.get(0)[1]);
	}
	
	public List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses) {
		return i2cGet(i2cLabelsAndSubAddresses, this.execI2cGet, this.i2cMainAddress);
	}
	
	private static List<String[]> i2cGet(List<String[]> i2cLabelsAndSubAddresses, String execI2cGet, String i2cMainAddress) {

		List<String[]> i2cLabelsAndOutputValues = new ArrayList<String[]>(); 
		try {
			Process i2cShellProc = Runtime.getRuntime().exec("sh");
			DataOutputStream dataOutputStream = new DataOutputStream(i2cShellProc.getOutputStream());
			BufferedReader lineReader = new BufferedReader (new InputStreamReader(i2cShellProc.getInputStream()));
			
			for (String[] i2cRow : i2cLabelsAndSubAddresses) {
				dataOutputStream.writeBytes((new StringBuilder()).append(execI2cGet).append(" -y ").append(i2cInterface).append(" ").append(i2cMainAddress).append(" ").append(i2cRow[1]).append(" w;\n").toString());
				dataOutputStream.flush();
			}
			dataOutputStream.writeBytes("exit;\n");
			dataOutputStream.flush();
			
			String lineContent; int lineIndex = 0; 
			while ((lineContent = lineReader.readLine()) != null) { 
				String thisLine = lineContent.trim();
				if (thisLine.length() > 0) {
					i2cLabelsAndOutputValues.add(new String[] { i2cLabelsAndSubAddresses.get(lineIndex)[0], ""+Long.parseLong(thisLine.substring(1+thisLine.indexOf("x")), 16) } );
					lineIndex++;
				}
			}
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
		return i2cLabelsAndOutputValues;
	}
	
	private void checkSetI2cBinaries(Context context) {
		
		String binaryDir = (new StringBuilder()).append(context.getFilesDir().toString()).append("/bin").toString();
		(new File(binaryDir)).mkdirs(); 
		FileUtils.chmod(binaryDir, 0777);
		
		this.execI2cGet = (new StringBuilder()).append(binaryDir).append("/i2cget").toString();
		
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

//		this.execI2cSet = (new StringBuilder()).append(binaryDir).append("/i2cset").toString();
//
//		if (!(new File(this.execI2cSet)).exists()) {
//			try {
//				InputStream inputStream = context.getAssets().open("i2cset");
//				OutputStream outputStream = new FileOutputStream(this.execI2cSet);
//				byte[] buf = new byte[1024]; int len; while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
//				inputStream.close(); outputStream.close();
//				FileUtils.chmod(this.execI2cSet, 0755);
//			} catch (IOException e) {
//				RfcxLog.logExc(logTag, e);
//			}
//		}
		
	}
	
	public static void resetI2cPermissions(Context context, String appRole) {
		Log.v(logTag, "Resetting Permissions on I2C Handler...");
		(new ShellCommands(context, appRole))
			.executeCommandAsRootAndIgnoreOutput( 
				(new StringBuilder()).append("chmod 666 /dev/i2c-").append(i2cInterface).append(";").toString() 
			);
	}

	
	
	
}
