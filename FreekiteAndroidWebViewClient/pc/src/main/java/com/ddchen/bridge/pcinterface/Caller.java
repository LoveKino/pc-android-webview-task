package com.ddchen.bridge.pcinterface;

import com.ddchen.bridge.pc.Promise;

/**
 * Created by yuer on 10/28/16.
 */

public interface Caller {
    Promise call(String name, Object[] args);
}
