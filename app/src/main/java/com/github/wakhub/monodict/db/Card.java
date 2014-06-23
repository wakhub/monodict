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
package com.github.wakhub.monodict.db;

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wak on 5/17/14.
 */
@DatabaseTable(tableName = "card")
public class Card extends Model {

    private static final String TAG_SEPARATOR = "|";
    private static final String TAG_SEPARATOR_PATTERN = "\\|";
    public static final int BOX_MIN = 1;
    public static final int BOX_MAX = 9;

    public static class Column extends Model.Column {
        public static final String DISPLAY = "display";
        public static final String TRANSLATE = "translate";
        public static final String NOTE = "note";
        public static final String BOX = "box";
        public static final String DICTIONARY = "dictionary";
        public static final String TAGS = "tags";
    }

    @DatabaseField(canBeNull = false)
    private String display;

    @DatabaseField
    private String translate;

    @DatabaseField
    private String note;

    @DatabaseField
    private int box = 1;

    @DatabaseField
    private String dictionary;

    @DatabaseField
    private String tags = "";

    public static String getStringFromJson(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) == null) {
            return "";
        }
        String itemString = jsonObject.get(key).getAsString();
        if (itemString == null) {
            return "";
        }
        return itemString;
    }

    public Card() {
        super();
    }

    public Card(JsonObject jsonObject) {
        super();
        display = getStringFromJson(jsonObject, Column.DISPLAY);
        translate = getStringFromJson(jsonObject, Column.TRANSLATE);
        dictionary = getStringFromJson(jsonObject, Column.DICTIONARY);
        box = jsonObject.get(Column.BOX).getAsInt();
    }

    @Override
    public String toString() {
        return String.format("Card(id=%d, display=%s)", getId(), display);
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getTranslate() {
        return translate;
    }

    public void setTranslate(String translate) {
        this.translate = translate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setBox(int box) {
        if (box < BOX_MIN || BOX_MAX < box) {
            throw new IllegalArgumentException(String.format("box has to be between %d and %d", BOX_MIN, BOX_MAX));
        }
        this.box = box;
    }

    public int getBox() {
        return box;
    }

    public String getDictionary() {
        return dictionary;
    }

    public void setDictionary(String dictionary) {
        this.dictionary = dictionary;
    }

    public List<String> getTags() {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> tagList = new ArrayList<String>();
        Collections.addAll(tagList, tags.split(TAG_SEPARATOR_PATTERN));
        return tagList;
    }

    public void setTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.tags = "";
            return;
        }
        this.tags = TextUtils.join(TAG_SEPARATOR, tags);
    }
}
