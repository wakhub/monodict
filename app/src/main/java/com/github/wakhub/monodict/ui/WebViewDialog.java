/*
 * Copyright (C) 2015 wak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.wakhub.monodict.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.R;

/**
 * Created by wak on 6/28/14.
 */
public class WebViewDialog extends MaterialDialog {

    public static WebViewDialog build(Context context, String url) {
        return new WebViewDialog(
                new Builder(context)
                        .customView(new ContentView(context, url), false)
                        .positiveText(android.R.string.ok)
        );
    }

    public WebViewDialog(Builder builder) {
        super(builder);
    }

    private static final class ContentView extends RelativeLayout {
        private final String url;
        private final WebView webView;
        private final ProgressBar progressBar;

        private ContentView(Context context, String url) {
            super(context);
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            this.url = url;
            webView = new WebView(context);

            webView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.equals(ContentView.this.url)) {
                        return true;
                    }
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    getContext().startActivity(intent);
                    progressBar.setVisibility(View.GONE);
                    return true;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    webView.loadData(
                            getContext().getResources().getString(R.string.message_network_error), null, null);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    progressBar.setVisibility(View.GONE);
                }
            });
            addView(webView);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
            progressBar.setLayoutParams(layoutParams);
            addView(progressBar);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            webView.loadUrl(url);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            webView.destroy();
        }
    }
}
