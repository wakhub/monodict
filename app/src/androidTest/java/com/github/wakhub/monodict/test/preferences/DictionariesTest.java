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

package com.github.wakhub.monodict.test.preferences;

import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.test.util.UnitTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by wak on 1/17/15.
 */
public class DictionariesTest extends UnitTest {

    private Gson gson;

    @Override
    public void setUp() throws Exception {
        gson = new GsonBuilder().serializeNulls().excludeFieldsWithoutExposeAnnotation().create();
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testParse() {
        DummyDictionaries dictionaries = gson.fromJson("{\"items\": ["
                + "{"
                + "\"name\": \"dic1\","
                + "\"path\": \"/tmp/dic1\","
                + "\"isEnglish\": true,"
                + "\"enabled\": true"
                + "},{"
                + "\"name\": \"dic2\","
                + "\"path\": \"/tmp/dic2\","
                + "\"isEnglish\": true,"
                + "\"enabled\": true"
                + "}"
                + "]}", DummyDictionaries.class);
        Dictionary dic1 = dictionaries.items.get(0);
        Dictionary dic2 = dictionaries.items.get(1);

        assertEquals(2, dictionaries.items.size());

        assertEquals("dic1", dic1.getName());
        assertEquals("Default", dic1.getIndexFont().getName());
        assertEquals("", dic1.getIndexFont().getPath());
        assertEquals(10, dic1.getMaxResults());

        assertEquals("dic2", dic2.getName());

        dictionaries = gson.fromJson("{\"items\": ["
                + "{"
                + "\"name\": \"dic1\","
                + "\"path\": \"/tmp/dic1\","
                + "\"isEnglish\": true,"
                + "\"enabled\": true,"
                + "\"maxResults\": 20,"
                + "\"indexFont\": {"
                + "\"path\": \"/tmp/font1.ttf\","
                + "\"style\": 0,"
                + "\"size\": 10"
                + "}"
                + "},{"
                + "\"name\": \"dic2\","
                + "\"path\": \"/tmp/dic2\","
                + "\"isEnglish\": true,"
                + "\"enabled\": true"
                + "}"
                + "]}", DummyDictionaries.class);
        dic1 = dictionaries.items.get(0);
        dic2 = dictionaries.items.get(1);

        assertEquals(2, dictionaries.items.size());
        assertEquals(20, dic1.getMaxResults());
        assertEquals("/tmp/font1.ttf", dic1.getIndexFont().getPath());
        assertEquals("dic2", dic2.getName());
    }

    public static final class DummyDictionaries {
        @Expose
        List<Dictionary> items;
    }
}
