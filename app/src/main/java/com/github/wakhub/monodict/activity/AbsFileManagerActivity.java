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

import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.melnykov.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @see com.github.wakhub.monodict.activity.FileSelectorActivity
 * @see com.github.wakhub.monodict.activity.DirectorySelectorActivity
 */
@EActivity
public abstract class AbsFileManagerActivity extends AbsListActivity {

    private static final String TAG = AbsFileManagerActivity.class.getSimpleName();

    final public static String RESULT_INTENT_PATH = "path";

    @ViewById
    protected TextView pathText;

    @ViewById
    protected FloatingActionButton backButton;

    @ViewById
    protected FloatingActionButton nextButton;

    @Pref
    protected Preferences_ preferences;

    @Bean
    protected CommonActivityTrait commonActivityTrait;

    @Bean
    protected ActivityHelper activityHelper;

    protected ArrayAdapter<String> listAdapter;

    protected String lastFullPath = "";

    protected String currentFullPath = "";

    @AfterViews
    protected void afterViewsAbsFileManagerActivity() {
        if (currentFullPath.isEmpty()) {
            currentFullPath = "/";
        }

        backButton.attachToListView(getListView());

        setResult(RESULT_CANCELED);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        setListAdapter(listAdapter);

        loadContents();
    }

    @Click(R.id.back_button)
    protected void onClickBackButton() {
        goUp();
    }

    protected boolean isDirectory(String path) {
        return path.equals("..") || path.endsWith("/");
    }

    protected boolean isRoot() {
        return currentFullPath.equals("/");
    }

    protected void goUp() {
        lastFullPath = currentFullPath;
        if (currentFullPath.lastIndexOf("/") <= 0) {
            currentFullPath = currentFullPath.substring(0, currentFullPath.lastIndexOf("/") + 1);
        } else {
            currentFullPath = currentFullPath.substring(0, currentFullPath.lastIndexOf("/"));
        }
        loadContents();
    }

    protected void goIn(String path) {
        lastFullPath = currentFullPath;
        if (currentFullPath.equals("/")) {
            currentFullPath += path;
        } else {
            currentFullPath = currentFullPath + "/" + path;
        }
        currentFullPath = currentFullPath.substring(0, currentFullPath.length() - 1);
        loadContents();
    }

    @ItemClick(android.R.id.list)
    protected void onListItemClick(int position) {
        String path = listAdapter.getItem(position);

        if (path.equals("..")) {
            goUp();
        } else if (path.endsWith("/")) {
            goIn(path);
        }
    }

    protected File[] getFilesAt(String path) {
        File[] files = new File(currentFullPath).listFiles();
        if (files == null) {
            return null;
        }

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File object1, File object2) {
                final boolean object1IsDir = object1.isDirectory();
                final boolean object2IsDir = object2.isDirectory();

                if (object1IsDir ^ object2IsDir) {
                    if (object1IsDir) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                return object1.getName().compareToIgnoreCase(object2.getName());
            }
        });
        return files;
    }

    protected void loadContents() {
        Log.d(TAG, "loadContents: " + currentFullPath);
        File[] files = getFilesAt(currentFullPath);
        if (files == null) {
            activityHelper.showToast(R.string.message_unable_to_access);
            if (!isRoot()) {
                goUp();
            }
            return;
        }

        ArrayList<String> items = new ArrayList<String>();

        if (!currentFullPath.equals("/")) {
            items.add("..");
        }

        for (File file : files) {
            if (file.canRead()) {
                if (file.isDirectory()) {
                    items.add(file.getName() + "/");
                } else {
                    items.add(file.getName());
                }
            }
        }
        onLoadContents(items);
    }

    @UiThread
    protected void onLoadContents(List<String> items) {
        pathText.setText(currentFullPath);
        listAdapter.clear();
        listAdapter.addAll(items);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (commonActivityTrait.onMenuItemSelected(item.getItemId(), item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
