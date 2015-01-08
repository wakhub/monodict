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

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.webkit.WebView;

public class BrowserActivityWebView extends WebView {

    private static final String TAG = BrowserActivityWebView.class.getSimpleName();

    public static interface ActionModeListener {
        ActionMode onWebViewStartActionMode(ActionMode actionMode);
    }

    private ActionModeListener actionModeListener;

    public BrowserActivityWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setActionModeListener(ActionModeListener actionModeListener) {
        this.actionModeListener = actionModeListener;
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        ActionMode actionMode = super.startActionMode(callback);
        if (actionModeListener != null) {
            return actionModeListener.onWebViewStartActionMode(actionMode);
        }
        return actionMode;
    }
}
