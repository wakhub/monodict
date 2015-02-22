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
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.Dictionary;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 6/10/14.
 */
public class DictionaryContextMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = DictionaryContextMenu.class.getSimpleName();

    private static final List<Integer> ITEM_IDS = Arrays.asList(
            R.string.action_more_detail,
            R.string.action_up,
            R.string.action_down);

    private OnContextActionListener contextActionListener;

    private final Context context;

    private final Dictionary dictionary;

    public DictionaryContextMenu(Context context, View anchor, Dictionary dictionary) {
        super(context, anchor);
        this.context = context;
        this.dictionary = dictionary;
        Resources resources = context.getResources();
        Menu menu = getMenu();
        for (Integer id : ITEM_IDS) {
            menu.add(resources.getString(id));
        }
        setOnMenuItemClickListener(this);
    }

    public DictionaryContextMenu setContextActionListener(OnContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
        return this;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (contextActionListener == null) {
            return false;
        }
        CharSequence title = item.getTitle();
        Resources resources = context.getResources();
        if (title.equals(resources.getString(R.string.action_more_detail))) {
            contextActionListener.onContextActionMoreDetail(dictionary);
            return true;
        }
        if (title.equals(resources.getString(R.string.action_up))) {
            contextActionListener.onContextActionUp(dictionary);
            return true;
        }
        if (title.equals(resources.getString(R.string.action_down))) {
            contextActionListener.onContextActionDown(dictionary);
            return true;
        }
        return false;
    }

    public interface OnContextActionListener {
        void onContextActionMoreDetail(Dictionary dictionary);

        void onContextActionUp(Dictionary dictionary);

        void onContextActionDown(Dictionary dictionary);
    }
}
