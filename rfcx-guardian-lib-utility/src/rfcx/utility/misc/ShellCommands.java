package rfcx.utility.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class ShellCommands {
	
	public ShellCommands(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, ShellCommands.class);
		this.context = context;
	}

	private String logTag = RfcxLog.generateLogTag("Utils", ShellCommands.class);
	private Context context = null;
	
	private static List<String> executeCommandInShell(String commandContents, String ifOutputContainThisStringReturnTrue, boolean asRoot, Context context, String logTag) {

	    List<String> standardOutStringLines = new ArrayList<String>();
	    standardOutStringLines.add("false");
	    
		String filePath = context.getFilesDir().toString()+"/scr/script"+(asRoot ? "-root" : "")+".sh";
	    (new File(filePath.substring(0,filePath.lastIndexOf("/")))).mkdirs();
	    File fileObj = new File(filePath);
	    	
	    if (fileObj.exists()) { 
	    		long waitToExecute = 500;
	    		Log.e(logTag,"OTHER SCRIPT IS RUNNING SO WAITING "+waitToExecute+"ms BEFORE STARTING..."); 
	//    		try { Thread.sleep(waitToExecute); } catch (InterruptedException e) { RfcxLog.logExc(logTag, e); }
		}
	    
	    try {

		    Process commandProcess = null;
	    		BufferedWriter scriptFile = new BufferedWriter(new FileWriter(filePath));
	        scriptFile.write(
	        		"#!/system/bin/sh"
	        		+"\n"+commandContents
	        		+"\n");
	        scriptFile.close();
	        FileUtils.chmod(fileObj, 0755);
	        
		    if (fileObj.exists()) {
		    	
		    		if (asRoot) { commandProcess = Runtime.getRuntime().exec(new String[] { "su", "-c", filePath }); }
		    		else { commandProcess = Runtime.getRuntime().exec(new String[] { filePath }); }
		    		
				commandProcess.waitFor();
					
				BufferedReader bufferedReaderOutput = new BufferedReader (new InputStreamReader(commandProcess.getInputStream())); 
				String eachLine_BufferedReaderOutput; while ((eachLine_BufferedReaderOutput = bufferedReaderOutput.readLine()) != null) {
					standardOutStringLines.add(eachLine_BufferedReaderOutput.trim());
					if (eachLine_BufferedReaderOutput.equalsIgnoreCase(ifOutputContainThisStringReturnTrue)) {
						standardOutStringLines.set(0, "true");
					}
				} bufferedReaderOutput.close();
				
//					BufferedReader bufferedReaderError = new BufferedReader (new InputStreamReader(commandProcess.getErrorStream())); 
//					String eachLine_BufferedReaderError; while ((eachLine_BufferedReaderError = bufferedReaderError.readLine()) != null) {
//						standardOutStringLines.add(eachLine_BufferedReaderError.trim());
//					} bufferedReaderError.close();
				
				if (ifOutputContainThisStringReturnTrue == null) { standardOutStringLines.set(0, (commandProcess.exitValue() == 1) ? "true" : "false"); }
				commandProcess.destroy();

		    } else {
		    		Log.e(logTag,"Shell script could not be located for execution");
		    }
	    } catch (Exception e) {
			RfcxLog.logExc(logTag, e);
	    } finally {
	    		if (fileObj.exists()) { fileObj.delete(); }
	    }
	    
		    
	    return standardOutStringLines;
	}
	
	private static boolean executeCommandInShell_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue, boolean asRoot, Context context, String logTag) {
		return executeCommandInShell(commandContents, ifOutputContainThisStringReturnTrue, asRoot, context, logTag).get(0).equals("true");
	}
	
	public List<String> executeCommand(String commandContents) { 
		return executeCommandInShell(commandContents, null, false, this.context, this.logTag);
	}

	public boolean executeCommandAndIgnoreOutput(String commandContents) { 
		return executeCommandInShell_ReturnBoolean(commandContents, null, false, this.context, this.logTag);
	}

	public List<String> executeCommandAsRoot(String commandContents) { 
		return executeCommandInShell(commandContents, null, true, this.context, this.logTag);
	}

	public boolean executeCommandAsRootAndIgnoreOutput(String commandContents) { 
		return executeCommandInShell_ReturnBoolean(commandContents, null, true, this.context, this.logTag);
	}
	
	public boolean executeCommandAndSearchOutput(String commandContents, String outputSearchString) { 
		return executeCommandInShell_ReturnBoolean(commandContents, outputSearchString, false, this.context, this.logTag);
	}

	public boolean executeCommandAsRootAndSearchOutput(String commandContents, String outputSearchString) { 
		return executeCommandInShell_ReturnBoolean(commandContents, outputSearchString, true, this.context, this.logTag);
	}
	
	
	
	
	
	public void killProcessByName(String searchTerm, String excludeTerm) {
		Log.i(this.logTag, "Attempting to kill process associated with search term '"+searchTerm+"'.");
		String grepExclude = (excludeTerm != null) ? " grep -v "+excludeTerm+" |" : "";
		executeCommandInShell("kill $(ps |"+grepExclude+" grep "+searchTerm+" | cut -d \" \" -f 5)", null, true, this.context, this.logTag);
	}
	
	public void triggerNeedForRootAccess() {
		executeCommandInShell_ReturnBoolean("pm list features", null, true, this.context, this.logTag);
	}
	
	public boolean triggerRebootAsRoot() {
		int rebootPreDelay = 5;
		Log.v(this.logTag, "Attempting graceful reboot... then after "+rebootPreDelay+" seconds, killing RFCx processes and forcing reboot...");
		return executeCommandInShell_ReturnBoolean(
				"am start -a android.intent.action.REBOOT; "+
				"am broadcast android.intent.action.ACTION_SHUTDOWN; "+
				"sleep "+rebootPreDelay+" && kill $(ps | grep org.rfcx.guardian | cut -d \" \" -f 5) && sleep 1 && reboot; "
			, null, true, this.context, this.logTag);
	}
	
}
