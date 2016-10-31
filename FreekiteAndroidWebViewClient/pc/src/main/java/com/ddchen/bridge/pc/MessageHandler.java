package com.ddchen.bridge.pc;

import com.ddchen.bridge.pcinterface.HandleCallResult;
import com.ddchen.bridge.pcinterface.Sender;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.ddchen.bridge.pc.AssemblePCData.assembleErrorResponse;
import static com.ddchen.bridge.pc.AssemblePCData.assembleResponse;
import static com.ddchen.bridge.pc.SandboxRuner.getFunctionRet;

/**
 * Created by yuer on 10/28/16.
 */

public class MessageHandler {
    private Map sandbox;
    private Sender sender;
    private Map idMap;

    public MessageHandler(Map sandbox, Sender sender, Map idMap) {
        this.sandbox = sandbox;
        this.sender = sender;
        this.idMap = idMap;
    }

    /**
     * {
     * type: "response",
     * data: {
     * data: 3,
     * id: "1"
     * }
     * }
     * <p>
     * {
     * type: "request",
     * data: {
     * id: "122",
     * source: {
     * type: "public",
     * name: "testCallback",
     * args: [{type: "jsonItem", arg: 1}]
     * }
     * }
     * }
     */
    public void handle(JSONObject jObject) throws JSONException {
        if (jObject.getString("type").equals("response")) {
            handleResponse(jObject);
        } else if (jObject.getString("type").equals("request")) {
            handleRequest(jObject);
        }
    }

    public void handleResponse(JSONObject jObject) throws JSONException {
        JSONObject responseData = jObject.getJSONObject("data");
        String id = responseData.getString("id");
        if (idMap.containsKey(id)) {
            HandleCallResult handleCallResult = (HandleCallResult) idMap.get(id);
            if (handleCallResult != null) {
                if (responseData.has("error") && responseData.get("error") != null) {
                    handleCallResult.handleError(
                            (JSONObject) responseData.get("error")
                    );
                } else {
                    handleCallResult.handle(responseData.get("data"));
                }
                idMap.remove(id);
            }
        } else {
            System.out.println(idMap);
            System.out.println("missing id " + id + " for id map." + "response json is " + jObject);
        }
    }

    public void handleRequest(JSONObject jObject) throws JSONException {
        JSONObject data = jObject.getJSONObject("data");
        String id = data.getString("id");

        JSONObject source = data.getJSONObject("source");
        String methodName = source.getString("name");
        JSONArray args = source.getJSONArray("args");

        try {
            if (sandbox.containsKey(methodName)) {
                sender.send(assembleResponse(getFunctionRet(sandbox, methodName, args), id));
            } else {
                throw new Error("missing method " + methodName);
            }
        } catch (Exception error) {
            sender.send(assembleErrorResponse(error, id));
        }
    }
}
