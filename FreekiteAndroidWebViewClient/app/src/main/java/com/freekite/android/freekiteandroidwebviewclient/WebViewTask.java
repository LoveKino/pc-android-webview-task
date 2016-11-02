package com.freekite.android.freekiteandroidwebviewclient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ddchen.bridge.pc.PC;
import com.ddchen.bridge.pcinterface.Caller;
import com.ddchen.bridge.pcinterface.Listener;
import com.ddchen.bridge.pcinterface.SandboxFunction;
import com.ddchen.bridge.pcinterface.Sender;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuer on 10/31/16.
 */

public class WebViewTask {
    private Activity context = null;
    private WebView webView = null;

    private String startJsContent = null;

    private Caller directCaller = null;

    public WebViewTask(Activity context, Caller directCaller) {
        this.context = context;
        this.directCaller = directCaller;
        this.webView = new WebView(context);
        buildBridge();
        configWebView(this.webView);
    }

    public Caller buildBridge() {
        Listener listener = new Listener() {
            @Override
            public void listen(final ListenHandler listenHandler) {
                JSBridge jsBridge = new JSBridge() {
                    @Override
                    @JavascriptInterface
                    public void accept(String json) {
                        try {
                            listenHandler.handle(new JSONObject(json));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                // export js object
                WebViewTask.this.webView.addJavascriptInterface(jsBridge, "__webView_bridge");
            }
        };

        Sender sender = new Sender() {
            @Override
            public void send(final Object msg) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                            JSONObject json = (JSONObject) msg;
                            webView.evaluateJavascript("window.__onWebViewMessage(" + json + ")", null);
                        } else {
                            // TODO support it
                        }
                    }
                });
            }
        };

        Map sandbox = new HashMap();

        sandbox.put("directToCenter", new SandboxFunction() {
            @Override
            public Object apply(Object[] args) {
                return directCaller.call("directToCenter", args);
            }
        });

        return new PC(listener, sender, sandbox).getCaller();
    }

    public WebView getWebView() {
        return this.webView;
    }

    public interface JSBridge {
        void accept(String json);
    }

    // TODO send task to webView
    public void runTask(String type, JSONObject content) throws JSONException {
        if (type.equals("runPage")) {
            String url = content.getString("startPageUrl");
            String startJsContent = content.getString("startJsContent");
            this.startJsContent = startJsContent;
            this.webView.loadUrl(url);
        }
    }

    public void configWebView(final WebView webView) {
        context.getWindow().requestFeature(Window.FEATURE_PROGRESS);

        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                context.setProgress(progress * 1000);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(context, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = WebViewTask.this.startJsContent;
                if (js != null) {
                    // run these js code at first
                    // webView.loadUrl("javascript:" + js);
                    if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                        webView.evaluateJavascript(js, null);
                    } else {
                        // TODO support it!
                    }
                }
            }
        });
    }
}
