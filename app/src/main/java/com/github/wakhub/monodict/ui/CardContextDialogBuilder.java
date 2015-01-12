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
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.BrowserActivity_;
import com.github.wakhub.monodict.db.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 5/29/14.
 */
public class CardContextDialogBuilder extends MaterialDialog.Builder implements MaterialDialog.ListCallback {

    private static final String TAG = CardContextDialogBuilder.class.getSimpleName();

    private static final List<Integer> ALL_ITEM_IDS = Arrays.asList(
            R.string.action_speech,
            R.string.action_edit,
            R.string.action_delete,
            R.string.action_move_into_inbox,
            R.string.action_search,
            R.string.action_search_by_google_com,
            R.string.action_search_by_dictionary_com,
            R.string.action_search_by_alc_co_jp);

    private CardContextActionListener contextActionListener;

    private final Card card;

    private ArrayList<Integer> itemIds = new ArrayList<>();

    private final Context context;

    public CardContextDialogBuilder(Context context, Card card, int[] ignoredStringIds) {
        super(context);
        this.context = context;
        this.card = card;
        Resources resources = context.getResources();
        for (Integer id : ALL_ITEM_IDS) {
            boolean ignored = false;
            for (int ignoredId : ignoredStringIds) {
                if (id == ignoredId) {
                    ignored = true;
                }
            }
            if (!ignored) {
                itemIds.add(id);
            }
        }
        title(card.getDisplay());
        icon(R.drawable.ic_flashcard_black_36dp);
        String[] itemLabels = new String[itemIds.size()];
        for (int i = 0; i < itemIds.size(); i++) {
            itemLabels[i] = resources.getString(itemIds.get(i));
        }
        items(itemLabels);
        itemsCallback(this);
    }

    public CardContextDialogBuilder setContextActionListener(CardContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
        return this;
    }

    @Override
    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
        int id = itemIds.get(i);
        Resources resources = context.getResources();
        String display = card.getDisplay();

        switch (id) {
            case R.string.action_speech:
                contextActionListener.onCardContextActionSpeech(card);
                break;
            case R.string.action_edit:
                contextActionListener.onCardContextActionEdit(card);
                break;
            case R.string.action_delete:
                contextActionListener.onCardContextActionDelete(card);
                break;
            case R.string.action_move_into_inbox:
                contextActionListener.onCardContextActionMoveIntoInbox(card);
                break;
            case R.string.action_search:
                contextActionListener.onCardContextActionSearch(card);
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
    }
}
