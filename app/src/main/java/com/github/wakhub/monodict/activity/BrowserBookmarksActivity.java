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

package com.github.wakhub.monodict.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.db.Bookmark;
import com.github.wakhub.monodict.preferences.Preferences_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ItemLongClick;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.sql.SQLException;
import java.util.List;


@EActivity(R.layout.activity_browser_bookmarks)
@OptionsMenu({R.menu.browser_bookmarks})
public class BrowserBookmarksActivity extends AbsListActivity {

    //private static final int REQUEST_CODE = 10030;

    public static final String EXTRA_URL = "url";

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @Bean
    DatabaseHelper databaseHelper;

    private ArrayAdapter<Bookmark> listAdapter;

    @AfterViews
    void afterViews() {
        commonActivityTrait.initActivity(preferences);
        listAdapter = new ArrayAdapter<Bookmark>(this, android.R.layout.simple_list_item_2, android.R.id.text1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Bookmark bookmark = getItem(position);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(bookmark.getTitle());
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text2.setText(bookmark.getUrl());
                return view;
            }
        };
        setListAdapter(listAdapter);
        loadBookmarks();
    }

    @Background
    void loadBookmarks() {
        List<Bookmark> bookmarks;
        try {
            bookmarks = databaseHelper.findAllBookmarks();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
        onLoadBookmarks(bookmarks);
    }

    @UiThread
    void onLoadBookmarks(List<Bookmark> bookmarks) {
        listAdapter.clear();
        listAdapter.addAll(bookmarks);
    }

    @ItemClick(android.R.id.list)
    void onListItemClick(int position) {
        Bookmark bookmark = listAdapter.getItem(position);
        Intent intent = getIntent();
        intent.putExtra(EXTRA_URL, bookmark.getUrl());
        setResult(RESULT_OK, intent);
        finish();
    }

    @ItemLongClick(android.R.id.list)
    void onListItemLongClick(int position) {
        final Bookmark bookmark = listAdapter.getItem(position);
        activityHelper
                .buildConfirmDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            databaseHelper.deleteBookmark(bookmark);
                        } catch (SQLException e) {
                            activityHelper.showError(e);
                            return;
                        }
                        activityHelper.showToast(getResources().getString(R.string.message_item_removed, bookmark.getTitle()));
                        loadBookmarks();
                    }
                })
                .setTitle(R.string.action_delete)
                .setMessage(R.string.message_confirm_delete).show();
    }
}
