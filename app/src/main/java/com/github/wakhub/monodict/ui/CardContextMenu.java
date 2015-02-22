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
import com.github.wakhub.monodict.db.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 5/29/14.
 */
public class CardContextMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = CardContextMenu.class.getSimpleName();

    private static final List<Integer> ALL_ITEM_IDS = Arrays.asList(
            R.string.action_speech,
            R.string.action_edit,
            R.string.action_delete,
            R.string.action_move_into_inbox,
            R.string.action_search);

    private CardContextActionListener contextActionListener;

    private final Card card;

    private ArrayList<Integer> itemIds = new ArrayList<>();

    private final Context context;

    public CardContextMenu(Context context, View anchor, Card card, int[] ignoredStringIds) {
        super(context, anchor);
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
        Menu menu  = getMenu();
        for (int i = 0; i < itemIds.size(); i++) {
            menu.add(resources.getString(itemIds.get(i)));
        }
        setOnMenuItemClickListener(this);
    }

    public CardContextMenu setContextActionListener(CardContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
        return this;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (contextActionListener == null)  {
            return false;
        }
        CharSequence title = item.getTitle();
        Resources resources = context.getResources();
        if (title.equals(resources.getString(R.string.action_speech))) {
            contextActionListener.onCardContextActionSpeech(card);
            return true;
        }
        if (title.equals(resources.getString(R.string.action_edit))) {
            contextActionListener.onCardContextActionEdit(card);
            return true;
        }
        if (title.equals(resources.getString(R.string.action_delete))) {
            contextActionListener.onCardContextActionDelete(card);
            return true;
        }
        if (title.equals(resources.getString(R.string.action_move_into_inbox))) {
            contextActionListener.onCardContextActionMoveIntoInbox(card);
            return true;
        }
        if (title.equals(resources.getString(R.string.action_search))) {
            contextActionListener.onCardContextActionSearch(card);
            return true;
        }
        return false;
    }
}
