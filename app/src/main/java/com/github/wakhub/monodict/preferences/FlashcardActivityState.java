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
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.StringPrefEditorField;

import java.util.Calendar;
import java.util.Random;

/**
 * <pre>
 * - box
 * - order
 * - randomSeed
 * </pre>
 * Created by wak on 6/8/14.
 */
@EBean
public class FlashcardActivityState implements JsonPreferencesFieldAdapter.Delegate {

    private static final int RANDOM_MAX = 100000;

    public static final int ORDER_SHUFFLE = 0;
    public static final int ORDER_ALPHABETICALLY = 1;

    @Pref
    Preferences_ preferences;

    JsonPreferencesFieldAdapter adapter;

    private Random random = new Random();

    @AfterInject
    void afterInject() {
        random.setSeed(Calendar.getInstance().getTime().getTime());
        adapter = new JsonPreferencesFieldAdapter(this, Data.class);
        if (getRandomSeed() == 0) {
            refreshRandomSeed();
        }
    }

    public int getBox() {
        return getData().box;
    }

    public void setBox(int box) {
        Data data = getData();
        data.box = box;
        adapter.saveData(data);
    }

    public int getOrder() {
        return getData().order;
    }

    public void setOrder(int order) {
        Data data = getData();
        data.order = order;
        adapter.saveData(data);
    }

    public int getRandomSeed() {
        return getData().randomSeed;
    }

    public void refreshRandomSeed() {
        Data data = getData();
        data.randomSeed = random.nextInt(RANDOM_MAX);
        adapter.saveData(data);
    }

    Data getData() {
        return (Data) adapter.getData();
    }

    @Override
    public String toString() {
        return loadJson().toString();
    }

    @Override
    public String loadJson() {
        return preferences.flashcardActivityState().get();
    }

    @Override
    public StringPrefEditorField<?> edit() {
        return preferences.edit().flashcardActivityState();
    }

    static class Data {
        int box = 1;
        int order = ORDER_SHUFFLE;
        int randomSeed = 0;
    }
}
