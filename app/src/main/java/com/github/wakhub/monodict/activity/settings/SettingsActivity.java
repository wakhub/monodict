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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.preferences.Preferences_;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.sharedpreferences.Pref;

/**
 * Created by wak on 6/8/14.
 *
 * @see SettingsActivity_
 */
@EActivity
public class SettingsActivity extends Activity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    //private static final int REQUEST_CODE = 10100;

    @Extra
    boolean extraOpenDownloads = false;

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsFragment = new SettingsFragment_();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment).commit();
        commonActivityTrait.initActivity(preferences);

        if (extraOpenDownloads) {
            Log.d(TAG, "EXTRA: openDownloads");
            DictionaryManagerActivity_.intent(this).extraOpenDownloads(true).start();
        }
    }

    void setOrientation(String orientation) {
        commonActivityTrait.setOrientation(orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsFragment.reload();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}
