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

package com.github.wakhub.monodict.activity;

import android.content.Intent;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.FontFileSelectorActivityState;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.google.common.base.Joiner;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.sharedpreferences.Pref;

/**
 * Created by wak on 6/27/14.
 */
@EActivity(R.layout.activity_abs_file_manager)
public class FontFileSelectorActivity extends AbsFileManagerActivity {

    private final static String TAG = FontFileSelectorActivity.class.getSimpleName();

    public final static String RESULT_INTENT_FILENAME = "filename";

    private static final String[] FONT_EXT_LIST = new String[]{".ttf", ".otf"};

    @Pref
    Preferences_ preferences;

    @Bean
    FontFileSelectorActivityState state;

    @Override
    @AfterViews
    protected void afterViewsAbsFileManagerActivity() {
        currentFullPath = state.getLastPath();
        super.afterViewsAbsFileManagerActivity();
        commonActivityTrait.initActivity(preferences);
    }

    private boolean isValidFontFile(String path) {
        for (String ext : FONT_EXT_LIST) {
            if (path.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
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
        if (!isValidFontFile(path)) {
            activityHelper.showToast(getResources().getString(
                    R.string.message_validation_file_ext, Joiner.on(",").join(FONT_EXT_LIST)));
            return;
        }
        Intent intent = getIntent();
        intent.putExtra(RESULT_INTENT_PATH, currentFullPath);
        intent.putExtra(RESULT_INTENT_FILENAME, path);
        setResult(RESULT_OK, intent);
        finish();
    }
}
