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

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.BrowserActivity_;
import com.jayway.android.robotium.solo.Solo;

/**
 * Created by wak on 5/9/14.
 */
public class BrowserActivityTest extends ActivityInstrumentationTestCase2<BrowserActivity_> {

    private static Solo solo;

    public BrowserActivityTest(){
        super(BrowserActivity_.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testActionBar() throws Exception {
        solo.clearEditText(0);
        solo.enterText(0, "http://example.com");
        solo.clickOnActionBarItem(R.id.action_back);
        solo.clickOnActionBarItem(R.id.action_forward);
        solo.clickOnActionBarItem(R.id.action_reload);
        solo.clickOnActionBarItem(R.id.action_reload);
        solo.clickOnActionBarItem(R.id.action_reload);
    }
}

