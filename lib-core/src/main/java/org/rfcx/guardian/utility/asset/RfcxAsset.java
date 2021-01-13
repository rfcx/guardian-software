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
			"video",		// 5
			"sms",			// 6
			"apk",			// 7
			"detection"		// 8
	};

	public static final String[]
		TYPES_PLURAL = new String[] {
			"audio",
			"meta",
			"screenshots",
			"logs",
			"photos",
			"videos",
			"messages",
			"apks",
			"detections"
	};

	public static final String[]
		TYPES_ABBREV = new String[] {
			"aud",
			"mta",
			"scn",
			"log",
			"pho",
			"vid",
			"sms",
			"apk",
			"det"
	};


	private static String getName(String assetType) {
		return TYPES[getInd(assetType)];
	}

	private static String getPlural(String assetType) {
		return TYPES_PLURAL[getInd(assetType)];
	}

	private static String getAbbrev(String assetType) {
		return TYPES_ABBREV[getInd(assetType)];
	}

	private static int getInd(String assetType) {
		return ArrayUtils.indexOfStringInStringArray(TYPES, assetType.toLowerCase(Locale.US));
	}

	public static boolean doesJsonHaveIndex(JSONObject jsonObj, String assetType) {
		return 	jsonObj.has(getName(assetType))
			||	jsonObj.has(getPlural(assetType))
			||	jsonObj.has(getAbbrev(assetType));
	}

}
