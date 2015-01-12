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
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.github.wakhub.monodict.R;

import java.lang.ref.WeakReference;

/**
 * Created by wak on 6/21/14.
 */
public class DictionarySearchView extends SearchView {

    private View searchPlate;
    private TextView textView;
    private ImageView icon;
    private ImageView closeButton;
    private InputMethodManager inputMethodManager;
    private Listener listener;

    public interface Listener {
        void onSearchViewFocusChange(boolean b);

        void onSearchViewQueryTextSubmit(String query);

        void onSearchViewQueryTextChange(String s);
    }

    public DictionarySearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        Resources res = getResources();

        // http://www.techrepublic.com/blog/software-engineer/pro-tip-customize-the-android-search-view-widget/
        // http://novoda.com/blog/styling-actionbar-searchview
        // http://stackoverflow.com/questions/10445760/how-to-change-the-default-icon-on-the-searchview-to-be-use-in-the-action-bar-on
        icon = (ImageView) findViewById(res.getIdentifier("android:id/search_mag_icon", null, null));
        searchPlate = findViewById(res.getIdentifier("android:id/search_plate", null, null));
        textView = (TextView) findViewById(res.getIdentifier("android:id/search_src_text", null, null));
        closeButton = (ImageView) findViewById(res.getIdentifier("android:id/search_close_btn", null, null));

        initViews();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void focus() {
        textView.requestFocus();
        inputMethodManager.showSoftInput(textView, InputMethodManager.SHOW_FORCED);
    }

    public void clear() {
        textView.setText("");
    }


    void initViews() {
        Resources res = getResources();

        setQueryRefinementEnabled(true);
        setQueryHint(res.getString(R.string.title_keyword));

        // TODO: Doesn't work well
        icon.setImageResource(R.drawable.ic_search_grey600_24dp);
        searchPlate.setBackgroundResource(R.drawable.search_view_background);
        closeButton.setImageDrawable(res.getDrawable(R.drawable.ic_close_grey600_24dp));
        textView.setOnFocusChangeListener(new OnFocusChangeListener(listener));

        setOnQueryTextListener(new OnQueryTextListener(this));
    }

    private static class OnFocusChangeListener implements SearchView.OnFocusChangeListener {

        private final WeakReference<Listener> listenerRef;

        private OnFocusChangeListener(Listener listener) {
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        public void onFocusChange(View view, boolean b) {
            if (listenerRef.get() != null) {
                listenerRef.get().onSearchViewFocusChange(b);
            }
        }
    }

    private static class OnQueryTextListener implements SearchView.OnQueryTextListener {

        private final WeakReference<DictionarySearchView> viewRef;

        OnQueryTextListener(DictionarySearchView view) {
            viewRef = new WeakReference<>(view);
        }

        @Override
        public boolean onQueryTextSubmit(String s) {
            if (viewRef.get() != null) {
                viewRef.get().clearFocus();
                viewRef.get().listener.onSearchViewQueryTextSubmit(s);
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (viewRef.get() != null) {
                viewRef.get().listener.onSearchViewQueryTextChange(s);
            }
            return false;
        }
    }
}
