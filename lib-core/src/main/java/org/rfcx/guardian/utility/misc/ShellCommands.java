package org.rfcx.guardian.utility.misc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ShellCommands {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "ShellCommands");
	
	private static List<String> executeCommandInShell(String[] commandLines, boolean asRoot) {

		List<String> outputLines = new ArrayList<String>();

		try {

			Process shellProcess = (asRoot) ? Runtime.getRuntime().exec( new String[] { "su", "-c", "sh" } ) : Runtime.getRuntime().exec("sh");
			DataOutputStream shellOutput = new DataOutputStream(shellProcess.getOutputStream());
			BufferedReader shellReader = new BufferedReader (new InputStreamReader(shellProcess.getInputStream()));

			for (String commandLine : commandLines) {
				shellOutput.writeBytes(commandLine+"\n");
			}
			shellOutput.writeBytes("exit\n");
			shellOutput.flush();
			shellOutput.close();
			shellProcess.waitFor();

			if (shellReader != null) {
				String shellLineContent;
				while ((shellLineContent = shellReader.readLine()) != null) {
					String thisLine = shellLineContent.replaceAll("\\p{Z}", "");
					if (thisLine.length() > 0) {
						outputLines.add(thisLine);
					}
				}
			}
			
		} catch (Exception e) {

			RfcxLog.logExc(logTag, e);
	    }

		Log.i(logTag, "Exec"+(asRoot ? " (as root)" : "")+": "+ TextUtils.join("; ",commandLines));

		return outputLines;
	}

	
	private static boolean executeCommandAsRoot_ReturnBoolean(String[] commandContents, String ifOutputContainThisStringReturnTrue) {
		List<String> outputLines = executeCommandInShell(commandContents, true);
		for (String outputLine : outputLines) {
			if (outputLine.replaceAll("\\p{Z}","").equalsIgnoreCase(ifOutputContainThisStringReturnTrue.replaceAll("\\p{Z}",""))) { return true; }
		}
		return false;
	}

	private static boolean executeCommandAsRoot_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue) {
		return executeCommandAsRoot_ReturnBoolean(new String[] { commandContents }, ifOutputContainThisStringReturnTrue);
	}
	
	private static boolean executeCommand_ReturnBoolean(String[] commandContents, String ifOutputContainThisStringReturnTrue) {
		List<String> outputLines = executeCommandInShell(commandContents, false);
		for (String outputLine : outputLines) {
			if (outputLine.replaceAll("\\p{Z}","").equalsIgnoreCase(ifOutputContainThisStringReturnTrue.replaceAll("\\p{Z}",""))) { return true; }
		}
		return false;
	}

	private static boolean executeCommand_ReturnBoolean(String commandContents, String ifOutputContainThisStringReturnTrue) {
		return executeCommand_ReturnBoolean(new String[] { commandContents }, ifOutputContainThisStringReturnTrue);
	}

	public static List<String> executeCommand(String[] commandContents) {
		return executeCommandInShell(commandContents, false);
	}

	public static List<String> executeCommand(String commandContents) {
		return executeCommandInShell(new String[] { commandContents }, false);
	}

	public static List<String> executeCommandAsRoot(String[] commandContents) {
		return executeCommandInShell(commandContents, true);
	}

	public static List<String> executeCommandAsRoot(String commandContents) {
		return executeCommandInShell(new String[] { commandContents }, true);
	}

	public static void executeCommandAndIgnoreOutput(String[] commandContents) {
		executeCommandInShell(commandContents, false);
	}

	public static void executeCommandAndIgnoreOutput(String commandContents) {
		executeCommandInShell(new String[] { commandContents }, false);
	}

	public static void executeCommandAsRootAndIgnoreOutput(String[] commandContents) {
		executeCommandInShell(commandContents, true);
	}

	public static void executeCommandAsRootAndIgnoreOutput(String commandContents) {
		executeCommandInShell(new String[] { commandContents }, true);
	}

	public static boolean executeCommandAndSearchOutput(String[] commandContents, String outputSearchString) {
		return executeCommand_ReturnBoolean(commandContents, outputSearchString);
	}

	public static boolean executeCommandAndSearchOutput(String commandContents, String outputSearchString) {
		return executeCommand_ReturnBoolean(new String[] { commandContents }, outputSearchString);
	}

	public static boolean executeCommandAsRootAndSearchOutput(String[] commandContents, String outputSearchString) {
		return executeCommandAsRoot_ReturnBoolean(commandContents, outputSearchString);
	}

	public static boolean executeCommandAsRootAndSearchOutput(String commandContents, String outputSearchString) {
		return executeCommandAsRoot_ReturnBoolean(new String[] { commandContents }, outputSearchString);
	}
	

	// this does not work on the orange pi, which does not have a "cut" installation
	public static void killProcessByName(String searchTerm, String excludeTerm) {
		Log.i(logTag, "Attempting to kill process associated with search term '"+searchTerm+"'.");
		String grepExclude = (excludeTerm != null) ? " grep -v "+excludeTerm+" |" : "";
		executeCommandInShell(new String[] { "kill $(ps |"+grepExclude+" grep "+searchTerm+" | cut -d \" \" -f 5)" }, true);
	}
	
	public static void triggerNeedForRootAccess() {
		executeCommandInShell(new String[] { "pm list features" }, true);
	}

	
}
