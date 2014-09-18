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

import android.database.Cursor;

import com.google.gson.JsonObject;
import com.j256.ormlite.field.DatabaseField;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by wak on 5/17/14.
 */
public abstract class Model {

    private static final String TAG = Model.class.getSimpleName();

    public static class Column {
        public static final String ID = "_id";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
    }

    static String getStringFromJson(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) == null) {
            return "";
        }
        String itemString = jsonObject.get(key).getAsString();
        if (itemString == null) {
            return "";
        }
        return itemString;
    }

    static String getStringFromCursor(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    static int getIntFromCursor(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    static long getLongFromCursor(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    static Date getDateFromCursor(Cursor cursor, String columnName) {
        return new Date(getLongFromCursor(cursor, columnName));
    }

    /**
     * Get random order SQL
     *
     * http://stackoverflow.com/questions/2171578/seeding-sqlite-random
     *
     * @param seed
     * @return
     */
    public static String getRandomOrderSql(int seed) {
        byte[] digest;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            digest = messageDigest.digest(String.valueOf(seed).getBytes());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        String hash = "";
        for (byte aDigest : digest) {
            int intValue = 0xff & aDigest;
            if (intValue < 0x10) {
                hash += "0" + Integer.toHexString(intValue);
            } else {
                hash += Integer.toHexString(intValue);
            }
        }
        List<String> oldChars = Arrays.asList("0", "a", "b", "c", "d", "e", "f");
        List<String> newChars = Arrays.asList("7", "3", "1", "5", "9", "8", "4");
        int i = 0;
        for (String oldChar : oldChars) {
            String newChar = newChars.get(i);
            hash = hash.replace(oldChar, newChar);
            i += 1;
        }
        return String.format("substr(%s * %s, length(%s) + 2)", Column.ID, hash, Column.ID);
    }

    @DatabaseField(generatedId = true, columnName = Column.ID)
    protected Long id;

    @DatabaseField(canBeNull = false)
    protected Date createdAt;

    @DatabaseField(canBeNull = false)
    protected Date updatedAt;

    public Model() {
        init();
    }

    // TODO: write test
    public boolean equals(Model model) {
        Long modelId = model.getId();
        if (id < 1L || modelId < 1L) {
            return false;
        }
        return id.equals(modelId);
    }

    void init() {
        createdAt = Calendar.getInstance().getTime();
        updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void renewUpdatedAt() {
        this.updatedAt = Calendar.getInstance().getTime();
    }
}
