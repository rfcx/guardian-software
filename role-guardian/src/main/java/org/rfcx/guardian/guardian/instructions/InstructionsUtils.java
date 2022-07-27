package org.rfcx.guardian.guardian.instructions;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class InstructionsUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsUtils");
    private static final String[] localProtocols = new String[]{"socket", "contentprovider"};
    private final RfcxGuardian app;

    public InstructionsUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
    }

    public void processReceivedInstructionJson(JSONArray instrJsonArr, String originProtocol) {
        try {
            List<String> queuedInstrIds = new ArrayList<>();

            for (int i = 0; i < instrJsonArr.length(); i++) {

                JSONObject instrObj = instrJsonArr.getJSONObject(i);

                if (instrObj.has("id")) {

                    String instrId = instrObj.getString("id");

                    if ((this.app.instructionsDb.dbExecuted.getCountById(instrId) == 0)
                            && (this.app.instructionsDb.dbQueued.getCountById(instrId) == 0)
                    ) {

                        String instrType = instrObj.getString("type");

                        String instrCmd = instrObj.getString("cmd");

                        JSONObject instrMetaObj = (instrObj.getString("meta").length() > 0) ? new JSONObject(instrObj.getString("meta")) : new JSONObject();

                        long executionBuffer = ArrayUtils.doesStringArrayContainString(localProtocols, originProtocol) ? 0 : InstructionsCycleService.CYCLE_DURATION;

                        long instrExecuteAt = ((instrObj.getString("at").length() > 0) ? Long.parseLong(instrObj.getString("at")) : System.currentTimeMillis()) + executionBuffer;

                        this.app.instructionsDb.dbQueued.findByIdOrCreate(instrId, instrType, instrCmd, instrExecuteAt, instrMetaObj.toString(), originProtocol);

                        Log.i(logTag, "Instruction Received (via " + originProtocol.toUpperCase(Locale.US) + ") with ID '" + instrId + "', Type: '" + instrType + "', Command: '" + instrCmd + "', Send at " + DateTimeUtils.getDateTime(instrExecuteAt) + ", JSON Meta: '" + instrMetaObj + "'");

                        queuedInstrIds.add(instrId);
                    }
                }
            }

            // Origin if from a Remote source
            if (!ArrayUtils.doesStringArrayContainString(localProtocols, originProtocol)) {

                // Send reception receipt to remote API
                if (queuedInstrIds.size() > 0) {
                    this.app.apiPingUtils.sendPing(false, new String[]{"instructions"}, true);
                }

                // Origin if from a Local source
            } else {

                // Attempt to execute instruction immediately, without waiting for instruction cycle
                if (app.instructionsDb.dbQueued.getCount() > 0) {
                    app.rfcxSvc.triggerService(InstructionsExecutionService.SERVICE_NAME, false);
                }

            }


        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);

        }
    }

    public JSONObject getInstructionsInfoAsJson() {

        JSONObject instrObj = new JSONObject();
        try {

            JSONArray receivedInstrArr = new JSONArray();
            for (String[] receivedRow : app.instructionsDb.dbQueued.getRowsInOrderOfExecution()) {
                if (receivedRow[0] != null) {
                    //				if ( (includeOnlyOriginProtocols.length == 0) || ((receivedRow[10] != null) && ArrayUtils.doesStringArrayContainString(includeOnlyOriginProtocols, receivedRow[10])) ) {
                    JSONObject receivedObj = new JSONObject();
                    receivedObj.put("id", receivedRow[1]);
                    receivedObj.put("received_at", receivedRow[0]);
                    receivedInstrArr.put(receivedObj);
                    app.instructionsDb.dbQueued.updateLastAccessedAtById(receivedRow[1]);
                    //				}
                }
            }
            instrObj.put("received", receivedInstrArr);

            JSONArray executedInstrArr = new JSONArray();
            for (String[] executedRow : app.instructionsDb.dbExecuted.getRowsInOrderOfExecution()) {
                if (executedRow[0] != null) {
                    //				if ( (includeOnlyOriginProtocols.length == 0) || ((executedRow[10] != null) && ArrayUtils.doesStringArrayContainString(includeOnlyOriginProtocols, executedRow[10])) ) {
                    JSONObject executedObj = new JSONObject();
                    executedObj.put("id", executedRow[1]);
                    executedObj.put("received_at", executedRow[7]);
                    executedObj.put("executed_at", executedRow[0]);
                    executedObj.put("attempts", executedRow[6]);
                    executedObj.put("response", executedRow[5]);
                    executedInstrArr.put(executedObj);
                    app.instructionsDb.dbExecuted.updateLastAccessedAtById(executedRow[1]);
                    //				}
                }
            }
            instrObj.put("executed", executedInstrArr);

        } catch (JSONException e) {
            RfcxLog.logExc(logTag, e);
        }
        return instrObj;
    }

    public int getInstructionsCount() {
        return app.instructionsDb.dbQueued.getCount() + app.instructionsDb.dbExecuted.getCount();
    }


    public String executeInstruction(String instrType, String instrCmd, JSONObject instrMeta) {

        JSONObject responseJson = new JSONObject();

        try {

            // Set Pref[s]
            if (instrType.equalsIgnoreCase("set") && instrCmd.equalsIgnoreCase("prefs")) {

                if (!instrMeta.toString().equalsIgnoreCase("{}")) {
                    JSONObject prefsKeysVals = instrMeta;
                    Iterator<String> prefsKeys = prefsKeysVals.keys();
                    while (prefsKeys.hasNext()) {
                        String prefKey = prefsKeys.next();
                        if (prefsKeysVals.getString(prefKey) instanceof String) {
                            app.setSharedPref(prefKey.toLowerCase(), prefsKeysVals.getString(prefKey).toLowerCase());
                        }
                    }
                }

            } else if (instrType.equalsIgnoreCase("ctrl")) {

                String commandValue = null;

                if (!instrMeta.toString().equalsIgnoreCase("{}")) {
                    JSONObject metaKeysVals = instrMeta;
                    Iterator<String> metaKeys = metaKeysVals.keys();
                    while (metaKeys.hasNext()) {
                        String metaKey = metaKeys.next();
                        if (metaKeysVals.getString(metaKey) instanceof String) {
                            commandValue = metaKeysVals.getString(metaKey);
                            break;
                        }
                    }
                }

                app.deviceControlUtils.runOrTriggerDeviceCommand(instrCmd, commandValue, app.getResolver());

                // Execute Send Command
            } else if (instrType.equalsIgnoreCase("send")) {

                if (instrCmd.equalsIgnoreCase("ping")) {

                    JSONArray inclFieldsArr = instrMeta.getJSONArray("include");
                    String[] inclFields = new String[inclFieldsArr.length()];
                    for (int i = 0; i < inclFieldsArr.length(); i++) {
                        inclFields[i] = inclFieldsArr.getString(i).toLowerCase();
                    }
                    app.apiPingUtils.sendPing(false, inclFields, true);

                } else if (instrCmd.equalsIgnoreCase("sms")) {

                    String sendAt = instrMeta.has("at") ? "" + Long.parseLong(instrMeta.getString("at")) : "" + System.currentTimeMillis();
                    String sendTo = instrMeta.has("to") ? instrMeta.getString("to") : app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SMS_ADDRESS);
                    String msgBody = instrMeta.has("body") ? instrMeta.getString("body") : "";

                    app.apiSmsUtils.queueSmsToSend(sendAt, sendTo, msgBody);

                }

                // Set Identity / Registration (only works if guardian is unregistered, or has no auth token)
            } else if (instrType.equalsIgnoreCase("set") && instrCmd.equalsIgnoreCase("identity")) {

                if (!app.isGuardianRegistered() && app.saveGuardianRegistration(instrMeta.toString())) {
                    responseJson = instrMeta;
                }/* else {


				}*/
            } else if (instrType.equalsIgnoreCase("set") && instrCmd.equalsIgnoreCase("classifier")) {

                if (!instrMeta.toString().equalsIgnoreCase("{}")) {
                    JSONObject classifierObj = instrMeta;
                    String type = classifierObj.getString("type");
                    String classifierId = classifierObj.getString("id");
                    if (type.equalsIgnoreCase("activate")) {
                        app.audioClassifyUtils.activateClassifier(classifierId);
                    } else {
                        app.audioClassifyUtils.deActivateClassifier(classifierId);
                    }
                }
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }


        return responseJson.toString();
    }


}
