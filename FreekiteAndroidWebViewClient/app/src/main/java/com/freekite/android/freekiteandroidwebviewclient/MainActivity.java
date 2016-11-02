package com.freekite.android.freekiteandroidwebviewclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ddchen.bridge.adbpc.AdbPc;
import com.ddchen.bridge.pcinterface.Caller;
import com.ddchen.bridge.pcinterface.SandboxFunction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private WebViewTask webViewTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.webViewTask = new WebViewTask(this, connectToPC());

        setContentView(this.webViewTask.getWebView());
    }

    private Caller connectToPC () {
        AdbPc adbPc = new AdbPc();

        String commandDir = "/data/user/0/" + this.getPackageName() + "/files/aosp_hook/output";
        String channel = "/data/user/0/" + this.getPackageName() + "/files/aosp_hook/command.json";

        Map sandbox = new HashMap();

        sandbox.put("runTask", new SandboxFunction() {
            @Override
            public Object apply(Object[] args) {
                final String type = (String) args[0];
                final JSONObject contentJson = (JSONObject) args[1];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.this.webViewTask.runTask(type, contentJson);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                return null;
            }
        });

        return adbPc.pc(channel, commandDir, sandbox);
    }
}
