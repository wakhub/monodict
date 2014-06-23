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
package com.github.wakhub.monodict.activity.settings;

import android.app.ListActivity;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// TODO: refactoring
@EActivity(R.layout.activity_file_selector)
@OptionsMenu({R.menu.file_selector})
public class FileSelectorActivity extends ListActivity {

    private final static String TAG = FileSelectorActivity.class.getSimpleName();

    //private static final int REQUEST_CODE = 10500;

    final public static String RESULT_INTENT_PATH = "path";

    @Extra
    String[] extraExtensions;

    @ViewById
    TextView pathText;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    private ArrayAdapter<String> listAdapter;

    private String lastFullPath;

    private String currentFullPath;

    @AfterViews
    void afterViews() {
        commonActivityTrait.initActivity();
        currentFullPath = "/";
        lastFullPath = currentFullPath;

        getActionBar().setDisplayHomeAsUpEnabled(true);

        setResult(RESULT_CANCELED);

        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        setListAdapter(listAdapter);

        loadContents();
    }

    boolean isRoot() {
        return currentFullPath.equals("/");
    }

    void goUp() {
        lastFullPath = currentFullPath;
        if (currentFullPath.lastIndexOf("/") <= 0) {
            currentFullPath = currentFullPath.substring(0, currentFullPath.lastIndexOf("/") + 1);
        } else {
            currentFullPath = currentFullPath.substring(0, currentFullPath.lastIndexOf("/"));
        }
        loadContents();
    }

    void goIn(String path) {
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
    void onListItemClick(int position) {
        String path = listAdapter.getItem(position);

        if (path.equals("..")) {
            goUp();
        } else if (path.endsWith("/")) {
            goIn(path);
        } else {
            Intent intent = getIntent();
            intent.putExtra(RESULT_INTENT_PATH, currentFullPath + "/" + path);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Background
    void loadContents() {
        Log.d(TAG, "loadContents: " + currentFullPath);
        File[] files = new File(currentFullPath).listFiles();
        if (files == null) {
            activityHelper.showToast(R.string.message_unable_to_access);
            if (!isRoot() && !currentFullPath.equals(lastFullPath)) {
                currentFullPath = lastFullPath;
                loadContents();
            }
            return;
        }

        // ディレクトリ→大文字小文字無視で名前順になるようにソート
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

        ArrayList<String> items = new ArrayList<String>();

        // ルートじゃない場合は、階層を上がれるように".."をArrayListの先頭に設定
        if (!currentFullPath.equals("/")) {
            items.add("..");
        }

        // ファイルは拡張子に一致するもの、ディレクトリは全部、ArrayListに追加する
        for (File file : files) {
            if (file.canRead()) {
                if (file.isDirectory()) {
                    items.add(file.getName() + "/");
                } else {
                    if (extraExtensions != null) {
                        String name = file.getName();
                        String lowerName = name.toLowerCase();
                        for (String ext : extraExtensions) {
                            if (lowerName.endsWith(ext)) {
                                items.add(name);
                            }
                            break;
                        }
                    } else {
                        items.add(file.getName());
                    }
                }
            }
        }
        onLoadContents(items);
    }

    @OptionsItem(R.id.action_help)
    void onActionHelp() {
        activityHelper
                .buildNoticeDialog(activityHelper.getStringFromRaw(R.raw.file_selector_help))
                .setTitle(R.string.title_help)
                .show();
    }

    @UiThread
    void onLoadContents(List<String> items) {
        pathText.setText(currentFullPath);
        listAdapter.clear();
        listAdapter.addAll(items);
    }

    @Override
    public void onBackPressed() {
        if (!isRoot()) {
            goUp();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}