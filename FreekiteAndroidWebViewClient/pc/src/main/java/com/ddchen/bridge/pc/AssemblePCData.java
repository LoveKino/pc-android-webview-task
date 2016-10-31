package com.ddchen.bridge.pc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yuer on 10/28/16.
 */

public class AssemblePCData {
    public static JSONObject assembleResponse(Object ret, String id) throws JSONException {
        return new JSONObject().
                put("type", "response").
                put("data", new JSONObject().
                        put("id", id).
                        put("data", ret));
    }

    public static JSONObject assembleErrorResponse(Exception error, String id) throws JSONException {
        return new JSONObject().
                put("type", "response").
                put("data", new JSONObject().
                        put("id", id).
                        put("error", new JSONObject().
                                put("msg", error.toString())));
    }


    public static JSONObject assembleRequestData(String name, Object[] args, String id) throws JSONException {
        JSONArray argJsons = new JSONArray();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            argJsons.put(new JSONObject().
                    put("type", "jsonItem").
                    put("arg", arg));
        }
        return new JSONObject().
                put("type", "request").
                put("data", new JSONObject().
                        put("id", id).
                        put("source", new JSONObject().
                                put("type", "public").
                                put("name", name).
                                put("args", argJsons)));
    }
}
