package org.rfcx.guardian.utility.network;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SSHServerUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "SSHServerUtils");

	private static final String sshKeyFileNameNoExt = "ssh_rsa_guardian";
	private static final String sshConfigFileDir = "/etc/ssh";
	private static final String sshConfigFilePath = sshConfigFileDir+"/sshd_config";

	public static void serverInit(Context context) {

		String sshDirPath = context.getFilesDir().getAbsolutePath() + "/ssh";
		FileUtils.initializeDirectoryRecursively(sshDirPath, false);

		loadTmpKeyFileFromAssets(context, "pub");
		loadTmpKeyFileFromAssets(context, "pem");

		if (!FileUtils.exists(sshConfigFileDir + "/" + sshKeyFileNameNoExt + ".pub")) {

			String publicKey = StringUtils.readStringFromFile(context.getFilesDir().getAbsolutePath()+"/ssh/"+sshKeyFileNameNoExt+".pub");

			String[] execSshInit = new String[] {
					"rm " + sshConfigFilePath,
					"echo \"AuthorizedKeysFile " + sshConfigFileDir + "/" + sshKeyFileNameNoExt + ".pub\" >> " + sshConfigFilePath,
					"echo \"Subsystem sftp internal-sftp\" >> " + sshConfigFilePath,
					"rm " + sshConfigFileDir + "/" + sshKeyFileNameNoExt + ".pub",
					"echo \""+publicKey+"\" >> " + sshConfigFileDir + "/" + sshKeyFileNameNoExt + ".pub"
			};
			ShellCommands.executeCommandAsRoot(execSshInit);
			serverStop();
			serverStart();
		}
	}


	private static void loadTmpKeyFileFromAssets(Context context, String fileExt) {

		try {
			String tmpKeyFilePath = context.getFilesDir().getAbsolutePath()+"/ssh/"+sshKeyFileNameNoExt+"."+fileExt;

			if (FileUtils.exists(tmpKeyFilePath)) {
				InputStream inputStream = context.getAssets().open(sshKeyFileNameNoExt + "." + fileExt);
				OutputStream outputStream = new FileOutputStream(tmpKeyFilePath);

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				while ((len = inputStream.read(buf)) > 0) {
					outputStream.write(buf, 0, len);
				}
				inputStream.close();
				outputStream.close();
			}

		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}

	}

	public static void serverStart() {
		ShellCommands.executeCommandAsRoot("start sshd");
	}

	public static void serverStop() {
		ShellCommands.executeCommandAsRoot("stop sshd");
	}


}
