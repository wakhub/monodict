/*
 * Copyright (C) 2015 wak
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
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.wakhub.monodict.R;

/**
 * Created by wak on 6/21/14.
 */
public class HeadingView extends FrameLayout {

    TextView textView;

    public HeadingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.view_heading, this);

        textView = (TextView) findViewById(R.id.text_view);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setText(String text) {
        textView.setText(text);
    }
}

