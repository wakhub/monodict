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

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.StringPrefEditorField;

/**
 * <pre>
 * - lastUrl
 * </pre>
 * Created by wak on 6/8/14.
 */
@EBean
public class BrowserActivityState implements JsonPreferencesFieldAdapter.Interface {

    @Pref
    Preferences_ preferences;

    private JsonPreferencesFieldAdapter adapter;

    @AfterInject
    void afterInject() {
        adapter = new JsonPreferencesFieldAdapter(this, Data.class);
    }

    public String getLastUrl() {
        String lastUrl = getData().lastUrl;
        if (lastUrl == null) {
            lastUrl = "";
        }
        return lastUrl;
    }

    @Background
    public void setLastUrl(String lastUrl) {
        Data data = getData();
        data.lastUrl = lastUrl;
        adapter.saveData(data);
    }

    Data getData() {
        return (Data) adapter.getData();
    }

    @Override
    public String loadJson() {
        return preferences.browserActivityState().get();
    }

    @Override
    public StringPrefEditorField<?> edit() {
        return preferences.edit().browserActivityState();
    }

    class Data {
        String lastUrl = "";
    }
}
