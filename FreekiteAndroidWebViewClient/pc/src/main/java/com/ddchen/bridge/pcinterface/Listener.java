package com.ddchen.bridge.pcinterface;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yuer on 11/2/16.
 */

public interface Listener {
    interface ListenHandler {
        void handle(JSONObject data) throws JSONException;
    }

    void listen(ListenHandler listenHandler);
}

