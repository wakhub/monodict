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

    private Delegate delegate = null;

    private Class dataClass = null;

    private Object data = null;

    public JsonPreferencesFieldAdapter(Delegate delegate, Class dataClass) {
        this.delegate = delegate;
        this.dataClass = dataClass;
        load();
    }

    private Gson createGson() {
        return new GsonBuilder().serializeNulls().excludeFieldsWithoutExposeAnnotation().create();
    }

    public void load() {
        String json = delegate.loadJson();
        data = createGson().fromJson(json, dataClass);
    }

    public void saveData(Object data) {
        String json = createGson().toJson(data, dataClass);
        Log.d(TAG, "saveData: " + json);
        delegate.edit().put(json).apply();
        load();
    }

    /**
     * The fields which is serialized by JsonPreferencesFieldAdapter has to decorated by @Expose
     *
     * http://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/annotations/Expose.html
     *
     * @return
     */
    public Object getData() {
        return data;
    }

    public interface Delegate {
        String loadJson();

        StringPrefEditorField<?> edit();
    }
}