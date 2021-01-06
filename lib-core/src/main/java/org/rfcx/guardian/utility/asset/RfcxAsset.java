package org.rfcx.guardian.utility.asset;

import org.json.JSONObject;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import java.util.Locale;

public class RfcxAsset {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "RfcxAsset");


	public static final String[]
		TYPES = new String[] {
			"audio",		// 0
			"meta",			// 1
			"screenshot",	// 2
			"log",			// 3
			"photo",		// 4
			"video"			// 5
	};

	public static final String[]
		TYPES_PLURAL = new String[] {
			"audio",
			"meta",
			"screenshots",
			"logs",
			"photos",
			"videos"
	};

	public static final String[]
		TYPES_ABBREV = new String[] {
			"aud",
			"mta",
			"scn",
			"log",
			"pho",
			"vid"
	};
	

	public static boolean doesJsonHaveIndex(JSONObject jsonObj, String assetType) {
		int typeInd = ArrayUtils.indexOfStringInStringArray(TYPES, assetType.toLowerCase(Locale.US));
		return 	jsonObj.has(assetType.toLowerCase(Locale.US))
			||	jsonObj.has(TYPES_PLURAL[typeInd])
			||	jsonObj.has(TYPES_ABBREV[typeInd]);
	}

}
