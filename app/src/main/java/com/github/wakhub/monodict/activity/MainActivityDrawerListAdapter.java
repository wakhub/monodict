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
import android.widget.CompoundButton;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionaries_;
import com.github.wakhub.monodict.preferences.Dictionary;
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

    static interface Listener {
        void onDrawerClickDictionaryItem(Dictionary dictionary);
        void onDrawerChangeDictionaryItemCheckbox(Dictionary dictionary, boolean isChecked);
        void onDrawerClickDownloadButton();
        void onDrawerClickAddLocalPdicButton();
        void onDrawerClickSettingsButton();
    }

    private final Listener listener;

    public MainActivityDrawerListAdapter(Context context, Listener listener) {
        super(context, R.layout.list_item_drawer, android.R.id.text1);
        this.listener = listener;
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
        items.add(new DictionaryActionsItem());
        items.add(new SettingsItem());
        addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        Item item = getItem(position);

        if (item.getType().equals(ItemType.DICTIONARY_ITEM)) {
            return getDictionaryItemView(view, item);
        }
        if (item.getType().equals(ItemType.DICTIONARY_ACTIONS)) {
            return getDictionaryActionsView(view);
        }
        if (item.getType().equals(ItemType.SETTINGS)) {
            return getSettingsView(view);
        }
        return view;
    }

    private View getDictionaryItemView(View view, Item item) {
        view.findViewById(R.id.dictionary_item_layout).setVisibility(View.VISIBLE);
        view.findViewById(R.id.dictionary_actions_layout).setVisibility(View.GONE);
        view.findViewById(R.id.settings_layout).setVisibility(View.GONE);

        final Dictionary dictionary = ((DictionaryItem) item).getDictionary();

        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        text1.setText(item.getTitle());

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.check_box);
        checkBox.setChecked(dictionary.isEnabled());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onDrawerChangeDictionaryItemCheckbox(dictionary, isChecked);
            }
        });

        view.findViewById(R.id.clickable_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDrawerClickDictionaryItem(dictionary);
            }
        });
        return view;
    }

    private View getDictionaryActionsView(View view) {
        view.findViewById(R.id.dictionary_item_layout).setVisibility(View.GONE);
        view.findViewById(R.id.dictionary_actions_layout).setVisibility(View.VISIBLE);
        view.findViewById(R.id.settings_layout).setVisibility(View.GONE);
        view.findViewById(R.id.download_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDrawerClickDownloadButton();
            }
        });
        view.findViewById(R.id.add_local_pdic_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDrawerClickAddLocalPdicButton();
            }
        });
        return view;
    }

    private View getSettingsView(View view) {
        view.findViewById(R.id.dictionary_item_layout).setVisibility(View.GONE);
        view.findViewById(R.id.dictionary_actions_layout).setVisibility(View.GONE);
        view.findViewById(R.id.settings_layout).setVisibility(View.VISIBLE);
        view.findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDrawerClickSettingsButton();
            }
        });
        return view;
    }

    public static enum ItemType {
        DICTIONARY_ITEM, DICTIONARY_ACTIONS, SETTINGS
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
            return ItemType.DICTIONARY_ITEM;
        }

        @Override
        public String getTitle() {
            return dictionary.getName();
        }

        public Dictionary getDictionary() {
            return dictionary;
        }
    }

    public static final class DictionaryActionsItem implements Item {
        @Override
        public ItemType getType() {
            return ItemType.DICTIONARY_ACTIONS;
        }

        @Override
        public String getTitle() {
            return "";
        }
    }

    public static final class SettingsItem implements Item {

        @Override
        public ItemType getType() {
            return ItemType.SETTINGS;
        }

        @Override
        public String getTitle() {
            return "";
        }
    }
}
