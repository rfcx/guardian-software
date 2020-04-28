package org.rfcx.guardian.utility.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxGarbageCollection;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ShellCommands {
	
	public ShellCommands(Context context) {
		this.context = context;
	}
	private Context context = null;

	private static final String logTag = RfcxLog.generateLogTag("Utils", "ShellCommands");
	
	private static List<String> executeCommandInShell(String[] commandLines, boolean asRoot, Context context) {

		List<String> outputLines = new ArrayList<String>(); 
		
		File rootScriptObj = null;
		File tmpScriptObj = null;

		// In case the file directory for the application is not accessible to the parts of the system that run the scripts.
		String accessibleDir = Environment.getExternalStorageDirectory().toString()+"/rfcx/scr";
		(new File(accessibleDir)).mkdirs(); FileUtils.chmod(accessibleDir,  "rw", "rw");
		tmpScriptObj = new File(accessibleDir+"/script-root-"+System.currentTimeMillis()+".sh");
		
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

			    	StringBuilder commandContent = (new StringBuilder()).append("#!/system/bin/sh\n");
			    	for (String commandLine : commandLines) {
						commandContent/*.append("su -c ")*/.append(commandLine).append(";\n");
					}

					BufferedWriter rootFileWriter = new BufferedWriter(new FileWriter(rootScriptPath));
					rootFileWriter.write(commandContent.toString());
			        rootFileWriter.close();

			        FileUtils.chmod(rootScriptObj, "rwx", "rx");
					FileUtils.copy(rootScriptObj, tmpScriptObj);
					FileUtils.chmod(tmpScriptObj, "rwx", "rx");

				    if (rootScriptObj.exists()) {

////						shellProcess = Runtime.getRuntime().exec("/system/xbin/su");
//						shellProcess = Runtime.getRuntime().exec("/system/bin/sh");
//						DataOutputStream shellOutput = new DataOutputStream(shellProcess.getOutputStream());
//						shellReader = new BufferedReader (new InputStreamReader(shellProcess.getInputStream()));
//
//                        for (String commandLine : commandLines) {
//                        	Log.i(logTag, "Line: "+commandLine);
//                            shellOutput.writeBytes("su -c "+commandLine+";\n");
//                            shellOutput.flush();
//                        }
////						shellOutput.writeBytes(tmpScriptObj.getAbsolutePath()+";\n");
////						shellOutput.flush();
//
//						shellOutput.writeBytes("exit;\n");
//						shellOutput.flush();
//
//						shellProcess.waitFor();
//						shellOutput.close();

//						for (String commandLine : commandLines) {
//							shellProcess = Runtime.getRuntime().exec("su");
//							DataOutputStream dos = new DataOutputStream(shellProcess.getOutputStream());
//							dos.writeBytes(commandLine + "\n");
//							dos.writeBytes("exit\n");
//							dos.flush();
//							dos.close();
//							shellProcess.waitFor();
//							Log.i(logTag, "Line: " + commandLine);
//						}

						shellProcess = Runtime.getRuntime().exec(new String[] { "su", "0", tmpScriptObj.getAbsolutePath() });
						shellProcess.waitFor();


						//				try {
//					Process proc = Runtime.getRuntime()
//							.exec(new String[]{ "su", "0", "reboot", "-p" });
//					proc.waitFor();
//				} catch (Exception ex) {
//					ex.printStackTrace();
//				}


				    }
				}
			
			} else {
				
				shellProcess = Runtime.getRuntime().exec("sh");
				DataOutputStream shellOutput = new DataOutputStream(shellProcess.getOutputStream());
				shellReader = new BufferedReader (new InputStreamReader(shellProcess.getInputStream()));

				for (String commandLine : commandLines) {
					Log.i(logTag, "Line: "+commandLine);
					shellOutput.writeBytes(commandLine+"\n");
				}
				shellOutput.writeBytes("exit\n");
				shellOutput.flush();
				shellOutput.close();
				shellProcess.waitFor();
			}

			
//			String shellLineContent;
//			while ((shellLineContent = shellReader.readLine()) != null) {
//				String thisLine = shellLineContent.trim();
//				if (thisLine.length() > 0) {
//					outputLines.add(thisLine);
//				}
//			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
	    } finally {
	    		if ((rootScriptObj != null) && rootScriptObj.exists()) { rootScriptObj.delete(); }
				if ((tmpScriptObj != null) && tmpScriptObj.exists()) { tmpScriptObj.delete(); }
	    }

		Log.e(logTag, "Exec"+(asRoot ? " (as root)" : "")+": "+ TextUtils.join(" | ",commandLines));

		return outputLines;
	}

	
	private static boolean executeCommandAsRoot_ReturnBoolean(String[] commandContents, String ifOutputContainThisStringReturnTrue, Context context) {
		List<String> outputLines = executeCommandInShell(commandContents, true, context);
		for (String outputLine : outputLines) {
			if (outputLine.trim().equalsIgnoreCase(ifOutputContainThisStringReturnTrue.trim())) { return true; }
		}
		return false;
	}

	private static boolean executeCommandAsRoot_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue, Context context) {
		return executeCommandAsRoot_ReturnBoolean(new String[] { commandContents }, ifOutputContainThisStringReturnTrue, context);
	}
	
	private static boolean executeCommand_ReturnBoolean(String[] commandContents, String ifOutputContainThisStringReturnTrue) {
		List<String> outputLines = executeCommandInShell(commandContents, false, null);
		for (String outputLine : outputLines) {
			if (outputLine.trim().equalsIgnoreCase(ifOutputContainThisStringReturnTrue.trim())) { return true; }
		}
		return false;
	}

	private static boolean executeCommand_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue) {
		return executeCommand_ReturnBoolean(new String[] { commandContents }, ifOutputContainThisStringReturnTrue);
	}

	public static List<String> executeCommand(String[] commandContents) {
		return executeCommandInShell(commandContents, false, null);
	}

	public static List<String> executeCommand(String commandContents) {
		return executeCommandInShell(new String[] { commandContents }, false, null);
	}

	public static List<String> executeCommandAsRoot(String[] commandContents, Context context) {
		return executeCommandInShell(commandContents, true, context);
	}

	public static List<String> executeCommandAsRoot(String commandContents, Context context) {
		return executeCommandInShell(new String[] { commandContents }, true, context);
	}

	public static void executeCommandAndIgnoreOutput(String[] commandContents) {
		executeCommandInShell(commandContents, false, null);
	}

	public static void executeCommandAndIgnoreOutput(String commandContents) {
		executeCommandInShell(new String[] { commandContents }, false, null);
	}

	public static void executeCommandAsRootAndIgnoreOutput(String[] commandContents, Context context) {
		executeCommandInShell(commandContents, true, context);
	}

	public static void executeCommandAsRootAndIgnoreOutput(String commandContents, Context context) {
		executeCommandInShell(new String[] { commandContents }, true, context);
	}

	public static boolean executeCommandAndSearchOutput(String[] commandContents, String outputSearchString) {
		return executeCommand_ReturnBoolean(commandContents, outputSearchString);
	}

	public static boolean executeCommandAndSearchOutput(String commandContents, String outputSearchString) {
		return executeCommand_ReturnBoolean(new String[] { commandContents }, outputSearchString);
	}

	public static boolean executeCommandAsRootAndSearchOutput(String[] commandContents, String outputSearchString, Context context) {
		return executeCommandAsRoot_ReturnBoolean(commandContents, outputSearchString, context);
	}

	public static boolean executeCommandAsRootAndSearchOutput(String commandContents, String outputSearchString, Context context) {
		return executeCommandAsRoot_ReturnBoolean(new String[] { commandContents }, outputSearchString, context);
	}
	

	// this does not work on the orange pi, which does not have a "cut" installation
	public static void killProcessByName(String searchTerm, String excludeTerm, Context context) {
		Log.i(logTag, "Attempting to kill process associated with search term '"+searchTerm+"'.");
		String grepExclude = (excludeTerm != null) ? " grep -v "+excludeTerm+" |" : "";
		executeCommandInShell(new String[] { "kill $(ps |"+grepExclude+" grep "+searchTerm+" | cut -d \" \" -f 5)" }, true, context);
	}
	
	public static void triggerNeedForRootAccess(Context context) {
		executeCommandInShell(new String[] { "pm list features" }, true, context);
	}

	
}
