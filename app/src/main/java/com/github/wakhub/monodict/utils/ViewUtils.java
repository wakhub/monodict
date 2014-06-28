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

package com.github.wakhub.monodict.utils;

import android.view.View;
import android.view.ViewParent;

/**
 * Created by wak on 6/28/14.
 */
public class ViewUtils {

    private static ViewUtils instance;

    public static ViewUtils getInstance() {
        if (instance == null) {
            instance = new ViewUtils();
        }
        return instance;
    }

    public ViewParent findParentView(View rootView, Class findClass) {
        View currentView = rootView;
        for (int i = 0; i < 100; i++) {
            ViewParent parentView = currentView.getParent();
            if (findClass.isInstance(parentView)) {
                return parentView;
            }
            currentView = (View)parentView;
        }

        return null;
    }
}
