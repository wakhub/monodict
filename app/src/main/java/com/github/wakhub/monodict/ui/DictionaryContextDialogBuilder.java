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
package com.github.wakhub.monodict.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.Dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 6/10/14.
 */
public class DictionaryContextDialogBuilder extends MaterialDialog.Builder implements MaterialDialog.ListCallback {

    private static final String TAG = DictionaryContextDialogBuilder.class.getSimpleName();

    private static final List<Integer> ITEM_IDS = Arrays.asList(
            R.string.action_more_detail,
            R.string.action_up,
            R.string.action_down);

    private OnContextActionListener contextActionListener;

    private ArrayList<String> itemLabels = new ArrayList<String>();

    private final Dictionary dictionary;

    public DictionaryContextDialogBuilder(Context context, Dictionary dictionary) {
        super(context);
        this.dictionary = dictionary;
        Resources resources = context.getResources();
        for (Integer id : ITEM_IDS) {
            if (id == R.string.action_enable && dictionary.isEnabled()) {
                id = R.string.action_disable;
            }
            itemLabels.add(resources.getString(id));
        }
        iconRes(R.drawable.ic_dictionary_black_36dp);
        title(dictionary.getName());
        items(itemLabels.toArray(new CharSequence[0]));
        itemsCallback(this);
        cancelable(true);
    }

    public DictionaryContextDialogBuilder setContextActionListener(OnContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
        return this;
    }

    @Override
    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
        int id = ITEM_IDS.get(i);
        Log.d(TAG, "click" + i);
        switch (id) {
            case R.string.action_more_detail:
                contextActionListener.onContextActionMoreDetail(dictionary);
                break;
            case R.string.action_up:
                contextActionListener.onContextActionUp(dictionary);
                break;
            case R.string.action_down:
                contextActionListener.onContextActionDown(dictionary);
                break;
        }
    }

    public interface OnContextActionListener {
        void onContextActionMoreDetail(Dictionary dictionary);

        void onContextActionUp(Dictionary dictionary);

        void onContextActionDown(Dictionary dictionary);
    }
}
