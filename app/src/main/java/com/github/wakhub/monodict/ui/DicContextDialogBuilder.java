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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;

import com.github.wakhub.monodict.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 5/25/14.
 */
public class DicContextDialogBuilder extends AlertDialog.Builder implements DialogInterface.OnClickListener {

    private static final String TAG = DicContextDialogBuilder.class.getSimpleName();
    private static final List<Integer> ITEM_IDS = Arrays.asList(
            R.string.action_copy_word,
            R.string.action_copy_all,
            R.string.action_share);

    private OnContextActionListener contextActionListener;
    private DicItemListView.Data data;
    private ArrayList<String> itemLabels = new ArrayList<String>();

    public interface OnContextActionListener {
        void onContextActionShare(DicItemListView.Data data);

        void onContextActionCopyWord(DicItemListView.Data data);

        void onContextActionCopyAll(DicItemListView.Data data);
    }

    public DicContextDialogBuilder(Context context, DicItemListView.Data data) {
        super(context);
        this.data = data;
        Resources resources = getContext().getResources();
        for (Integer id : ITEM_IDS) {
            itemLabels.add(resources.getString(id));
        }
        setTitle(data.Index.toString());
        setItems(itemLabels.toArray(new CharSequence[0]), this);
    }

    public DicContextDialogBuilder setContextActionListener(OnContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
        return this;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int id = ITEM_IDS.get(which);
        switch (id) {
            case R.string.action_copy_word:
                contextActionListener.onContextActionCopyWord(data);
                break;
            case R.string.action_copy_all:
                contextActionListener.onContextActionCopyAll(data);
                break;
            case R.string.action_share:
                contextActionListener.onContextActionShare(data);
                break;
        }
    }
}
