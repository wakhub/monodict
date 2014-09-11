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
package com.github.wakhub.monodict.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.activity.settings.SettingsActivity_;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.dice.DiceFactory;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.MainActivityState;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.search.DictionaryService;
import com.github.wakhub.monodict.search.DictionaryServiceConnection;
import com.github.wakhub.monodict.ui.DicContextDialogBuilder;
import com.github.wakhub.monodict.ui.DicItemListView;
import com.github.wakhub.monodict.ui.DictionarySearchView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DimensionRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @see com.github.wakhub.monodict.activity.MainActivity_
 */
@EActivity(R.layout.activity_main)
@OptionsMenu({R.menu.main})
public class MainActivity extends Activity implements
        DicItemListView.Callback,
        DicContextDialogBuilder.OnContextActionListener,
        DictionaryService.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();

    //private static final int REQUEST_CODE = 10000;

    @App
    MonodictApp app;

    @Pref
    Preferences_ preferences;

    @ViewById
    DicItemListView dicItemListView;

    @ViewById
    TableLayout nav;

    @SystemService
    ClipboardManager clipboardManager;

    @SystemService
    SearchManager searchManager;

    @SystemService
    InputMethodManager inputMethodManager;

    @Bean
    ActivityHelper activityHelper;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    DatabaseHelper databaseHelper;

    @Bean
    MainActivityState state;

    @Bean
    SpeechHelper speechHelper;

    @Bean
    Dictionaries dictionaries;

    @DimensionRes
    float spaceRelax;

    @DimensionRes
    float spaceWell;

    private boolean queryInitialized = false;

    private String extraActionSearchQuery = null;

    private String extraActionSendText = null;

    private DictionarySearchView searchView = null;

    private int delay = 0;

    private DicItemListView.ResultAdapter resultAdapter;

    private ArrayList<DicItemListView.Data> resultData;

    private CharSequence lastClipboard = null;

    private DictionaryServiceConnection dictionaryServiceConnection;

    /**
     * Called when the activity is first created.
     */
    @AfterViews
    public void afterViews() {
        Log.d(TAG, "afterViews: state=" + state.toString());

        Resources resources = getResources();

        setTitle(String.format("%s %s", resources.getString(R.string.app_name), app.getPackageInfo().versionName));

        commonActivityTrait.initActivity(preferences);

        dicItemListView.setCallback(this);
        resultData = new ArrayList<DicItemListView.Data>();
        resultAdapter = new DicItemListView.ResultAdapter(
                this,
                R.layout.list_item_dic,
                R.id.dic_item_list_view,
                resultData);
        dicItemListView.setAdapter(resultAdapter);

        resultAdapter.notifyDataSetChanged();
    }

    void initQuery() {
        Log.d(TAG, "initQuery");
        if (queryInitialized || searchView == null || !dictionaryServiceConnection.isConnected()) {
            Log.d(TAG, "initQuery has cancelled");
            return;
        }
        String query = null;

        if (extraActionSearchQuery != null && !extraActionSearchQuery.isEmpty()) {
            query = extraActionSearchQuery;
            extraActionSearchQuery = null;
        }
        if (extraActionSendText != null && !extraActionSendText.isEmpty()) {
            query = extraActionSendText;
            extraActionSendText = null;
        }

        if (query == null && preferences.clipboardSearch().get()) {
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                String clipText = clipData.getItemAt(0).getText().toString();
                if (lastClipboard == null || !clipText.equals(lastClipboard)) {
                    query = clipText;
                    lastClipboard = clipText;
                }
            }
        }
        if (query != null) {
            query = query.trim();
            searchView.setQuery(query, true);
            searchView.setSelected(true);
        } else {
            String lastSearchQuery = state.getLastSearchQuery();
            if (lastSearchQuery != null && !lastSearchQuery.isEmpty()) {
                searchView.setQuery(lastSearchQuery, true);
            }
        }
        queryInitialized = true;
    }

    @Background
    void search(String text, int timer) {
        resultAdapter.setHighlightKeyword(text);
        dictionaryServiceConnection.search(text);
    }

    @Click(R.id.flashcard_button)
    void onClickFlashcardButton() {
        FlashcardActivity_.intent(this).start();
    }

    @Click(R.id.browser_button)
    void onClickBrowserButton() {
        BrowserActivity_.intent(this).start();
    }

    @Click(R.id.settings_button)
    void onClickSettingsButton() {
        SettingsActivity_.intent(this).start();
    }

    @UiThread
    void setNavVisibility(boolean visible) {
        if (visible) {
            if (inputMethodManager != null && getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
            nav.setVisibility(View.VISIBLE);
        } else {
            nav.setVisibility(View.GONE);
        }
    }

    @OptionsItem({
            R.id.action_search_by_google_com,
            R.id.action_search_by_alc_co_jp,
            R.id.action_search_by_dictionary_com})
    void onActionSearchByWeb(MenuItem item) {
        String url = null;
        String query = searchView.getQuery().toString().trim();
        Resources resources = getResources();
        switch (item.getItemId()) {
            case R.id.action_search_by_google_com:
                url = resources.getString(R.string.url_google_com_search, query);
                break;
            case R.id.action_search_by_dictionary_com:
                url = resources.getString(R.string.url_dictionary_com_search, query);
                break;
            case R.id.action_search_by_alc_co_jp:
                url = resources.getString(R.string.url_alc_co_jp_search, query);
                break;
        }
        if (url == null) {
            return;
        }
        BrowserActivity_.intent(this).extraUrlOrKeywords(url).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (speechHelper != null && speechHelper.isProcessing()) {
            return;
        }
        Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEARCH)) {
            Log.d(TAG, "Intent.ACTION_SEARCH: " + SearchManager.QUERY);
            extraActionSearchQuery = intent.getExtras().getString(SearchManager.QUERY);
        }
        if (action.equals(Intent.ACTION_SEND)) {
            Log.d(TAG, "Intent.ACTION_SEND: " + Intent.EXTRA_TEXT);
            extraActionSendText = intent.getExtras().getString(Intent.EXTRA_TEXT);
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        if (dictionaryServiceConnection == null) {
            dictionaryServiceConnection = new DictionaryServiceConnection(this);
            activityHelper.showProgressDialog(R.string.message_loading_dictionaries);
        }

        bindService(
                new Intent(this, DictionaryService.class),
                dictionaryServiceConnection,
                Context.BIND_AUTO_CREATE);

        dicItemListView.setFastScrollEnabled(preferences.fastScroll().get());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uriData = intent.getData();
        if (uriData != null) {
            Log.d(TAG, "uriData: " + uriData.toString());
            if (uriData.getScheme().equals("monodict") && uriData.getHost().equals("search")) {
                List<String> paths = uriData.getPathSegments();
                if (paths.size() > 0) {
                    searchView.setQuery(paths.get(0), true);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        commonActivityTrait.setOrientation(preferences.orientation().get());

        nav.requestLayout();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        speechHelper.finish();

        queryInitialized = false;
        unbindService(dictionaryServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        searchView = new DictionarySearchView(this, new DictionarySearchView.Listener() {
            @Override
            public void onSearchViewFocusChange(boolean b) {
                setNavVisibility(!b);
            }

            @Override
            public void onSearchViewQueryTextSubmit(String query) {
                search(query, delay);
            }

            @Override
            public void onSearchViewQueryTextChange(String s) {
                String text = DiceFactory.convert(s);
                String lastSearchQuery = state.getLastSearchQuery();
                if (text.length() > 0 && !lastSearchQuery.equals(text)) {
                    int timer = delay;
                    if (lastSearchQuery.length() > 0 &&
                            lastSearchQuery.charAt(lastSearchQuery.length() - 1) != text.charAt(text.length() - 1)) {
                        timer = 10;
                    }
                    search(text, timer);
                }
            }
        });

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        RelativeLayout wrapSearchView = new RelativeLayout(this);
        wrapSearchView.addView(searchView);
        actionBar.setCustomView(wrapSearchView);

        Configuration configuration = getResources().getConfiguration();

        int screenSize = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL
                || screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        initQuery();

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SpeechHelper.REQUEST_CODE_TTS) {
            speechHelper.onActivityResult(requestCode, resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDicviewItemClickAddToFlashcardButton(int position) {
        final DicItemListView.Data data = resultAdapter.getItem(position);
        addFlashcard(data);
    }

    @Override
    public void onDicviewItemClickSpeechButton(int position) {
        final DicItemListView.Data data = resultAdapter.getItem(position);
        speechHelper.speech(data.Index.toString());
    }

    @Override
    public void onDicviewItemClickActionButton(int position) {
        final DicItemListView.Data data = resultAdapter.getItem(position);
        new DicContextDialogBuilder(this, data).setContextActionListener(this).show();
    }

    @Override
    public void onDicviewItemActionModeSearch(String selectedText) {
        searchView.setQuery(selectedText, true);
    }

    @Override
    public void onDicviewItemActionModeSpeech(String selectedText) {
        speechHelper.speech(selectedText);
    }

    void addFlashcard(DicItemListView.Data data) {
        Card card;
        try {
            Card duplicate = databaseHelper.getCardByDisplay(data.Index.toString());
            if (duplicate != null) {
                activityHelper.onDuplicatedCardFound(duplicate,
                        data.Trans.toString(),
                        dictionaries.getDictionary(data.getDic()).getName());
                return;
            }
            card = databaseHelper.createCard(data);
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
        Resources resources = getResources();
        String message = resources.getString(R.string.message_item_added_to,
                card.getDisplay(),
                resources.getString(R.string.title_activity_flashcard));

        activityHelper.showToast(message);
    }

    @Override
    public void onContextActionAddToFlashcard(DicItemListView.Data data) {
        addFlashcard(data);
    }

    @Override
    public void onContextActionShare(DicItemListView.Data data) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, data.toSummaryString());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            activityHelper.showError(e);
        }
    }

    @Override
    public void onContextActionCopyAll(DicItemListView.Data data) {
        Log.d(TAG, "onContextActionCopyAll");
        clipboardManager.setPrimaryClip(ClipData.newPlainText("all", data.toSummaryString()));
        activityHelper.showToast(R.string.message_success);
    }

    private static void removeDirectory(File path) {
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    removeDirectory(file);
                }
                file.delete();
            }
        }
    }

    @Override
    @UiThread
    public void onDictionaryServiceInitialized() {
        activityHelper.hideProgressDialog();
        int lastVersionCode = preferences.lastVersionCode().getOr(0);
        int currentVersionCode = app.getPackageInfo().versionCode;
        if (currentVersionCode > lastVersionCode) {
            preferences.lastVersionCode().put(currentVersionCode);
            removeDirectory(getCacheDir());

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.title_welcome)
                    .setMessage(activityHelper.getStringFromRaw(R.raw.welcome))
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(R.string.action_download_now, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            SettingsActivity_.intent(MainActivity.this)
                                    .extraOpenDownloads(true)
                                    .start();
                        }
                    })
                    .show();
        }
        initQuery();
    }

    @Override
    public void onDictionaryServiceUpdateDictionaries() {
        dictionaryServiceConnection.search(searchView.getQuery().toString());
    }

    @Override
    @UiThread
    public void onDictionaryServiceResult(String query, ArrayList<DicItemListView.Data> result) {
        resultData.clear();
        resultData.addAll(result);
        resultAdapter.notifyDataSetChanged();
        if (!speechHelper.isProcessing()) {
            dicItemListView.setSelection(0);
        }

        if (result.size() < 1) {
            return;
        }
        state.setLastSearchQuery(query);
    }
}
