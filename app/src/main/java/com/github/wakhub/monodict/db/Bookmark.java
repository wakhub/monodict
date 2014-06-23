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

package com.github.wakhub.monodict.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 6/16/14.
 */
@DatabaseTable(tableName = "bookmark")
public class Bookmark extends Model {

    public static class Column extends Model.Column {
        public static final String URL = "url";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
    }

    @DatabaseField(canBeNull = false)
    private String url;

    @DatabaseField(canBeNull = false)
    private String title;

    @DatabaseField
    private String description;

    public Bookmark() {
    }

    public Bookmark(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static void initData(Dao<Bookmark, Long> dao) throws SQLException {
        List<Bookmark> bookmarkData = Arrays.asList(
                new Bookmark("http://m.gutenberg.org/", "Project Gutenberg"),
                new Bookmark("http://en.wikipedia.org/wiki/Main_Page", "Wikipedia"),
                new Bookmark("http://mobile.nytimes.com/international/", "The New York Times"),
                new Bookmark("http://m.bbc.com", "BBC"),
                new Bookmark("http://m.aljazeera.com/index/home", "AJE - Al Jazeera English"),
                new Bookmark("http://www.channelnewsasia.com/mobile", "Channel NewsAsia"),
                new Bookmark("http://www.japantoday.com/smartphone", "Japan Today"));
        for (Bookmark bookmark : bookmarkData) {
            dao.create(bookmark);
        }
    }
}
