package org.rfcx.guardian.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.util.Log;

public class ShellCommands {

	private static final String logTag = "Rfcx-Utils-"+ShellCommands.class.getSimpleName();
	
	public static void killProcessByName(Context context, String searchTerm, String excludeTerm) {
		Log.i(logTag, "Attempting to kill process associated with search term '"+searchTerm+"'.");
		String grepExclude = (excludeTerm != null) ? " grep -v "+excludeTerm+" |" : "";
		executeCommand("kill $(ps |"+grepExclude+" grep "+searchTerm+" | cut -d \" \" -f 5)", null, true, context);
	}
	
	public static boolean executeCommand(String commandContents, String outputSearchString, boolean asRoot, Context context) {
	    String filePath = context.getFilesDir().toString()+"/txt/script.sh";
	    (new File(filePath.substring(0,filePath.lastIndexOf("/")))).mkdirs();
	    File fileObj = new File(filePath);
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
	        FileUtils.chmod(fileObj, 0755);
		    if (fileObj.exists()) {
		    	if (outputSearchString != null) {
		    		if (asRoot) { commandProcess = Runtime.getRuntime().exec(new String[] { "su", "-c", filePath }); }
		    		else { commandProcess = Runtime.getRuntime().exec(new String[] { filePath }); }
					BufferedReader reader = new BufferedReader (new InputStreamReader(commandProcess.getInputStream()));
					String eachLine; while ((eachLine = reader.readLine()) != null) {
						if (eachLine.equals(outputSearchString)) { commandSuccess = true; }
					}
		    	} else {
		    		if (asRoot) { commandProcess = (new ProcessBuilder("su", "-c", filePath)).start(); }
		    		else { commandProcess = (new ProcessBuilder(filePath)).start(); } 
		    		commandProcess.waitFor();
		    		commandSuccess = true;
		    	}
		    } else {
		    	Log.e(logTag,"Shell script could not be located for execution");
		    }
	    } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
	    } catch (InterruptedException e) {
			RfcxLog.logExc(logTag, e);
		}
	    return commandSuccess;
	}
	
	public static void triggerNeedForRootAccess(Context context) {
		executeCommand("pm list features",null,true,context);
	}
	
}
