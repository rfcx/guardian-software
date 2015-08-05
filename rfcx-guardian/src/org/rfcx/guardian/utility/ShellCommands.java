package org.rfcx.guardian.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class ShellCommands {

	private static final String TAG = "RfcxGuardian-"+ShellCommands.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public void killProcessByName(Context context, String searchTerm) {
		Log.i(TAG, "Attempting to kill process associated with search term '"+searchTerm+"'.");
		executeCommandAsRoot("kill $(ps | grep -v .updater | grep "+searchTerm+" | cut -d \" \" -f 5)",null,context);
	}
	
	public boolean executeCommandAsRoot(String commandContents, String outputSearchString, Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		FileUtils fileUtils = new FileUtils();
	    String filePath = app.getApplicationContext().getFilesDir().toString()+"/txt/script.sh";
	    File fileObj = new File(filePath);
	    fileObj.mkdirs();
	    fileUtils.chmod(new File(app.getApplicationContext().getFilesDir().toString()+"/txt"), 0755);
	    if (fileObj.exists()) { fileObj.delete(); }
	    boolean commandSuccess = false;
	    Process commandProcess = null;
	    try {
	    	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        outFile.write(
	        		"#!/system/xbin/bash"
	        		+"\n"+commandContents
	        		+"\n");
	        outFile.close();
	        fileUtils.chmod(new File(filePath), 0755);
		    if ((new File(filePath)).exists()) {
		    	if (outputSearchString != null) {
			    	commandProcess = Runtime.getRuntime().exec(new String[] {"su", "-c", filePath});
					BufferedReader reader = new BufferedReader (new InputStreamReader(commandProcess.getInputStream()));
					String eachLine; while ((eachLine = reader.readLine()) != null) {
						if (eachLine.equals(outputSearchString)) { commandSuccess = true; }
					}
		    	} else {
		    		commandProcess = (new ProcessBuilder("su", "-c", filePath)).start();
		    		commandProcess.waitFor();
		    		commandSuccess = true;
		    	}
		    } else {
		    	Log.e(TAG,"Shell script could not be located for execution");
		    }
	    } catch (IOException e) {
	    	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    } catch (InterruptedException e) {
	    	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	    return commandSuccess;
	}
	
}
