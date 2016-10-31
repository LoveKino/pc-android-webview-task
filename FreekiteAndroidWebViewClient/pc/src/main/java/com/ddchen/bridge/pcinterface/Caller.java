package com.ddchen.bridge.pcinterface;

/**
 * Created by yuer on 10/28/16.
 */

public interface Caller {
    void call(String name, Object[] args, HandleCallResult handleCallResult);
}
