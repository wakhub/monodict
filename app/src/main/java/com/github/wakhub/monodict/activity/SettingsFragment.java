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

import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.ui.WebViewDialog;

import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

/**
 * Created by wak on 6/8/14.
 */
@EFragment
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final String PREFERENCES_NAME = "Preferences";
    private static final String KEY_ORIENTATION = "orientation";
    private static final String KEY_TTS_DEFAULT_LOCALE = "ttsDefaultLocale";
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

        reload();

    }

    private String getOrientationLabel(String value) {
        Resources resources = getResources();
        String[] orientationValues = resources.getStringArray(R.array.orientation_values);
        String[] orientationLabels = resources.getStringArray(R.array.orientations);

        for (int i = 0; i < orientationValues.length; i++) {
            if (orientationValues[i].equals(value)) {
                return orientationLabels[i];
            }
        }
        return "";
    }

    public void reload() {
        dictionaries.reload();

        Preference prefItem;

        prefItem = findPreference(KEY_ORIENTATION);
        prefItem.setSummary(getOrientationLabel(preferences.orientation().get()));
        prefItem.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(getOrientationLabel((String) o));
                ((SettingsActivity) getActivity()).setOrientation((String) o);
                return true;
            }
        });

        prefItem = findPreference(KEY_TTS_DEFAULT_LOCALE);
        prefItem.setSummary(preferences.ttsDefaultLocale().get());
        prefItem.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary((String) o);
                return true;
            }

        });

        prefItem = findPreference(KEY_ABOUT);
        final PackageInfo packageInfo = MonodictApp.getPackageInfo(getActivity());
        prefItem.setSummary(String.format("%s %s", appName, packageInfo.versionName));
        prefItem.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                WebViewDialog.build(getActivity(),"https://github.com/wakhub/monodict/blob/master/README.rst").show();
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
    }
}
