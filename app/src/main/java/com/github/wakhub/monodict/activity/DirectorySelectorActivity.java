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
import com.github.wakhub.monodict.preferences.DirectorySelectorActivityState;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

/**
 * Created by wak on 6/26/14.
 */
@EActivity(R.layout.activity_abs_file_manager)
@OptionsMenu({R.menu.directory_selector})
public class DirectorySelectorActivity extends AbsFileManagerActivity {

    private static final String TAG = DirectorySelectorActivity.class.getSimpleName();

    //private static final int REQUEST_CODE = 10040;

    @Bean
    DirectorySelectorActivityState state;

    @Override
    @AfterViews
    protected void afterViews() {
        currentFullPath = state.getLastPath();
        super.afterViews();
    }

    @Override
    protected void loadContents() {
        state.setLastPath(currentFullPath);
        super.loadContents();
    }

    @OptionsItem(R.id.action_ok)
    void onActionOk() {
        Intent intent = getIntent();
        intent.putExtra(RESULT_INTENT_PATH, currentFullPath);
        setResult(RESULT_OK, intent);
        finish();
    }
}
