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

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Preferences_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wak on 6/8/14.
 */
@EFragment
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final String PREFERENCES_NAME = "Preferences";
    private static final String KEY_DICTIONARY_MANAGER = "dictionaryManager";
    private static final String KEY_ABOUT = "about";
    private static final String KEY_LEGAL = "legal";

    @App
    MonodictApp app;

    @Pref
    Preferences_ preferences;

    @Bean
    Dictionaries dictionaries;

    @Bean
    ActivityHelper activityHelper;

    @StringRes
    String appName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // http://stackoverflow.com/questions/10970068
        getPreferenceManager().setSharedPreferencesName(PREFERENCES_NAME);
        addPreferencesFromResource(R.xml.preferences);
    }

    @AfterViews
    void afterViews() {
        reload();
    }

    public void reload() {
        dictionaries.reload();

        Preference prefItem;

        prefItem = findPreference(KEY_ABOUT);
        PackageInfo packageInfo = app.getPackageInfo();
        prefItem.setSummary(String.format(
                "%s %s",
                appName,
                packageInfo.versionName));
        prefItem.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activityHelper.buildNoticeDialog(activityHelper.getHtmlFromRaw(R.raw.about)).show();
                return false;
            }
        });

        findPreference(KEY_LEGAL).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activityHelper.buildNoticeDialog(activityHelper.getStringFromRaw(R.raw.legal)).show();
                return false;
            }
        });

        prefItem = findPreference(KEY_DICTIONARY_MANAGER);
        List<String> dictionaryNames = new ArrayList<String>();
        int size = dictionaries.getDictionaryCount();
        for (int i = 0; i < size; i++) {
            dictionaryNames.add(dictionaries.getDictionary(i).getNameWithEmoji());
        }
        if (dictionaryNames.isEmpty()) {
            prefItem.setSummary(getResources().getString(R.string.action_add_dictionary));
        } else {
            prefItem.setSummary(TextUtils.join("\n", dictionaryNames));
        }
        prefItem.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DictionaryManagerActivity_.intent(getActivity()).start();
                return false;
            }
        });
    }
}
