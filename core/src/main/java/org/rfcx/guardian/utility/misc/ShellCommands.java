package org.rfcx.guardian.utility.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxGarbageCollection;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ShellCommands {
	
	public ShellCommands(Context context) {
		this.context = context;
	}
	private Context context = null;

	private static final String logTag = RfcxLog.generateLogTag("Utils", ShellCommands.class);
	
	
	private static List<String> executeCommandInShell(String commandContents, boolean asRoot, Context context) {

		List<String> outputLines = new ArrayList<String>(); 
		
		File rootScriptObj = null;
		
		try {

			Process shellProcess = null;
			BufferedReader shellReader = null;
			
			if (asRoot) {
				
				if (context == null) {
				
					Log.e(logTag,"need 'context' to run as root..."); 
					
				} else {
				
					String rootScriptPath = context.getFilesDir().toString()+"/scr/script-root.sh";
				    (new File(rootScriptPath.substring(0,rootScriptPath.lastIndexOf("/")))).mkdirs();
				    rootScriptObj = new File(rootScriptPath);
				    
			    		long deleteOldScriptFileIfOlderThan = 3000;
			    		long loopDelay = 300;
			    		int maxLoops = ( (int) Math.ceil(deleteOldScriptFileIfOlderThan / loopDelay) ) + 1;
			    		
			    		try {
			    			int delayLoop = 0;
			    			while (rootScriptObj.exists() && (delayLoop < maxLoops)) {
				    			if (delayLoop == 0) { Log.e(logTag,"Another script is running... delaying execution for up to "+Math.round(deleteOldScriptFileIfOlderThan/1000)+" seconds."); }
			    				delayLoop++;
			    				long msSinceModified = FileUtils.millisecondsSinceLastModified(rootScriptObj);
				    			if (msSinceModified > deleteOldScriptFileIfOlderThan) { 
				    				Log.e(logTag,"Deleting old script file... ("+Math.round(msSinceModified/1000)+" seconds old)");
				    				rootScriptObj.delete();
				    			} else {
				    				 Thread.sleep(loopDelay);
				    			}
			    			}
			    		} catch (Exception e) {
			    			RfcxLog.logExc(logTag, e);
			    		}
				    
		    			BufferedWriter rootFileWriter = new BufferedWriter(new FileWriter(rootScriptPath));
			        rootFileWriter.write((new StringBuilder()).append("#!/system/bin/sh\n").append(commandContents).append("\n").toString());
			        rootFileWriter.close();
			        FileUtils.chmod(rootScriptObj, "rwx", "rx");
				    
				    if (rootScriptObj.exists()) {
				    	shellProcess = Runtime.getRuntime().exec( new String[] { "su", "-c", rootScriptPath } );
						shellProcess.waitFor();
						shellReader = new BufferedReader (new InputStreamReader(shellProcess.getInputStream()));
			    	    }
				}
			
			} else {
				
				shellProcess = Runtime.getRuntime().exec("sh");	
				DataOutputStream shellOutput = new DataOutputStream(shellProcess.getOutputStream());
				shellReader = new BufferedReader (new InputStreamReader(shellProcess.getInputStream()));
				
	    			shellOutput.writeBytes(commandContents+";\n");
		    		shellOutput.flush();

				shellOutput.writeBytes("exit;\n");
				shellOutput.flush();
			}
			
			
			String shellLineContent;
			while ((shellLineContent = shellReader.readLine()) != null) { 
				String thisLine = shellLineContent.trim();
				if (thisLine.length() > 0) {
					outputLines.add(thisLine);
				}
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
	    } finally {
	    		if ((rootScriptObj != null) && rootScriptObj.exists()) { rootScriptObj.delete(); }
	    }
		
		
		return outputLines;
	}

	
	private static boolean executeCommandAsRoot_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue, Context context) {
		List<String> outputLines = executeCommandInShell(commandContents, true, context);
		for (String outputLine : outputLines) {
			if (outputLine.trim().equalsIgnoreCase(ifOutputContainThisStringReturnTrue.trim())) { return true; }
		}
		return false;
	}
	
	private static boolean executeCommand_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue) {
		List<String> outputLines = executeCommandInShell(commandContents, false, null);
		for (String outputLine : outputLines) {
			if (outputLine.trim().equalsIgnoreCase(ifOutputContainThisStringReturnTrue.trim())) { return true; }
		}
		return false;
	}
	
	public static List<String> executeCommand(String commandContents) {
		return executeCommandInShell(commandContents, false, null);
	}

	public static List<String> executeCommandAsRoot(String commandContents, Context context) { 
		return executeCommandInShell(commandContents, true, context);
	}

	public static void executeCommandAndIgnoreOutput(String commandContents) { 
		executeCommandInShell(commandContents, false, null);
	}

	public static void executeCommandAsRootAndIgnoreOutput(String commandContents, Context context) { 
		executeCommandInShell(commandContents, true, context);
	}
	
	public static boolean executeCommandAndSearchOutput(String commandContents, String outputSearchString) { 
		return executeCommand_ReturnBoolean(commandContents, outputSearchString);
	}

	public static boolean executeCommandAsRootAndSearchOutput(String commandContents, String outputSearchString, Context context) { 
		return executeCommandAsRoot_ReturnBoolean(commandContents, outputSearchString, context);
	}
	
	
	
	
	
	public static void killProcessByName(String searchTerm, String excludeTerm, Context context) {
		Log.i(logTag, "Attempting to kill process associated with search term '"+searchTerm+"'.");
		String grepExclude = (excludeTerm != null) ? " grep -v "+excludeTerm+" |" : "";
		executeCommandAsRootAndIgnoreOutput("kill $(ps |"+grepExclude+" grep "+searchTerm+" | cut -d \" \" -f 5)", context);
	}
	
	public static void triggerNeedForRootAccess(Context context) {
		executeCommandInShell("pm list features", true, context);
	}
	
//	public static boolean triggerRebootAsRoot(Context context) {
//
//		RfcxGarbageCollection.runAndroidGarbageCollection();
//
//		int rebootPreDelay = 3;
//
//		Log.v(logTag, "Attempting graceful reboot... then after "+rebootPreDelay+" seconds, killing RFCx processes and forcing reboot...");
//
//		executeCommandAsRoot(""
//				+"am start -a android.intent.action.REBOOT; "
//				+"am broadcast android.intent.action.ACTION_SHUTDOWN; "
//				+"sleep "+rebootPreDelay
//					+" && kill $(ps | grep org.rfcx.org.rfcx.guardian.guardian | cut -d \" \" -f 5)"
//					+" && umount -vl "+Environment.getExternalStorageDirectory().toString()
//					+" && reboot; "
//				+"sleep "+rebootPreDelay+" && reboot; "
//			, context);
//		return true;
//	}
	
	
	
	
	
	
	
//	private static List<String> executeCommandInShell(String commandContents, String ifOutputContainThisStringReturnTrue, boolean asRoot, Context context, String logTag) {
//
//	    List<String> standardOutStringLines = new ArrayList<String>();
//	    standardOutStringLines.add("false");
//	    
//		String filePath = context.getFilesDir().toString()+"/scr/script"+(asRoot ? "-root" : "")+".sh";
//	    (new File(filePath.substring(0,filePath.lastIndexOf("/")))).mkdirs();
//	    File fileObj = new File(filePath);
//	    	
//	    if (fileObj.exists()) { 
//	    		long waitToExecute = 500;
//	    		Log.e(logTag,"OTHER SCRIPT IS RUNNING SO WAITING "+waitToExecute+"ms BEFORE STARTING..."); 
//	//    		try { Thread.sleep(waitToExecute); } catch (InterruptedException e) { RfcxLog.logExc(logTag, e); }
//		}
//	    
//	    try {
//
//		    Process commandProcess = null;
//	    		BufferedWriter scriptFile = new BufferedWriter(new FileWriter(filePath));
//	        scriptFile.write(
//	        		"#!/system/bin/sh"
//	        		+"\n"+commandContents
//	        		+"\n");
//	        scriptFile.close();
//	        FileUtils.chmod(fileObj, "rwx", "rx");
//	        
//		    if (fileObj.exists()) {
//		    	
//		    		if (asRoot) { commandProcess = Runtime.getRuntime().exec(new String[] { "su", "-c", filePath }); }
//		    		else { commandProcess = Runtime.getRuntime().exec(new String[] { filePath }); }
//		    		
//				commandProcess.waitFor();
//					
//				BufferedReader bufferedReaderOutput = new BufferedReader (new InputStreamReader(commandProcess.getInputStream())); 
//				String eachLine_BufferedReaderOutput; while ((eachLine_BufferedReaderOutput = bufferedReaderOutput.readLine()) != null) {
//					standardOutStringLines.add(eachLine_BufferedReaderOutput.trim());
//					if (eachLine_BufferedReaderOutput.equalsIgnoreCase(ifOutputContainThisStringReturnTrue)) {
//						standardOutStringLines.set(0, "true");
//					}
//				} bufferedReaderOutput.close();
//				
////					BufferedReader bufferedReaderError = new BufferedReader (new InputStreamReader(commandProcess.getErrorStream())); 
////					String eachLine_BufferedReaderError; while ((eachLine_BufferedReaderError = bufferedReaderError.readLine()) != null) {
////						standardOutStringLines.add(eachLine_BufferedReaderError.trim());
////					} bufferedReaderError.close();
//				
//				if (ifOutputContainThisStringReturnTrue == null) { standardOutStringLines.set(0, (commandProcess.exitValue() == 1) ? "true" : "false"); }
//				commandProcess.destroy();
//
//		    } else {
//		    		Log.e(logTag,"Shell script could not be located for execution");
//		    }
//	    } catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//	    } finally {
//	    		if (fileObj.exists()) { fileObj.delete(); }
//	    }
//	    
//		    
//	    return standardOutStringLines;
//	}
	
	
	
}
