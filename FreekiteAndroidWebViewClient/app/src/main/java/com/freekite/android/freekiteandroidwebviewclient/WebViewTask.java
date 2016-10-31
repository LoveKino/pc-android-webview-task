package com.freekite.android.freekiteandroidwebviewclient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * Created by yuer on 10/31/16.
 */

public class WebViewTask {
    private Activity context = null;
    private WebView webView = null;

    public WebViewTask(Activity context) {
        this.context = context;
        this.webView = new WebView(context);

        // TODO export js object
        this.webView.addJavascriptInterface(new JSInterface(), "testBridge");

        configWebView(this.webView);
    }

    public WebView getWebView() {
        return this.webView;
    }

    public class JSInterface {
        @JavascriptInterface
        public void test() {
            Toast.makeText(context, "test bridge", Toast.LENGTH_LONG).show();
        }
    }

    // TODO send task to webView
    public void runTask(String type, Object content) {
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
                System.out.println("******************");
                System.out.println(url);
                // insert js code
                webView.loadUrl("javascript:testBridge.test()");
                super.onPageStarted(view, url, favicon);
            }
        });
    }
}
