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
package com.github.wakhub.monodict.activity.bean;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Point;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.res.DimensionRes;

/**
 * Created by wak on 5/19/14.
 */
@EBean
public class DimensionHelper {

    @RootContext
    Activity activity;

    @DimensionRes
    float spaceSuperRelax;

    /**
     * http://stackoverflow.com/questions/3407256/height-of-status-bar-in-android
     *
     * @return int
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * http://stackoverflow.com/questions/7165830/what-is-the-size-of-actionbar-in-pixels
     *
     * @return
     */
    public int getActionBarHeight() {
        final TypedArray styledAttributes = activity.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int height = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return height;
    }

    /**
     * http://stackoverflow.com/questions/1016896/how-to-get-screen-dimensions
     *
     * @return
     */
    public Point getDisplaySize() {
        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        return displaySize;
    }
}
