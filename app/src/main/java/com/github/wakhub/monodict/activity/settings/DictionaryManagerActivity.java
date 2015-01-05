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
package com.github.wakhub.monodict.activity.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.AbsListActivity;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.dice.DiceFactory;
import com.github.wakhub.monodict.dice.IdicInfo;
import com.github.wakhub.monodict.dice.Idice;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.ui.DictionaryContextDialogBuilder;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@EActivity(R.layout.activity_dictionary_manager)
@OptionsMenu({R.menu.dictionary_manager})
public class DictionaryManagerActivity extends AbsListActivity
        implements DictionaryContextDialogBuilder.OnContextActionListener {

    private static final String TAG = DictionaryManagerActivity.class.getSimpleName();

    private static final int REQUEST_CODE_DOWNLOAD_DICTIONARY = 10110;
    private static final int REQUEST_CODE_ADD_DICTIONARY = 10111;

    @Extra
    boolean extraOpenDownloads = false;

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @Bean
    Dictionaries dictionaries;

    private Idice dice;

    private ListAdapter listAdapter;

    @AfterViews
    void afterViews() {
        commonActivityTrait.initActivity(preferences);

        dice = DiceFactory.getInstance();

        listAdapter = new ListAdapter(this);
        setListAdapter(listAdapter);
        loadDictionaries();

        if (extraOpenDownloads) {
            DownloadsActivity_.intent(this).startForResult(REQUEST_CODE_DOWNLOAD_DICTIONARY);
        }

    }

    @UiThread
    void loadDictionaries() {
        List<Dictionary> dictionaryList = new ArrayList<Dictionary>();
        for (int i = 0; i < dictionaries.getDictionaryCount(); i++) {
            dictionaryList.add(dictionaries.getDictionary(i));
        }
        listAdapter.clear();
        listAdapter.addAll(dictionaryList);
    }

    @OnActivityResult(REQUEST_CODE_ADD_DICTIONARY)
    void onActivityResultAddDictionary(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Bundle extras = data.getExtras();
        String path = extras.getString(DictionaryFileSelectorActivity.RESULT_INTENT_PATH);
        String filename = extras.getString(DictionaryFileSelectorActivity.RESULT_INTENT_FILENAME);
        if (path != null) {
            activityHelper.showProgressDialog(R.string.message_creating_index);
            addDictionary(path + "/" + filename);
        }
    }

    @OnActivityResult(REQUEST_CODE_DOWNLOAD_DICTIONARY)
    void onActivityResultDownloadDictionary(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        final String path = data.getExtras().getString(DownloadsActivity.RESULT_INTENT_PATH);
        if (path != null) {
            activityHelper.showProgressDialog(R.string.message_creating_index);
            addDictionary(path);
        }
    }

    private void addDictionary(final String path) {
        Log.d(TAG, "opening dictionary: " + path);
        final IdicInfo dicInfo = dice.open(path);
        if (dicInfo == null) {
            activityHelper.showToastLong(getResources().getString(R.string.message_item_loading_failed, path));
            activityHelper.hideProgressDialog();
            return;
        }

        if (dicInfo.readIndexBlock(Dictionary.createIndexCacheFile(this, path))) {
            for (int i = 0; i < dice.getDicNum(); i++) {
                Dictionary dictionary = new Dictionary(this, dice.getDicInfo(i));
                dictionaries.addDictionary(dictionary);
            }
            activityHelper.showToastLong(getResources().getString(R.string.message_item_added, path));
            loadDictionaries();
        } else {
            dice.close(dicInfo);
            activityHelper.showToastLong(getResources().getString(R.string.message_item_loading_failed, path));
        }
        activityHelper.hideProgressDialog();
    }

    @OptionsItem(R.id.action_add_dictionary)
    void onActionAddDictionary() {
        DictionaryFileSelectorActivity_.intent(this).startForResult(REQUEST_CODE_ADD_DICTIONARY);
    }

    @OptionsItem(R.id.action_download_dictionary)
    void onActionDownloadDictionary() {
        DownloadsActivity_.intent(this).startForResult(REQUEST_CODE_DOWNLOAD_DICTIONARY);
    }

    @ItemClick(android.R.id.list)
    void onListItemClick(int position) {
        Dictionary dictionary = listAdapter.getItem(position);
        new DictionaryContextDialogBuilder(this, dictionary)
                .setContextActionListener(this)
                .show();
    }

    @Background
    void deleteDictionary(Dictionary dictionary) {
        String path = dictionary.getPath();
        final IdicInfo dicInfo = dice.getDicInfo(path);
        if (dicInfo != null) {
            dice.close(dicInfo);
        }
        new File(dictionary.getIndexCacheFilePath(this)).delete();
        dictionaries.removeDictionary(dictionary);

        loadDictionaries();

        activityHelper.showToastLong(getResources().getString(R.string.message_item_removed, path));
    }

    @Override
    protected void onDestroy() {
        activityHelper.clear();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(item.getItemId(), item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContextActionDelete(Dictionary dictionary) {
        final Dictionary dictionaryRef = dictionary;
        activityHelper
                .buildConfirmDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteDictionary(dictionaryRef);
                    }
                })
                .setTitle(dictionary.getNameWithEmoji())
                .setMessage(R.string.message_confirm_delete)
                .setIcon(R.drawable.ic_delete_black_36dp)
                .show();
    }

    @Override
    public void onContextActionToggleEnabled(Dictionary dictionary) {
        dictionary.setEnabled(!dictionary.isEnabled());
        if (dictionaries.updateDictionary(dictionary)) {
            loadDictionaries();
        }
    }

    @Override
    public void onContextActionRename(final Dictionary dictionary) {
        activityHelper
                .buildInputDialog(
                        dictionary.getName(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                TextView textView = (TextView) ((AlertDialog) dialogInterface).findViewById(android.R.id.text1);
                                String dictionaryName = textView.getText().toString().trim();
                                if (dictionaryName.isEmpty()) {
                                    return;
                                }
                                dictionary.setName(dictionaryName);
                                dictionaries.updateDictionary(dictionary);
                            }
                        }
                )
                .setIcon(R.drawable.ic_edit_black_36dp)
                .setTitle(R.string.action_rename)
                .show();
    }

    @Override
    public void onContextActionUp(Dictionary dictionary) {
        dictionaries.swap(dictionary, -1);

        final IdicInfo dicInfo = dice.getDicInfo(dictionary.getPath());
        dice.swap(dicInfo, -1);

        loadDictionaries();
    }

    @Override
    public void onContextActionDown(Dictionary dictionary) {
        dictionaries.swap(dictionary, 1);

        final IdicInfo dicInfo = dice.getDicInfo(dictionary.getPath());
        dice.swap(dicInfo, 1);

        loadDictionaries();
    }

    private class ListAdapter extends ArrayAdapter<Dictionary> {

        ListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Dictionary item = getItem(position);

            View view = super.getView(position, convertView, parent);

            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            text1.setText(item.getNameWithEmoji());
            if (item.isEnabled()) {
                text1.setTextColor(Color.BLACK);
            } else {
                text1.setTextColor(Color.GRAY);
            }

            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            text2.setText(item.getPath());

            return view;
        }
    }
}