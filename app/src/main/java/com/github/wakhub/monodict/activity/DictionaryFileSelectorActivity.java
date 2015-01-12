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

import android.content.Intent;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.DictionaryFileSelectorActivityState;
import com.github.wakhub.monodict.preferences.Preferences_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.sharedpreferences.Pref;

/**
 * Created by wak on 6/27/14.
 */
@EActivity(R.layout.activity_abs_file_manager)
@OptionsMenu({R.menu.file_selector})
public class DictionaryFileSelectorActivity extends AbsFileManagerActivity {

    private final static String TAG = DictionaryFileSelectorActivity.class.getSimpleName();

    public final static String RESULT_INTENT_FILENAME = "filename";

    private static final String DICTIONARY_EXT = ".dic";

    @Pref
    Preferences_ preferences;

    @Bean
    DictionaryFileSelectorActivityState state;

    @Override
    @AfterViews
    protected void afterViewsAbsFileManagerActivity() {
        currentFullPath = state.getLastPath();
        super.afterViewsAbsFileManagerActivity();
        commonActivityTrait.initActivity(preferences);
    }

    @OptionsItem(R.id.action_help)
    void onActionHelp() {
        activityHelper
                .buildNoticeDialog(activityHelper.getStringFromRaw(R.raw.file_selector_help))
                .title(R.string.title_help)
                .show();
    }

    @Override
    protected void loadContents() {
        state.setLastPath(currentFullPath);
        super.loadContents();
    }

    @Override
    @ItemClick(android.R.id.list)
    protected void onListItemClick(int position) {
        String path = listAdapter.getItem(position);
        if (isDirectory(path)) {
            super.onListItemClick(position);
            return;
        }

        if (!path.endsWith(DICTIONARY_EXT)) {
            return;
        }
        Intent intent = getIntent();
        intent.putExtra(RESULT_INTENT_PATH, currentFullPath);
        intent.putExtra(RESULT_INTENT_FILENAME, path);
        setResult(RESULT_OK, intent);
        finish();
    }
}
