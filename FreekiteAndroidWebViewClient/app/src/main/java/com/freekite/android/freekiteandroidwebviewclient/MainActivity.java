package com.freekite.android.freekiteandroidwebviewclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebViewTask webViewTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.webViewTask = new WebViewTask(this);

        // this.webViewTask.getWebView().loadUrl("file:///android_asset/test1.html");
        this.webViewTask.getWebView().loadUrl("file:///android_asset/test2/index.html");

        setContentView(this.webViewTask.getWebView());
    }
}
