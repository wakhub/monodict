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

package com.github.wakhub.monodict.ui;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;

import com.github.wakhub.monodict.activity.bean.DimensionHelper;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EView;

import java.lang.ref.WeakReference;

/**
 * Created by wak on 9/22/14.
 * <p/>
 * EditText does not trigger changes when back is pressed
 * http://stackoverflow.com/questions/6918364/edittext-does-not-trigger-changes-when-back-is-pressed
 */
@EView
public class MainActivityRootLayout extends DrawerLayout {

    @Bean
    DimensionHelper dimensionHelper;

    public interface Listener {
        void onSoftKeyboardShown(boolean isShowing);
    }

    private WeakReference<Listener> listenerRef = new WeakReference<>(null);

    public MainActivityRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Listener listener) {
        this.listenerRef = new WeakReference<>(listener);
    }

    /**
     * http://stackoverflow.com/questions/6918364/edittext-does-not-trigger-changes-when-back-is-pressed
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int displayHeight = dimensionHelper.getDisplaySize().y
                - dimensionHelper.getStatusBarHeight()
                - dimensionHelper.getActionBarHeight();
        int diff = displayHeight - height;
        if (listenerRef.get() != null) {
            listenerRef.get().onSoftKeyboardShown(diff > 128); // assume all soft
            // keyboards are at
            // least 128 pixels high
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
