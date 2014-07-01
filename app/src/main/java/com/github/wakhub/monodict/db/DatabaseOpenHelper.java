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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wak on 5/17/14.
 */
public class DatabaseOpenHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = DatabaseOpenHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "data.db";

    private static final int DATABASE_VERSION = 1;

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private List<String> getTableNames(SQLiteDatabase database) {
        List<String> tableNames = new ArrayList<String>();
        Cursor cursor = database.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master", null);
        if (cursor == null) {
            return tableNames;
        }
        if (cursor.getCount() < 1) {
            cursor.close();
            return tableNames;
        }

        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            tableNames.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return tableNames;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        Log.d(TAG, "onCreate");
        List<String> tableNames = getTableNames(database);
        try {

            if (!tableNames.contains("bookmark")) {
                TableUtils.createTableIfNotExists(connectionSource, Bookmark.class);
                Bookmark.initData((Dao<Bookmark, Long>) getDao(Bookmark.class));
            }
            if (!tableNames.contains("card")) {
                TableUtils.createTableIfNotExists(connectionSource, Card.class);
                Card.initData((Dao<Card, Long>) getDao(Card.class));
            }
        } catch (SQLException e) {
            Log.d(TAG, "Failed to create tables", e);
        }
    }

    /**
     * http://www.greenmoonsoftware.com/2012/02/sqlite-schema-migration-in-android/
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        Log.d(TAG, String.format("onUpgrade: %d => %d", oldVersion, newVersion));
        if (REVISIONS.length < 1) {
            return;
        }
        for (int i = oldVersion; i < newVersion; i++) {
            try {
                Revision revision = REVISIONS[i - 1];
                revision.execute(this, connectionSource);
            } catch (Exception e) {
                Log.d(TAG, "Version up error", e);
                throw new RuntimeException(e);
            }
        }
        onCreate(database, connectionSource);
    }

    private static interface Revision {
        public void execute(DatabaseOpenHelper openHelper, ConnectionSource connectionSource)
                throws SQLException;
    }

    /**
     * Don't include "CREATE TABLE" here
     */
    private static final Revision[] REVISIONS = new Revision[]{
// Example
//            new Revision() {
//                @Override
//                public void execute(DatabaseOpenHelper openHelper, ConnectionSource connectionSource)
//                        throws SQLException {
//                    openHelper.getDao(Card.class).executeRaw(String.format(
//                            "ALTER TABLE card ADD COLUMN %s VARCHAR",
//                            Card.Column.DICTIONARY));
//                }
//            }
    };

}
