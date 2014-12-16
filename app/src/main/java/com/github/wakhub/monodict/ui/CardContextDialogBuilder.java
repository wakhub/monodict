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
import com.github.wakhub.monodict.db.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 5/29/14.
 */
public class CardContextDialogBuilder extends AlertDialog.Builder implements DialogInterface.OnClickListener {

    private static final String TAG = CardContextDialogBuilder.class.getSimpleName();
    private static final List<Integer> ITEM_IDS = Arrays.asList(
            R.string.action_move_into_inbox,
            R.string.action_edit,
            R.string.action_delete,
            R.string.action_search,
            R.string.action_speech,
            R.string.action_search_by_google_com,
            R.string.action_search_by_dictionary_com,
            R.string.action_search_by_alc_co_jp);

    private OnContextActionListener contextActionListener;

    private final CardDialog dialog;

    private final Card card;

    private ArrayList<String> itemLabels = new ArrayList<String>();

    public interface OnContextActionListener {
        void onContextActionMoveIntoInbox(Card card);
        void onContextActionEdit(Card card);
        void onContextActionDelete(Card card);
        void onContextActionSpeech(Card card);
        void onContextActionSearch(Card card);
    }

    public CardContextDialogBuilder(Context context, CardDialog dialog, Card card) {
        super(context);
        this.dialog = dialog;
        this.card = card;
        Resources resources = getContext().getResources();
        for (Integer id : ITEM_IDS) {
            itemLabels.add(resources.getString(id));
        }
        setTitle(card.getDisplay());
        setItems(itemLabels.toArray(new CharSequence[0]), this);
    }

    public CardContextDialogBuilder setContextActionListener(OnContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
        return this;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int id = ITEM_IDS.get(which);
        Context context = getContext();
        Resources resources = context.getResources();
        String display = card.getDisplay();

        switch (id) {
            case R.string.action_move_into_inbox:
                contextActionListener.onContextActionMoveIntoInbox(card);
                break;
            case R.string.action_edit:
                contextActionListener.onContextActionEdit(card);
                break;
            case R.string.action_delete:
                contextActionListener.onContextActionDelete(card);
                break;
            case R.string.action_speech:
                contextActionListener.onContextActionSpeech(card);
                break;
            case R.string.action_search:
                contextActionListener.onContextActionSearch(card);
                break;
            case R.string.action_search_by_google_com:
                BrowserActivity_.intent(context)
                        .extraUrlOrKeywords(resources.getString(R.string.url_google_com_search, display))
                        .start();
                break;
            case R.string.action_search_by_dictionary_com:
                BrowserActivity_.intent(context)
                        .extraUrlOrKeywords(resources.getString(R.string.url_dictionary_com_search, display))
                        .start();
                break;
            case R.string.action_search_by_alc_co_jp:
                BrowserActivity_.intent(context)
                        .extraUrlOrKeywords(resources.getString(R.string.url_alc_co_jp_search, display))
                        .start();
                break;
        }
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
    }
}
