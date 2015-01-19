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

package com.github.wakhub.monodict.activity;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class BrowserActivityJavaScriptInterface {

    private static final String TAG = BrowserActivityJavaScriptInterface.class.getSimpleName();

    public static final String NAME = "__com_github_wakhub_monodict";

    public static final String ON_GET_SELECTION_METHOD = "onBrowserJavaScriptGetSelection";

    public interface Listener {
        void onJavaScriptInterfaceGetSelection(String callback, String selection);
    }

    private final Listener listener;

    public BrowserActivityJavaScriptInterface(Listener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void onBrowserJavaScriptGetSelection(String callback, String selection) {
        Log.d(TAG, "onBrowserJavaScriptGetSelection");
        listener.onJavaScriptInterfaceGetSelection(callback, selection);
    }
}

