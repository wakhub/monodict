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
package com.github.wakhub.monodict.test;

import android.test.ActivityInstrumentationTestCase2;

import com.github.wakhub.monodict.activity.settings.SettingsActivity_;
import com.jayway.android.robotium.solo.Solo;

/**
 * Created by wak on 5/9/14.
 */
public class SettingsActivityTest extends ActivityInstrumentationTestCase2<SettingsActivity_> {

    private static Solo solo;

    public SettingsActivityTest() {
        super(SettingsActivity_.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testFastScroll() {
        String label = "Fast Scroll";
        solo.clickOnText(label);
        solo.clickOnText(label);
        solo.clickOnText(label);
        solo.clickOnText(label);
    }

    public void testClipboardSearch() {
        String label = "Clipboard Search";
        solo.clickOnText(label);
        solo.clickOnText(label);
        solo.clickOnText(label);
        solo.clickOnText(label);
    }

    public void testLanguageForSpeech() {
        String label = "Default Language";
        solo.clickOnText(label);
        solo.clickOnText("Cancel");
        solo.clickOnText(label);
        solo.clickOnText("Cancel");
    }

    public void testAbout() {
        solo.clickOnText("About");
    }

    public void testLegal() {
        solo.clickOnText("Legal");
    }
}

