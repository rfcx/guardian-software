package org.rfcx.guardian.guardian.api.methods.segment;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
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
	private static final int MSG_TYPE_LENGTH = 3;
	private static final int MSG_CHECKSUM_LENGTH = 40;

	// Segment Group Header Format:
	// {GROUP_ID}{SEGMENT_ID}{GUARDIAN_GUID}{MSG_TYPE}{SEGMENT_COUNT}{MSG_CHECKSUM}{FIRST_SEGMENT}
	// {ABCd}{000}{298c2kwyfg55}{cmd}{0fe272f9da329ab5e64c08b223dc6f044a5b5e79be0}{1234}

	public static final String[] SEGMENT_PROTOCOLS = new String[] { "sms", "iridium" };

	private static final Map<String, Integer> SEGMENT_PAYLOAD_MAX_LENGTH_BY_PROTOCOL =
		Collections.unmodifiableMap( new HashMap<String, Integer>() {{
			put("sms", 160 );
			put("iridium", 100 );
		}}
	);

	public static int getSegmentPayloadMaxLength(String protocol) {
		if (SEGMENT_PAYLOAD_MAX_LENGTH_BY_PROTOCOL.containsKey(protocol.toLowerCase())) {
			return (int) (SEGMENT_PAYLOAD_MAX_LENGTH_BY_PROTOCOL.get(protocol.toLowerCase())- GROUP_ID_LENGTH - SEGMENT_ID_LENGTH);
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

	public void receiveSegment(String segmentPayload, String originProtocol) {

		try {
			String groupId = segmentPayload.substring(0, GROUP_ID_LENGTH);
			int segmentId = segmentId_paddedHexToDec(segmentPayload.substring(GROUP_ID_LENGTH, GROUP_ID_LENGTH + SEGMENT_ID_LENGTH));
			String segmentBody = segmentPayload.substring(GROUP_ID_LENGTH + SEGMENT_ID_LENGTH);

	/*		if (	!isSegmentAnInitialGroupHeader(segmentId)
				&&	canSegmentBeAssociatedWithValidGroup(groupId, segmentId)
				&& 	!isSegmentAlreadyReceived(groupId, segmentId)
				) {

				saveSegment(groupId, segmentId, segmentBody);

			} else */if ( isSegmentAnInitialGroupHeader(segmentId) ) {

				parseSegmentInitialGroupHeader(groupId, segmentBody, originProtocol);

			} else if ( canSegmentBeAssociatedWithValidGroup(groupId, segmentId) && !isSegmentAlreadyReceived(groupId, segmentId) ) {

				saveSegment(groupId, segmentId, segmentBody);

			} else {

				Log.e(logTag, "Received segment was not saved: " + segmentPayload +" ("+segmentPayload.length()+" chars)");
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private void parseSegmentInitialGroupHeader(String groupId, String segmentBody, String originProtocol) {

		String guardianGuid = segmentBody.substring(0, RfcxGuardianIdentity.GUID_LENGTH);
		String msgType = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + MSG_TYPE_LENGTH);
		int segmentCount = segmentId_paddedHexToDec(segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + MSG_TYPE_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + MSG_TYPE_LENGTH + SEGMENT_ID_LENGTH));
		String msgChecksum = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + MSG_TYPE_LENGTH + SEGMENT_ID_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + MSG_TYPE_LENGTH + SEGMENT_ID_LENGTH + MSG_CHECKSUM_LENGTH);
		String segmentBodyZero = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + MSG_TYPE_LENGTH + SEGMENT_ID_LENGTH + MSG_CHECKSUM_LENGTH);

		if (!guardianGuid.equalsIgnoreCase(app.rfcxGuardianIdentity.getGuid())) {
			Log.e(logTag, "Specified Guardian ID in Segment Group Header does not match this guardian: " + guardianGuid);

		} else if (!msgType.equalsIgnoreCase("cmd")) {
			Log.e(logTag, "Specified Message Category in Segment Group Header is not valid: " + msgType);

		} else if (!originProtocol.equalsIgnoreCase("sms")) {
			Log.e(logTag, "Specified Message origin protocol in Segment Group Header is not valid: " + originProtocol);

		} else if (app.apiSegmentDb.dbGroups.getSingleRowById(groupId)[0] != null) {
			Log.e(logTag, "Segment Group has already been initialized: " +groupId+", "+segmentCount+" segments");
			if (!isSegmentAlreadyReceived(groupId, 0)) { saveSegment(groupId, 0, segmentBodyZero); }

		} else {
			app.apiSegmentDb.dbGroups.insert(groupId, segmentCount, msgChecksum, msgType.toLowerCase(), originProtocol.toLowerCase());
			Log.i(logTag, "Segment Group initialized: "+groupId+", "+segmentCount+" segments");
			saveSegment(groupId, 0, segmentBodyZero);

		}
	}

	private void saveSegment(String groupId, int segmentId, String segmentBody) {

		app.apiSegmentDb.dbReceived.insert(groupId, segmentId, segmentBody);
		Log.i(logTag, "Received and saved Segment " + segmentId + " of Group " + groupId +" ("+segmentBody.length()+" chars)");
	}

	private boolean canSegmentBeAssociatedWithValidGroup(String groupId, int segmentId) {
		String[] grpCheck = app.apiSegmentDb.dbGroups.getSingleRowById(groupId);
		if ((grpCheck[0] == null)) {
			Log.e(logTag, "No Valid Segment Group was found with ID '"+groupId+"'");
			return false;
		} else if ((((int) Integer.parseInt(grpCheck[2])) < segmentId)) {
			Log.e(logTag, "Received Segment "+segmentId+" is beyond the initialized size ("+grpCheck[2]+") for Group with ID '"+groupId+"'");
			return false;
		} else {
			return true;
		}
	}

	private boolean isSegmentAnInitialGroupHeader(int segmentId) {
		return (segmentId == 0);
	}

	private boolean isSegmentAlreadyReceived(String groupId, int segmentId) {
		if (app.apiSegmentDb.dbReceived.getSegmentByGroupAndId(groupId, segmentId)[0] != null) {
			Log.e(logTag, "Segment "+segmentId+" within Group "+groupId+" has already been received.");
			return true;
		}
		return false;
	}






}
