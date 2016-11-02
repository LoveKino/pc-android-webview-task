package com.ddchen.bridge.adbpc;

import com.ddchen.bridge.adbpc.MonitorCommand.ExecuteCommand;
import com.ddchen.bridge.pc.PC;
import com.ddchen.bridge.pcinterface.Caller;
import com.ddchen.bridge.pcinterface.Listener;
import com.ddchen.bridge.pcinterface.Sender;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuer on 10/19/16.
 */

/**
 * {
 * channel: "/data/user/0/com.freekite.android.yard.adbcontest1/files/aosp_hook/command.json",
 * data: {
 * type: "request",
 * data: {
 * id: 1,
 * source: {
 * type: "public",
 * name: "add",
 * args: [{
 * type: "jsonItem",
 * arg: 1
 * }, {
 * type: "jsonItem",
 * arg: 2
 * }]
 * }
 * }
 * }
 * }
 * <p>
 * {
 * type: "response",
 * data: {
 * data: 3,
 * id: "1"
 * }
 * }
 * <p>
 * TODO
 * 1. queue support
 * 2. resend if error happened
 */

public class AdbPc {
    private HashMap idMap = new HashMap();

    public AdbPc() {
    }

    /**
     * @param channel       used to monitor information from PC
     * @param outputDirName used to send information to PC
     *                      eg: aosp_hook/output
     * @param sandbox
     * @return
     */
    public Caller pc(final String channel, String outputDirName, final Map sandbox) {
        final Sender sender = getSender(channel, outputDirName);

        Listener listener = new Listener() {
            @Override
            public void listen(final ListenHandler listenHandler) {
                MonitorCommand.monitor(channel, new ExecuteCommand() {
                    @Override
                    public void execute(String command) {
                        try {
                            JSONObject jObject = new JSONObject(command);
                            listenHandler.handle(jObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        return new PC(listener, sender, sandbox).getCaller();
    }

    private Sender getSender(final String channel, String outputDirName) {
        final AdbSender adbSender = new AdbSender(outputDirName);
        return new Sender() {
            @Override
            public void send(Object msg) {
                try {
                    adbSender.send(wrapChannel(msg, channel));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private JSONObject wrapChannel(Object data, String channel) throws JSONException {
        return new JSONObject().
                put("channel", channel).
                put("data", data);
    }
}
