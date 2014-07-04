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
import android.content.res.Resources;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.github.wakhub.monodict.R;

/**
 * Created by wak on 6/21/14.
 */
public class DictionarySearchView extends SearchView {

    private final View searchPlate;
    private final TextView textView;
    private final ImageView icon;
    private final ImageView closeButton;

    private Listener listener;

    public interface Listener {
        void onSearchViewFocusChange(boolean b);

        void onSearchViewQueryTextSubmit(String query);

        void onSearchViewQueryTextChange(String s);
    }

    public DictionarySearchView(Context context, Listener listener) {
        super(context);

        this.listener = listener;

        Resources resources = getResources();

        // http://www.techrepublic.com/blog/software-engineer/pro-tip-customize-the-android-search-view-widget/
        // http://novoda.com/blog/styling-actionbar-searchview
        icon = (ImageView) findViewById(resources.getIdentifier("android:id/search_mag_icon", null, null));
        searchPlate = findViewById(resources.getIdentifier("android:id/search_plate", null, null));
        textView = (TextView) findViewById(resources.getIdentifier("android:id/search_src_text", null, null));
        closeButton = (ImageView) findViewById(resources.getIdentifier("android:id/search_close_btn", null, null));

        initViews();
    }

    void initViews() {
        setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);

        Resources resources = getResources();

        setQueryRefinementEnabled(true);
        setIconifiedByDefault(false);
        setQueryHint(resources.getString(R.string.title_keyword));

        icon.setImageDrawable(resources.getDrawable(R.drawable.ic_action_search));

        searchPlate.setBackgroundResource(R.drawable.search_view_background);

        textView.setOnFocusChangeListener(new OnFocusChangeListener());
        setOnQueryTextListener(new OnQueryTextListener());

        closeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_action_cancel));
    }

    // TODO: http://stackoverflow.com/questions/6918364/edittext-does-not-trigger-changes-when-back-is-pressed
    private class OnFocusChangeListener implements SearchView.OnFocusChangeListener {
        @Override
        public void onFocusChange(View view, boolean b) {
            listener.onSearchViewFocusChange(b);
        }
    }

    private class OnQueryTextListener implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String s) {
            clearFocus();
            listener.onSearchViewQueryTextSubmit(s);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            listener.onSearchViewQueryTextChange(s);
            return false;
        }
    }
}
