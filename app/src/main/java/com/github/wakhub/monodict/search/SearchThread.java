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
package com.github.wakhub.monodict.search;

import android.util.Log;

import com.github.wakhub.monodict.dice.IdicResult;
import com.github.wakhub.monodict.dice.Idice;
import com.github.wakhub.monodict.ui.DicItemListView;

import java.util.ArrayList;

/**
 * Created by wak on 5/25/14.
 */
class SearchThread extends Thread {

    private final static String TAG = SearchThread.class.getSimpleName();

    interface Interface {
        void generateViewForSearch(int mode, int dic, IdicResult pr, ArrayList<DicItemListView.Data> result, int po);

        void onSearchFinished(String query, ArrayList<DicItemListView.Data> result);
    }

    private String query = null;
    private Interface delegate = null;
    private int timer = 0;
    private final Idice dice;

    public SearchThread(Interface delegate, final Idice dice, String query, int timer) {
        this.delegate = delegate;
        this.query = query;
        this.timer = timer;
        this.dice = dice;
    }

    public void run() {
        search();
    }

    private void search() {

        final ArrayList<DicItemListView.Data> result = new ArrayList<DicItemListView.Data>();
        try {
            sleep(timer);
            int dicnum = dice.getDicNum();
            for (int dic = 0; dic < dicnum; dic++) {
                if (interrupted())
                    return;

                if (!dice.isEnable(dic)) {
                    continue;
                }

                if (interrupted())
                    return;

                dice.search(dic, query);

                IdicResult pr = dice.getResult(dic);

                if (interrupted())
                    return;
                if (pr.getCount() > 0) {
                    delegate.generateViewForSearch(DictionaryService.DISP_MODE_HEADER, dic, null, result, -1);
                    delegate.generateViewForSearch(DictionaryService.DISP_MODE_RESULT, dic, pr, result, -1);
                }

                if (interrupted())
                    return;
            }

            if (result.size() == 0) {
                delegate.generateViewForSearch(DictionaryService.DISP_MODE_NORESULT, -1, null, result, -1);
            }

            if (!interrupted()) {
                delegate.onSearchFinished(query, result);
            }

        } catch (InterruptedException e) {
            Log.d(TAG, "interrupted.");
        }

    }
}
