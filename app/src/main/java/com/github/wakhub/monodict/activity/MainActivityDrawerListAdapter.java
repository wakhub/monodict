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

package com.github.wakhub.monodict.activity;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.settings.SettingsActivity_;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionaries_;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.ui.DictionaryContextDialogBuilder;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;

/**
 * Created by wak on 9/22/14.
 * <p/>
 * EditText does not trigger changes when back is pressed
 * http://stackoverflow.com/questions/6918364/edittext-does-not-trigger-changes-when-back-is-pressed
 */
public class MainActivityDrawerListAdapter extends ArrayAdapter<MainActivityDrawerListAdapter.Item> {

    private static final String TAG = MainActivityDrawerListAdapter.class.getSimpleName();

    public MainActivityDrawerListAdapter(Context context) {
        super(context, R.layout.list_item_drawer, android.R.id.text1);
        reload();
    }

    public void reload() {
        Log.d(TAG, "reload");
        clear();
        ArrayList<Item> items = new ArrayList<>();
        Dictionaries dictionaries = Dictionaries_.getInstance_(getContext());
        dictionaries.reload();
        for (int i = 0; i < dictionaries.getDictionaryCount(); i++) {
            items.add(new DictionaryItem(dictionaries.getDictionary(i)));
        }
        Log.d(TAG, items.toString());
        items.add(new SettingsItem());
        addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Item item = getItem(position);
        View view = super.getView(position, convertView, parent);
        View clickableView = view.findViewById(R.id.clickable_view);
        ImageView image = (ImageView) view.findViewById(R.id.image);
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.check_box);
        if (item.getType().equals(ItemType.DICTIONARY)) {
            checkBox.setVisibility(View.VISIBLE);
            final Dictionary dictionary = ((DictionaryItem) item).getDictionary();
            clickableView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new DictionaryContextDialogBuilder(getContext(), dictionary).show();
                }
            });
            image.setImageResource(R.drawable.ic_dictionary_black_24dp);
            image.setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.check_box).setVisibility(View.GONE);
            clickableView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsActivity_.intent(getContext()).start();
                }
            });
        }
        text1.setText(item.getTitle());
        return view;
    }

    public static enum ItemType {
        DICTIONARY, SETTINGS
    }

    public static interface Item {
        ItemType getType();

        String getTitle();
    }

    public static final class DictionaryItem implements Item {

        private final Dictionary dictionary;

        public DictionaryItem(Dictionary dictionary) {
            this.dictionary = dictionary;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .addValue(getDictionary().getName())
                    .toString();
        }

        @Override
        public ItemType getType() {
            return ItemType.DICTIONARY;
        }

        @Override
        public String getTitle() {
            return dictionary.getName();
        }

        public Dictionary getDictionary() {
            return dictionary;
        }
    }

    public static final class SettingsItem implements Item {

        @Override
        public ItemType getType() {
            return ItemType.SETTINGS;
        }

        @Override
        public String getTitle() {
            return "Settings";
        }
    }
}
