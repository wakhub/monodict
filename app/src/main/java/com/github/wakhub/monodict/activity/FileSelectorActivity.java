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
package com.github.wakhub.monodict.activity;

import android.content.Intent;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.FileSelectorActivityState;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ItemClick;

@EActivity(R.layout.activity_abs_file_manager)
public class FileSelectorActivity extends AbsFileManagerActivity {

    private final static String TAG = FileSelectorActivity.class.getSimpleName();

    public final static String RESULT_INTENT_FILENAME = "filename";

    //private static final int REQUEST_CODE = 10130;

    @Extra
    String extraTitle;

    @Extra
    String[] extraExtensions;

    @Bean
    FileSelectorActivityState state;

    @Override
    @AfterViews
    protected void afterViews() {
        if (extraTitle != null) {
            setTitle(extraTitle);
        }
        currentFullPath = state.getLastPath();
        super.afterViews();
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
        String returnPath = null;
        if (extraExtensions == null) {
            returnPath = path;
        } else {
            for (String ext : extraExtensions) {
                if (path.endsWith(ext)) {
                    returnPath = path;
                }
            }
        }
        if (returnPath == null) {
            return;
        }
        Intent intent = getIntent();
        intent.putExtra(RESULT_INTENT_PATH, currentFullPath);
        intent.putExtra(RESULT_INTENT_FILENAME, returnPath);
        setResult(RESULT_OK, intent);
        finish();
    }
}