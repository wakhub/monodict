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
package com.github.wakhub.monodict.activity.bean;

import android.app.ActionBar;
import android.app.Activity;
import android.view.MenuItem;

import com.github.wakhub.monodict.activity.MainActivity;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

/**
 * Created by wak on 5/19/14.
 */
@EBean
public class CommonActivityTrait {

    //private static final int REQUEST_CODE = 20000;

    @RootContext
    Activity activity;

    private boolean isGoingBack = false;

    public void initActivity() {
        if (activity instanceof MainActivity) {
            return;
        }
        ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (activity.getParent() != null) {
                    isGoingBack = true;
                    activity.finish();
                } else {
                    isGoingBack = true;
                    activity.onBackPressed();
                }
                return true;
        }
        return false;
    }

    public boolean isGoingBack() {
        return isGoingBack;
    }
}
