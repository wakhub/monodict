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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.BrowserActivity_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 5/28/14.
 */
public class MainContextDialogBuilder extends AlertDialog.Builder implements DialogInterface.OnClickListener {

    private static final String TAG = MainContextDialogBuilder.class.getSimpleName();
    private static final List<Integer> ITEM_IDS = Arrays.asList(
            R.string.action_search_by_google_com,
            R.string.action_search_by_dictionary_com,
            R.string.action_search_by_alc_co_jp);

    private final String query;
    private ArrayList<String> itemLabels = new ArrayList<String>();

    public MainContextDialogBuilder(Context context, String query) {
        super(context);
        this.query = query;
        Resources resources = getContext().getResources();
        for (Integer id : ITEM_IDS) {
            itemLabels.add(resources.getString(id));
        }
        setTitle(query);
        setItems(itemLabels.toArray(new CharSequence[0]), this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int id = ITEM_IDS.get(which);
        Context context = getContext();
        Resources resources = context.getResources();
        String url = null;
        switch (id) {
            case R.string.action_search_by_google_com:
                url = resources.getString(R.string.url_google_com_search, query);
                break;
            case R.string.action_search_by_dictionary_com:
                url = resources.getString(R.string.url_dictionary_com_search, query);
                break;
            case R.string.action_search_by_alc_co_jp:
                url = resources.getString(R.string.url_alc_co_jp_search, query);
                break;
        }
        if (url != null) {
            BrowserActivity_.intent(context).extraUrlOrKeywords(url).start();
        }
    }
}
