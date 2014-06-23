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
package com.github.wakhub.monodict;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import com.github.wakhub.monodict.preferences.Preferences_;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.Locale;

/**
 * Application
 */
@EApplication
public class MonodictApp extends Application {

    private static final String TAG = MonodictApp.class.getSimpleName();

    @Pref
    Preferences_ preferences;

    @AfterInject
    void afterInject() {
//        setLocale(Locale.JAPANESE);
    }

    public PackageInfo getPackageInfo() {
        String packageName = getApplicationInfo().packageName;
        try {
            return getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean isVersionUp() {
        PackageInfo packageInfo = getPackageInfo();
        int lastVersionCode = preferences.lastVersionCode().get();
        Log.d(TAG, String.format("packageInfo.versionCode=%d, lastVersionCode=%d",
                packageInfo.versionCode,
                lastVersionCode));
        if (packageInfo.versionCode == lastVersionCode) {
            return false;
        }
        preferences.edit().lastVersionCode().put(packageInfo.versionCode).apply();
        return true;
    }

    private void setLocale(Locale locale) {
        Resources resources = getResources();
        Configuration conf = resources.getConfiguration();
        conf.locale = locale;
        resources.updateConfiguration(conf, resources.getDisplayMetrics());
    }
}
