package com.ddchen.bridge.pcinterface;

import org.json.JSONObject;

/**
 * Created by yuer on 10/28/16.
 */

public interface HandleCallResult {
    void handle(Object json);

    void handleError(JSONObject errorInfo);
}
