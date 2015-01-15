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

import com.google.gson.annotations.Expose;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.StringPrefEditorField;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wak on 6/8/14.
 */
@EBean
public class Dictionaries implements JsonPreferencesFieldAdapter.Delegate {

    private static final String TAG = Dictionaries.class.getSimpleName();

    @Pref
    Preferences_ preferences;

    private JsonPreferencesFieldAdapter adapter;

    @AfterInject
    void afterInject() {
        adapter = new JsonPreferencesFieldAdapter(this, Data.class);
        Data data = getData();
        if (data.dictionaries == null) {
            data.dictionaries = new ArrayList<>();
            adapter.saveData(data);
        }
    }

    public void reload() {
        adapter.load();
    }

    public boolean hasDictionary(Dictionary dictionary) {
        for (Dictionary storedDictionary : getData().dictionaries) {
            if (dictionary.equals(storedDictionary)) {
                return true;
            }
        }
        return false;
    }

    public void swap(Dictionary dictionary, int direction) {
        int current = getDictionaryIndex(dictionary);
        Log.d(TAG, String.format("swap: %s %d %d", dictionary, direction, current));
        Data data = getData();
        if (direction == 0) {
            return;
        } else if (direction < 0 && current > 0) {
            data.dictionaries.remove(dictionary);
            data.dictionaries.add(current - 1, dictionary);
        } else if (direction > 0 && current < data.dictionaries.size() - 1) {
            data.dictionaries.remove(dictionary);
            data.dictionaries.add(current + 1, dictionary);
        }
        adapter.saveData(data);
    }

    public int getDictionaryCount() {
        Data data = getData();
        if (data == null || data.dictionaries == null) {
            return 0;
        }
        return data.dictionaries.size();
    }

    public boolean addDictionary(Dictionary dictionary) {
        if (hasDictionary(dictionary)) {
            Log.d(TAG, "Already has the dictionary: " + dictionary);
            return false;
        }
        Data data = getData();
        data.dictionaries.add(dictionary);
        adapter.saveData(data);
        return true;
    }

    public boolean updateDictionary(Dictionary dictionary) {
        if (!hasDictionary(dictionary)) {
            return false;
        }
        Data data = getData();
        Log.d(TAG, "BEFORE: " + toString());
        int i = 0;
        for (Dictionary storedDictionary : data.dictionaries) {
            if (dictionary.equals(storedDictionary)) {
                data.dictionaries.set(i, dictionary);
            }
            i++;
        }
        adapter.saveData(data);
        Log.d(TAG, "AFTER: " + toString());

        return true;
    }

    public Dictionary getDictionaryByPath(String path) {
        for (Dictionary dictionary : getData().dictionaries) {
            if (dictionary.getPath().equals(path)) {
                return dictionary;
            }
        }
        return null;
    }

    public Dictionary getDictionary(int index) {
        Data data = getData();
        if (data.dictionaries.size() - 1 < index) {
            return null;
        }
        return data.dictionaries.get(index);
    }

    public int getDictionaryIndex(Dictionary dictionary) {
        return getData().dictionaries.indexOf(dictionary);
    }

    public void removeDictionary(Dictionary dictionary) {
        removeDictionaryAt(getDictionaryIndex(dictionary));
    }

    public void removeDictionaryAt(int index) {
        Data data = getData();
        data.dictionaries.remove(index);
        adapter.saveData(data);
    }

    Data getData() {
        Data data = (Data) adapter.getData();
        if (data == null) {
            data = new Data();
        }
        return data;
    }

    @Override
    public String toString() {
        return loadJson().toString();
    }

    @Override
    public String loadJson() {
        Log.d(TAG, "loadJson: " + preferences.dictionaries().get());
        return preferences.dictionaries().get();
    }

    @Override
    public StringPrefEditorField<?> edit() {
        return preferences.edit().dictionaries();
    }

    static final class Data {
        @Expose
        List<Dictionary> dictionaries = new ArrayList<>();
    }
}
