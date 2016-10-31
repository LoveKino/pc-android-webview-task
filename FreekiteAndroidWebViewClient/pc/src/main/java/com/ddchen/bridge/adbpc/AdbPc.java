package com.ddchen.bridge.adbpc;

import com.ddchen.bridge.adbpc.MonitorCommand.ExecuteCommand;
import com.ddchen.bridge.pc.MessageHandler;
import com.ddchen.bridge.pcinterface.Caller;
import com.ddchen.bridge.pcinterface.HandleCallResult;
import com.ddchen.bridge.pcinterface.Sender;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.ddchen.bridge.pc.AssemblePCData.assembleRequestData;

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

    /**
     * @param channel       used to monitor information from PC
     * @param outputDirName used to send information to PC
     *                      eg: aosp_hook/output
     * @param sandbox
     * @return
     */
    public Caller pc(final String channel, String outputDirName, final Map sandbox) {
        final Sender sender = getSender(channel, outputDirName);

        final MessageHandler handler = new MessageHandler(sandbox, sender, idMap);

        MonitorCommand.monitor(channel, new ExecuteCommand() {
            @Override
            public void execute(String command) {
                /**
                 * 1. parse command (call command)
                 * 2. execute command
                 * 3. send results
                 */
                try {
                    JSONObject jObject = new JSONObject(command);
                    handler.handle(jObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return new Caller() {
            /**
             * {
             *     channel: "/data/user/0/com.freekite.android.yard.adbcontest1/files/aosp_hook/command.json",
             *     data: {
             *         type: "request",
             *         data: {
             *             id: 1,
             *             source: {
             *                 type: "public",
             *                 name: "add",
             *                 args: [{
             *                     type: "jsonItem",
             *                     arg: 1
             *                 }, {
             *                     type: "jsonItem",
             *                     arg: 2
             *                 }]
             *             }
             *         }
             *     }
             * }
             */
            @Override
            public void call(String name, Object[] args, HandleCallResult handleCallResult) {
                // generate id
                String id = UUID.randomUUID().toString();
                // map id
                idMap.put(id, handleCallResult);

                try {
                    sender.send(assembleRequestData(name, args, id));
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
