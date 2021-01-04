package org.rfcx.guardian.guardian.api.methods.segment;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
	private static final int MSG_CHECKSUM_SNIPPET_LENGTH = 20;

	// Segment Group Header Format:
	// {GROUP_ID}{SEGMENT_ID}{GUARDIAN_GUID}{MSG_TYPE}{MSG_CHECKSUM}{SEGMENT_COUNT}{FIRST_SEGMENT}
	// {ABCd}{000}{298c2kwyfg55}{cmd}{0fe272f9da329ab5e64c08b223dc6f044a5b5e79be0}{fff}{1234}

	public static final String[] JSON_MSG_TYPES = new String[] { "cmd", "png", "chk" };
	public static final String[] BINARY_MSG_TYPES = new String[] {  };

	public static final String[] SEGMENT_PROTOCOLS = new String[] { "sms", "sbd" };

	private static final Map<String, Integer> SEGMENT_PAYLOAD_MAX_SEND_LENGTH_BY_PROTOCOL =
		Collections.unmodifiableMap( new HashMap<String, Integer>() {{
			put("sms", 160 );
			put("sbd", 340 );
		}}
	);

//	public static int getSegmentPayloadMaxLength(String protocol) {
//		if (SEGMENT_PAYLOAD_MAX_SEND_LENGTH_BY_PROTOCOL.containsKey(protocol.toLowerCase())) {
//			return (SEGMENT_PAYLOAD_MAX_SEND_LENGTH_BY_PROTOCOL.get(protocol.toLowerCase()) - GROUP_ID_LENGTH - SEGMENT_ID_LENGTH);
//		} else {
//			return 100;
//		}
//	}

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

			if ( isSegmentAnInitialGroupHeader(segmentId) ) {
				parseSegmentInitialGroupHeader(groupId, segmentBody, originProtocol);

			} else if ( /*canSegmentBeAssociatedWithValidGroup(groupId, segmentId) &&*/ !isSegmentAlreadyReceived(groupId, segmentId) ) {
				saveReceivedSegment(groupId, segmentId, segmentBody);

			} else {
				Log.e(logTag, "Received segment was not saved: " + segmentPayload +" ("+segmentPayload.length()+" chars)");
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private void parseSegmentInitialGroupHeader(String groupId, String segmentBody, String originProtocol) {

		String guardianGuid = segmentBody.substring(0, RfcxGuardianIdentity.GUID_LENGTH);
		String guardianPinCode = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH);
		String msgType = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH + MSG_TYPE_LENGTH);
		String msgChecksumSnippet = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH + MSG_TYPE_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH + MSG_TYPE_LENGTH + MSG_CHECKSUM_SNIPPET_LENGTH);
		int segmentCount = segmentId_paddedHexToDec(segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH + MSG_TYPE_LENGTH + MSG_CHECKSUM_SNIPPET_LENGTH, RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH + MSG_TYPE_LENGTH + MSG_CHECKSUM_SNIPPET_LENGTH + SEGMENT_ID_LENGTH));
		String segmentBodyZero = segmentBody.substring(RfcxGuardianIdentity.GUID_LENGTH + RfcxGuardianIdentity.PINCODE_LENGTH + MSG_TYPE_LENGTH + MSG_CHECKSUM_SNIPPET_LENGTH + SEGMENT_ID_LENGTH);

		if (!guardianGuid.equalsIgnoreCase(app.rfcxGuardianIdentity.getGuid())) {
			Log.e(logTag, "Specified Guardian ID ("+guardianGuid+") in Segment Group Header does not match this guardian: " + app.rfcxGuardianIdentity.getGuid());

		} else if (!guardianPinCode.equalsIgnoreCase(app.rfcxGuardianIdentity.getPinCode())) {
			Log.e(logTag, "Specified PIN Code ("+guardianPinCode+") in Segment Group Header does not match this guardian: " + app.rfcxGuardianIdentity.getPinCode());

		} else if (!ArrayUtils.doesStringArrayContainString(JSON_MSG_TYPES, msgType)) {
			Log.e(logTag, "Specified Message Category in Segment Group Header is not valid: " + msgType);

		} else if (!ArrayUtils.doesStringArrayContainString(SEGMENT_PROTOCOLS, originProtocol)) {
			Log.e(logTag, "Specified Message origin protocol in Segment Group Header is not valid: " + originProtocol);

		} else if (app.apiSegmentDb.dbGroups.getSingleRowById(groupId)[0] != null) {
			Log.e(logTag, "Segment Group has already been initialized: " +groupId+", "+segmentCount+" segments");
			if (!isSegmentAlreadyReceived(groupId, 0)) { saveReceivedSegment(groupId, 0, segmentBodyZero); }

		} else {
			app.apiSegmentDb.dbGroups.insert(groupId, segmentCount, msgChecksumSnippet, msgType.toLowerCase(), originProtocol.toLowerCase());
			Log.i(logTag, "Segment Group initialized: "+groupId+", "+segmentCount+" segments");
			saveReceivedSegment(groupId, 0, segmentBodyZero);

		}
	}

	private void saveReceivedSegment(String groupId, int segmentId, String segmentBody) {
		app.apiSegmentDb.dbReceived.insert(groupId, segmentId, segmentBody);
		Log.i(logTag, "Received and saved Segment " + (segmentId+1) + " of Group " + groupId +" ("+segmentBody.length()+" chars)");
		if (isSegmentGroupFullyReceived(groupId)) { assembleReceivedSegments(groupId); }

	}

	private boolean canSegmentBeAssociatedWithValidGroup(String groupId, int segmentId) {
		String[] grpCheck = app.apiSegmentDb.dbGroups.getSingleRowById(groupId);
		if ((grpCheck[0] == null)) {
			Log.e(logTag, "No Valid Segment Group was found with ID '"+groupId+"'");
			return false;
		} else if (Integer.parseInt(grpCheck[2]) <= segmentId) {
			Log.e(logTag, "Received Segment "+(segmentId+1)+" is beyond the initialized size ("+grpCheck[2]+") for Group with ID '"+groupId+"'");
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
			if (isSegmentGroupFullyReceived(groupId)) { assembleReceivedSegments(groupId); }
			return true;
		}
		return false;
	}

	private boolean isSegmentGroupFullyReceived(String groupId) {
		int segmentCount = app.apiSegmentDb.dbReceived.getCountByGroupId(groupId);
		String[] segmentGroupRow = app.apiSegmentDb.dbGroups.getSingleRowById(groupId);
		if ( (segmentGroupRow[0] != null) && (segmentCount == Integer.parseInt(segmentGroupRow[2])) ) {
			Log.i(logTag, "All Segments ("+segmentCount+" in total) for Group " + groupId +" have been received.");
			return true;
		}
		return false;
	}

	private void assembleReceivedSegments(String groupId) {

		try {

			String[] grpInfo = app.apiSegmentDb.dbGroups.getSingleRowById(groupId);

			if (grpInfo[0] != null) {

				String msgChecksum = grpInfo[3];
				String msgProtocol = grpInfo[4];
				String msgType = grpInfo[5];

				StringBuilder concatSegments = new StringBuilder();
				for (String[] segmentRow : app.apiSegmentDb.dbReceived.getAllSegmentsForGroupOrderedBySegmentId(groupId)) {
					concatSegments.append(segmentRow[3]);
				}
				String concatMsg = concatSegments.toString();

				if (ArrayUtils.doesStringArrayContainString(JSON_MSG_TYPES, msgType)) {

					String msgJson = StringUtils.gZippedBase64ToUnGZippedString(concatMsg);

					if (msgChecksum.equalsIgnoreCase(StringUtils.getSha1HashOfString(msgJson).substring(0, MSG_CHECKSUM_SNIPPET_LENGTH))) {

						if (msgType.equalsIgnoreCase("cmd")) {
							app.apiCommandUtils.processApiCommandJson(msgJson);
							deleteSegmentsById(groupId);

						} else {
							Log.e(logTag, msgProtocol.toUpperCase() + ": " + msgJson);
							deleteSegmentsById(groupId);
						}

					} else {
						Log.e(logTag, "Assembled Segments for Group "+groupId+" failed checksum verification.");
					}
				} else if (ArrayUtils.doesStringArrayContainString(BINARY_MSG_TYPES, msgType)) {


				}

			} else {
				Log.e(logTag, "No Valid Segment Group was found with ID '"+groupId+"'");
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}




	public String constructSegmentsGroupForQueue(String msgType, String apiProtocol, String msgJson, String attachmentFilePath) {

		String groupId = generateSegmentGroupId();
		int segMaxLength = SEGMENT_PAYLOAD_MAX_SEND_LENGTH_BY_PROTOCOL.get(apiProtocol);
		int segBodyMaxLength = segMaxLength - GROUP_ID_LENGTH - SEGMENT_ID_LENGTH;
		List<String> segments = new ArrayList<String>();

		if (ArrayUtils.doesStringArrayContainString(JSON_MSG_TYPES, msgType)) {

			try {

				String msgChecksumSnippet = StringUtils.getSha1HashOfString(msgJson).substring(0, MSG_CHECKSUM_SNIPPET_LENGTH);
				int msgOriginalLength = msgJson.length();
				String msgPayloadFull = StringUtils.stringToGZippedBase64(msgJson);
				int msgPayloadFullLength = msgPayloadFull.length();

				String initSegHeader = groupId + segmentId_decToPaddedHex(0) + app.rfcxGuardianIdentity.getGuid() + app.rfcxGuardianIdentity.getPinCode() + msgType + msgChecksumSnippet;
				int initSegBodyLength = segMaxLength - initSegHeader.length() - SEGMENT_ID_LENGTH;
				if (initSegBodyLength > msgPayloadFullLength) { initSegBodyLength = msgPayloadFullLength; }
				String initSegBody = msgPayloadFull.substring(0, initSegBodyLength);

				double segCount = 1 + (((double) msgPayloadFullLength - initSegBodyLength) / segBodyMaxLength);
				int segCountCeil = (int) Math.ceil(segCount);
				int fullLengthOfSegments = msgPayloadFullLength + ((segCountCeil-1)*(GROUP_ID_LENGTH+SEGMENT_ID_LENGTH)) + initSegHeader.length() + SEGMENT_ID_LENGTH;

				Log.d(logTag, "Segment Group Created: "+groupId+", "+segCountCeil+" segment(s), "+fullLengthOfSegments+" encoded characters to be transferred, "+msgOriginalLength+" original length");

				if (segCount > 1) {
					for (int i = 0; i < Math.ceil(segCount-1); i++) {
						int segBodyOffset = (initSegBodyLength + (i * segBodyMaxLength));
						int segBodyLength = ((segBodyOffset + segBodyMaxLength) <= msgPayloadFullLength) ? segBodyMaxLength : (msgPayloadFullLength - segBodyOffset);
						segments.add(groupId + segmentId_decToPaddedHex(i + 1) + msgPayloadFull.substring(segBodyOffset, segBodyOffset + segBodyLength));
					}
				}

				app.apiSegmentDb.dbGroups.insert(groupId, segCountCeil, msgChecksumSnippet, msgType.toLowerCase(), apiProtocol.toLowerCase());

				String initSegPayload = initSegHeader + segmentId_decToPaddedHex(segCountCeil) + initSegBody;
				app.apiSegmentDb.dbQueued.insert(groupId, 0, initSegPayload);
				//Log.d(logTag,  String.format(Locale.US, "%04d", 1) + ") " + initSegPayload);

				for (int i = 0; i < segments.size(); i++) {
					app.apiSegmentDb.dbQueued.insert(groupId, i+1, segments.get(i) );
				//	Log.d(logTag,String.format(Locale.US, "%04d", (i+2)) + ") " + segments.get(i));
				}

				return groupId;

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return null;
	}


	public int deleteSegmentsById(String segmentAssetId) {

		int deleteCount = 0;

		if (segmentAssetId.contains("-")) {
			String groupId = segmentAssetId.split("-")[0];
			int segmentId = segmentId_paddedHexToDec(segmentAssetId.split("-")[1]);
			deleteCount += app.apiSegmentDb.dbReceived.deleteSegmentsByGroupAndId(groupId, segmentId);
			deleteCount += app.apiSegmentDb.dbQueued.deleteSegmentsByGroupAndId(groupId, segmentId);
			deleteCount += deleteOrReQueueCompletedSegments(groupId);
		} else {
			String groupId = segmentAssetId;
			deleteCount += app.apiSegmentDb.dbGroups.deleteSingleRowById(groupId);
			deleteCount += app.apiSegmentDb.dbReceived.deleteSegmentsForGroup(groupId);
			deleteCount += app.apiSegmentDb.dbQueued.deleteSegmentsForGroup(groupId);
		}
		return deleteCount;
	}

	private int deleteOrReQueueCompletedSegments(String groupId) {

		int deleteCount = 0;

		if ( (app.apiSegmentDb.dbReceived.getCountByGroupId(groupId) + app.apiSegmentDb.dbQueued.getCountByGroupId(groupId)) == 0 ) {

			deleteCount += deleteSegmentsById(groupId);
			Log.d(logTag, "Segment Group "+groupId+" has been cleared.");

		} else {

			// check to see if segments should be requeued.

		}

		return deleteCount;
	}

	public int setLastAccessedAtById(String segmentAssetId) {

		int updateCount = 0;

		if (segmentAssetId.contains("-")) {
			String groupId = segmentAssetId.split("-")[0];
			int segmentId = segmentId_paddedHexToDec(segmentAssetId.split("-")[1]);
			app.apiSegmentDb.dbReceived.updateLastAccessedAt(groupId, segmentId);
			app.apiSegmentDb.dbQueued.updateLastAccessedAt(groupId, segmentId);
			updateCount++;
		} else {
			String groupId = segmentAssetId;
			app.apiSegmentDb.dbGroups.updateLastAccessedAt(groupId);
			updateCount++;
		}

		return updateCount;
	}

	public int incrementAttemptsById(String segmentAssetId) {

		int updateCount = 0;

		if (segmentAssetId.contains("-")) {
			String groupId = segmentAssetId.split("-")[0];
			int segmentId = segmentId_paddedHexToDec(segmentAssetId.split("-")[1]);
			app.apiSegmentDb.dbReceived.incrementSingleRowAttempts(groupId, segmentId);
			app.apiSegmentDb.dbQueued.incrementSingleRowAttempts(groupId, segmentId);
			updateCount++;
		} else {
			String groupId = segmentAssetId;
			app.apiSegmentDb.dbGroups.incrementSingleRowAttempts(groupId);
			updateCount++;
		}

		return updateCount;
	}

	public int queueSegmentsForDispatch(String groupId) {

		int queuedSegments = 0;

		try {

			String[] grpInfo = app.apiSegmentDb.dbGroups.getSingleRowById(groupId);

			if (grpInfo[0] != null) {

				app.apiSegmentDb.dbGroups.updateLastAccessedAt(groupId);

				String msgProtocol = grpInfo[4];

				for (String[] segmentRow : app.apiSegmentDb.dbQueued.getAllSegmentsForGroupOrderedBySegmentId(groupId)) {

					String segBody = segmentRow[3];
					int segId = Integer.parseInt(segmentRow[2]);

					if (msgProtocol.equalsIgnoreCase("sms")) {

						app.apiSmsUtils.queueSmsToApiToSendImmediately(segBody);
						app.apiSegmentDb.dbQueued.updateLastAccessedAt(groupId, segId);

					} else {

						Log.e(logTag, "Not currently able to send segments over protocol '"+msgProtocol+"'");
						break;
					}

				}

			} else {
				Log.e(logTag, "No Valid Segment Group was found with ID '"+groupId+"'");
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		return queuedSegments;
	}





}
