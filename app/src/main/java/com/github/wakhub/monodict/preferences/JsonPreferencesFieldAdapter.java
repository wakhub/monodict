/**
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
package com.github.wakhub.monodict.preferences;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.androidannotations.api.sharedpreferences.StringPrefEditorField;

/**
 * Created by wak on 6/8/14.
 */
class JsonPreferencesFieldAdapter {

    private static final String TAG = JsonPreferencesFieldAdapter.class.getSimpleName();

    private Interface delegate = null;

    private Class dataClass = null;

    private Object data = null;

    public JsonPreferencesFieldAdapter(Interface delegate, Class dataClass) {
        this.delegate = delegate;
        this.dataClass = dataClass;
        load();
    }

    public void load() {
        String json = delegate.loadJson();
        Gson gson = (new GsonBuilder()).create();
        data = gson.fromJson(json, dataClass);
    }

    public void saveData(Object data) {
        Gson gson = (new GsonBuilder()).create();
        String json = gson.toJson(data, dataClass);
        Log.d(TAG, "saveData: " + json);
        delegate.edit().put(json).apply();
        load();
    }

    public Object getData() {
        return data;
    }

    public interface Interface {
        String loadJson();

        StringPrefEditorField<?> edit();
    }
}