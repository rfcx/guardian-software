package org.rfcx.guardian.guardian.api.methods.segment;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiSegmentUtils {

	public ApiSegmentUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiSegmentUtils");

	private RfcxGuardian app;

	public static final int GROUP_ID_LENGTH = 4;
	private static final int SEGMENT_ID_LENGTH = 3;

	private static final Map<String, Integer> SEGMENT_PAYLOAD_MAX_LENGTHS =
		Collections.unmodifiableMap( new HashMap<String, Integer>() {{
			put("sms", 160 );
			put("iridium", 100 );
		}}
	);

	public static int getSegmentPayloadMaxLength(String protocol) {
		if (SEGMENT_PAYLOAD_MAX_LENGTHS.containsKey(protocol.toLowerCase())) {
			return (int) (SEGMENT_PAYLOAD_MAX_LENGTHS.get(protocol.toLowerCase())- GROUP_ID_LENGTH - SEGMENT_ID_LENGTH);
		} else {
			return 100;
		}
	}

	public static String generateSegmentGroupId() {
		return StringUtils.randomAlphanumericString(GROUP_ID_LENGTH, true);
	}

	private static int segmentId_paddedHexToDec(String segmentIdAsPaddedHex) {
		return Integer.valueOf(String.valueOf(segmentIdAsPaddedHex), 16);
	}

	public static String segmentId_decToPaddedHex(int segmentIdAsDec) {
		String asHex = Integer.toHexString(segmentIdAsDec);
		while (asHex.length() < SEGMENT_ID_LENGTH) { asHex = "0" + asHex; }
		return asHex;
	}

	public void receiveSegment(String segmentPayload) {

		String groupId = segmentPayload.substring(0, GROUP_ID_LENGTH);
		int segmentId = segmentId_paddedHexToDec(segmentPayload.substring(GROUP_ID_LENGTH, GROUP_ID_LENGTH + SEGMENT_ID_LENGTH));
		String segmentBody = segmentPayload.substring(GROUP_ID_LENGTH + SEGMENT_ID_LENGTH);

		if ( canSegmentBeAssociatedWithValidGroup(groupId, segmentId) ) {
			if (!isSegmentAlreadyReceived(groupId, segmentId)) {
				app.apiSegmentDb.dbReceived.insert(groupId, segmentId, segmentBody);
				Log.i(logTag, "Received: Segment " + segmentId + " of Group " + groupId +" ("+segmentBody.length()+" chars)");
			}

		} else {

			app.apiSegmentDb.dbGroups.insert( groupId, 4000, "checksum", "sms");
		}


	}

	private boolean canSegmentBeAssociatedWithValidGroup(String groupId, int segmentId) {
		String[] grpCheck = app.apiSegmentDb.dbGroups.getSingleRowById(groupId);
		boolean isGroupValid = (grpCheck[0] != null);
		boolean isSegmentWithinGroupBounds = (((int) Integer.parseInt(grpCheck[2])) >= segmentId);
		if (!isGroupValid) {
			Log.e(logTag, "No Valid Segment Group was found with ID '"+groupId+"'");
		} else if (!isSegmentWithinGroupBounds) {
			Log.e(logTag, "Segment "+segmentId+" is beyond the stated size ("+grpCheck[2]+") for Group with ID '"+groupId+"'");
		}
		return isGroupValid && isSegmentWithinGroupBounds;
	}

	private boolean isSegmentAlreadyReceived(String groupId, int segmentId) {
		String[] segCheck = app.apiSegmentDb.dbReceived.getSegmentByGroupAndId(groupId, segmentId);
		if (segCheck[0] != null) {
			Log.e(logTag, "Segment "+segmentId+" within Group "+groupId+" has already been received.");
			return true;
		} else {
			return false;
		}
	}






}
