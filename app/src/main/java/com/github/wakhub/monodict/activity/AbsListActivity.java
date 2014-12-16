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

import android.support.v7.app.ActionBarActivity;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

/**
 * ListView for AppCompat
 *
 * Created by wak on 12/15/14.
 */
@EActivity
public abstract class AbsListActivity extends ActionBarActivity {

    @AfterViews
    protected void afterViewAbsListActivity() {
        ListView listView = getListView();
        if (listView != null && listView.getEmptyView() == null) {
            listView.setEmptyView(findViewById(android.R.id.empty));
        }
    }

    public ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    public void setListAdapter(ListAdapter listAdapter) {
        ListView listView = getListView();
        if (listView != null) {
            listView.setAdapter(listAdapter);
        }
    }

    public ListAdapter getListAdapter() {
        ListView listView = getListView();
        if (listView != null) {
            return listView.getAdapter();
        }
        return null;
    }
}
