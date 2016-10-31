package com.ddchen.bridge.pc;

import com.ddchen.bridge.pcinterface.SandboxFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by yuer on 10/28/16.
 */

public class SandboxRuner {
    public static Object getFunctionRet(Map sandbox, String methodName, JSONArray args) throws JSONException {
        SandboxFunction fun = (SandboxFunction) sandbox.get(methodName);
        Object[] params = new Object[args.length()];
        for (int i = 0; i < args.length(); i++) {
            JSONObject arg = args.getJSONObject(i);
            params[i] = arg.get("arg");
        }
        Object ret = fun.apply(params);

        return ret;
    }
}
