/*
 * Copyright (C) 2014 wak
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

package com.github.wakhub.monodict.search;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by wak on 6/17/14.
 */
public final class DictionaryServiceConnection implements ServiceConnection {

    private final static String TAG = DictionaryServiceConnection.class.getSimpleName();

    private DictionaryService.DictionaryServiceBinder binder;

    private DictionaryService.Listener listener;

    private boolean isConnected = false;

    public DictionaryServiceConnection(DictionaryService.Listener listener) {
        this.listener = listener;
    }

    public void search(String query) {
        if (!isConnected) {
            return;
        }
        binder.search(query);
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected");
        binder = ((DictionaryService.DictionaryServiceBinder) iBinder);
        binder.setListener(this.listener);
        isConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        binder.removeListener();
        binder = null;
    }
}
