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
package com.github.wakhub.monodict.test.activity;

import android.support.v4.widget.DrawerLayout;
import android.test.ActivityInstrumentationTestCase2;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.BrowserActivity_;
import com.github.wakhub.monodict.activity.FlashcardActivity_;
import com.github.wakhub.monodict.activity.MainActivity_;
import com.github.wakhub.monodict.activity.SettingsActivity_;
import com.github.wakhub.monodict.test.util.Solo;

import java.util.Arrays;
import java.util.List;

/**
 * You have to install some dictionary data before you run the tests
 *
 * Created by wak on 5/9/14.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity_> {

    private static Solo solo;

    boolean isDownloadDialogChecked = false;

    public MainActivityTest() {
        super(MainActivity_.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        if (!isDownloadDialogChecked && solo.searchButton("Cancel")) {
            solo.clickOnButton("Cancel");
        }
        isDownloadDialogChecked = true;
    }

    public void testSearchText() throws Exception {
        List<String> keywords = Arrays.asList("apple", "banana", "this is no results", "the end");
        for (String keyword : keywords) {
            solo.clearEditText(0);
            solo.enterText(0, keyword);
        }
    }

    private void openDrawer() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                DrawerLayout drawerLayout = (DrawerLayout) solo.getView(R.id.root_layout);
                drawerLayout.openDrawer(solo.getView(R.id.drawer_list));
            }
        });
    }

    public void testNav() throws Throwable {
        solo.clearEditText(0);

        openDrawer();
        solo.clickOnView(R.id.flashcards_button);
        solo.waitForActivity(FlashcardActivity_.class);
        solo.goBack();

        openDrawer();
        solo.clickOnView(R.id.browser_button);
        solo.waitForActivity(BrowserActivity_.class);
        solo.goBack();

        openDrawer();
        solo.clickOnView(R.id.settings_button);
        solo.waitForActivity(SettingsActivity_.class);
        solo.goBack();
    }
}

