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

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.Model;
import com.github.wakhub.monodict.dice.DiceFactory;
import com.github.wakhub.monodict.dice.IdicInfo;
import com.github.wakhub.monodict.dice.Idice;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.preferences.MainActivityState;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.search.DictionaryService;
import com.github.wakhub.monodict.search.DictionaryServiceConnection;
import com.github.wakhub.monodict.ui.DicContextDialogBuilder;
import com.github.wakhub.monodict.ui.DicItemListView;
import com.github.wakhub.monodict.ui.DictionaryContextDialogBuilder;
import com.github.wakhub.monodict.ui.DictionarySearchView;
import com.github.wakhub.monodict.utils.ViewUtils;
import com.google.common.eventbus.Subscribe;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
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
//@OptionsMenu({R.menu.main})
public class MainActivity extends ActionBarActivity implements
        MainActivityRootLayout.Listener,
        MainActivityDrawerListAdapter.Listener,
        MainActivityDrawerListAdapter.DataSource,
        DicItemListView.Callback,
        DicItemListView.ResultAdapter.DictionaryDataSource,
        DicContextDialogBuilder.OnContextActionListener,
        DictionaryService.Listener,
        DictionaryContextDialogBuilder.OnContextActionListener,
        DictionarySearchView.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_DOWNLOAD_DICTIONARY = 10000;

    private static final int REQUEST_CODE_SELECT_LOCAL_DICTIONARY = 10001;

    @App
    MonodictApp app;

    @Pref
    Preferences_ preferences;

    @ViewById
    MainActivityRootLayout rootLayout;

    @ViewById
    Toolbar toolbar;

    @ViewById
    DictionarySearchView searchView;

    @ViewById
    ListView drawerList;

    @ViewById
    DicItemListView dicItemListView;

    @ViewById
    FloatingActionButton searchButton;

    @SystemService
    ClipboardManager clipboardManager;

    @SystemService
    SearchManager searchManager;

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

        commonActivityTrait.initActivity(preferences);

        rootLayout.setListener(this);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, rootLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerToggle.setDrawerIndicatorEnabled(true);
        rootLayout.setDrawerListener(drawerToggle);
        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp);
        drawerList.setAdapter(new MainActivityDrawerListAdapter(this, this, this));

        searchView.setListener(this);
        searchView.onActionViewExpanded();

        dicItemListView.setCallback(this);
        resultData = new ArrayList<>();
        resultAdapter = new DicItemListView.ResultAdapter(
                this,
                R.layout.list_item_dic,
                R.id.dic_item_list_view,
                resultData);
        resultAdapter.setDictionaryDataSource(this);
        dicItemListView.setAdapter(resultAdapter);
        resultAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onEvent(DictionaryService.DictionaryDeletedEvent event) {
        Log.d(TAG, "DictionaryDeleted: " + event.getDictionary());
        dictionaries.removeDictionary(event.getDictionary());
        reloadDictionaries();
    }

    @Subscribe
    public void onEvent(DictionaryService.DictionarySwappedEvent event) {
        Log.d(TAG, "DictionarySwapped: " + event.getDictionary());
        dictionaries.swap(event.getDictionary(), event.getDirection());
        reloadDictionaries();
    }

    @Subscribe
    public void onEvent(Model.ModelChangeRequestEvent event) {
        Log.d(TAG, "onEvent: " + event);
        Card card = (Card) event.getModel();
        if (event.getType().equals(Model.ModelChangeRequestEvent.TYPE_UPDATE)) {
            try {
                databaseHelper.updateCard(card);
            } catch (SQLException e) {
                activityHelper.showError(e);
                return;
            }
            SnackbarManager.show(
                    createSnackbar().text(
                            getResources().getString(
                                    R.string.message_item_modified,
                                    card.getShortDisplay())),
                    this);
        }
    }

    void reloadDictionaries() {
        dictionaries.reload();
        ((MainActivityDrawerListAdapter) drawerList.getAdapter()).reload();
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

    @Click(R.id.search_button)
    void onClickSearchButton() {
        searchView.clear();
        searchView.focus();
    }

    private void startActivityFromNav(View view, Intent intent) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                view,
                0,
                0,
                view.getWidth(),
                view.getHeight());
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    private void showNavButtons(boolean animate) {
        if (rootLayout.isSoftKeyboardShown()) {
            return;
        }
        searchButton.show(animate);
    }

    private void hideNavButtons(boolean animate) {
        searchButton.hide(animate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (speechHelper != null && speechHelper.isProcessing()) {
            return;
        }
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEARCH)) {
            String query = intent.getExtras().getString(SearchManager.QUERY);
            Log.d(TAG, "Intent.ACTION_SEARCH: " + query);
            extraActionSearchQuery = query;
        }
        if (action.equals(Intent.ACTION_SEND)) {
            String query = intent.getExtras().getString(Intent.EXTRA_TEXT);
            Log.d(TAG, "Intent.ACTION_SEND: " + query);
            extraActionSendText = query;
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
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

        if (dictionaryServiceConnection == null) {
            dictionaryServiceConnection = new DictionaryServiceConnection();
            dictionaryServiceConnection.setListener(this);
            activityHelper.showProgressDialog(R.string.message_loading_dictionaries);
        }

        bindService(
                new Intent(this, DictionaryService.class),
                dictionaryServiceConnection,
                Context.BIND_AUTO_CREATE);

        MonodictApp.getEventBus().register(this);
        commonActivityTrait.setOrientation(preferences.orientation().get());
        reloadDictionaries();

        initQuery();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        MonodictApp.getEventBus().unregister(this);
        unbindService(dictionaryServiceConnection);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        speechHelper.finish();

        queryInitialized = false;
//        unbindService(dictionaryServiceConnection);

        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Log.d(TAG, "Cancel ACTION_SEND");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (rootLayout.isDrawerOpen(drawerList)) {
            rootLayout.closeDrawer(drawerList);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (commonActivityTrait.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    public Dictionary getDictionaryForDicItemListAdapter(int index) {
        return dictionaries.getDictionary(index);
    }

    private Snackbar createSnackbar() {
        return Snackbar.with(getApplicationContext())
                .swipeToDismiss(true)
                .eventListener(new EventListener() {
                    @Override
                    public void onShow(Snackbar snackbar) {
                        int snackbarHeight = snackbar.getHeight();
                        ViewUtils.addMarginBottom(searchButton, snackbarHeight);
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        snackbar.animation(false);
                    }

                    @Override
                    public void onDismiss(Snackbar snackbar) {
                        int snackbarHeight = snackbar.getHeight();
                        ViewUtils.addMarginBottom(searchButton, -snackbarHeight);
                    }

                    @Override
                    public void onDismissed(Snackbar snackbar) {
                    }
                });
    }

    private void addFlashcard(DicItemListView.Data data) {
        final Card card;
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
                card.getShortDisplay(),
                resources.getString(R.string.title_activity_flashcard));

        SnackbarManager.show(
                createSnackbar()
                        .text(message)
                        .type(SnackbarType.MULTI_LINE)
                        .actionLabel("Undo")
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                String message = getResources().getString(
                                        R.string.message_item_removed,
                                        card.getShortDisplay());
                                try {
                                    databaseHelper.deleteCard(card);
                                } catch (SQLException e) {
                                    activityHelper.showError(e);
                                    return;
                                }
                                SnackbarManager.show(
                                        createSnackbar()
                                                .text(message)
                                                .type(SnackbarType.MULTI_LINE)
                                                .duration(Snackbar.SnackbarDuration.LENGTH_SHORT),
                                        MainActivity.this);
                            }
                        })
                ,
                this);
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
        SnackbarManager.show(createSnackbar().text(R.string.message_copy_succeeded), this);
    }

    private static void removeDirectory(File path) {
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    removeDirectory(file);
                }
                if (file.delete()) {
                    Log.d(TAG, String.format("Failed to remove directory: %s", file.getPath()));
                }
            }
        }
    }

    @Override
    @UiThread
    public void onDictionaryServiceInitialized() {
        activityHelper.hideProgressDialog();
        int lastVersionCode = preferences.lastVersionCode().getOr(0);
        int currentVersionCode = MonodictApp.getPackageInfo(this).versionCode;
        if (currentVersionCode > lastVersionCode) {
            preferences.lastVersionCode().put(currentVersionCode);
            removeDirectory(getCacheDir());

            new MaterialDialog.Builder(this)
                    .title(R.string.title_welcome)
                    .content(activityHelper.getStringFromRaw(R.raw.welcome))
                    .positiveText(R.string.action_download_now)
                    .callback(new MaterialDialog.SimpleCallback() {
                        @Override
                        public void onPositive(MaterialDialog materialDialog) {
                            materialDialog.dismiss();
                            DownloadsActivity_.intent(MainActivity.this)
                                    .startForResult(REQUEST_CODE_DOWNLOAD_DICTIONARY);
                        }
                    })
                    .negativeText(android.R.string.cancel)
                    .cancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
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

    @Override
    public void onDictionaryServiceError(String query, Exception e) {
        activityHelper.showError(e);
    }

    @Override
    public void onSoftKeyboardShown(boolean isShowing) {
        if (isShowing) {
            hideNavButtons(false);
        } else {
            showNavButtons(false);
        }
    }

    /*
    @Override
    public void onContextActionDelete(final Dictionary dictionary) {

    }

    @Override
    public void onContextActionToggleEnabled(Dictionary dictionary) {

    }

    @Override
    public void onContextActionRename(final Dictionary dictionary) {

    }
    */

    @Override
    public void onContextActionMoreDetail(Dictionary dictionary) {
        DictionaryActivity_.intent(this)
                .extraDictionaryPath(dictionary.getPath())
                .start();
    }

    @Override
    public void onContextActionUp(Dictionary dictionary) {
        dictionaryServiceConnection.swap(dictionary, -1);
    }

    @Override
    public void onContextActionDown(Dictionary dictionary) {
        dictionaryServiceConnection.swap(dictionary, 1);
    }

    @Override
    public void onDrawerClickDictionaryItem(Dictionary dictionary) {
        Log.d(TAG, "onDrawerClickDictionaryItem: " + dictionary);
        new DictionaryContextDialogBuilder(this, dictionary)
                .setContextActionListener(this)
                .show();
    }

    @Override
    public void onDrawerChangeDictionaryItemCheckbox(Dictionary dictionary, boolean isChecked) {
        Log.d(TAG, String.format("onDrawerChangeDictionaryItemCheckbox: %s %s", dictionary, isChecked ? "Y" : "N"));
        dictionary.setEnabled(isChecked);
        if (dictionaries.updateDictionary(dictionary)) {
            dictionaryServiceConnection.reload();
            reloadDictionaries();
        }
    }

    @Override
    public void onDrawerClickDownloadButton() {
        Log.d(TAG, "onDrawerClickDownloadButton");
        DownloadsActivity_.intent(this).startForResult(REQUEST_CODE_DOWNLOAD_DICTIONARY);
    }

    @Override
    public Dictionaries getDictionariesForDrawer() {
        return dictionaries;
    }

    @OnActivityResult(REQUEST_CODE_DOWNLOAD_DICTIONARY)
    void onActivityResultDownloadDictionary(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        String path = data.getExtras().getString(DownloadsActivity.RESULT_INTENT_PATH);
        if (path != null) {
            activityHelper.showProgressDialog(R.string.message_creating_index);
            addDictionary(path);
        }
    }

    @Override
    public void onDrawerClickAddLocalPdicButton() {
        Log.d(TAG, "onDrawerClickAddLocalPdicButton");
        DictionaryFileSelectorActivity_.intent(this).startForResult(REQUEST_CODE_SELECT_LOCAL_DICTIONARY);
    }

    @OnActivityResult(REQUEST_CODE_SELECT_LOCAL_DICTIONARY)
    void onActivityResultSelectLocalDictionary(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        String path = data.getStringExtra(DownloadsActivity.RESULT_INTENT_PATH)
                + "/" + data.getStringExtra(DownloadsActivity.RESULT_INTENT_FILENAME);
        activityHelper.showProgressDialog(R.string.message_creating_index);
        addDictionary(path);
    }

    @Override
    public void onDrawerClickSettingsButton() {
        Log.d(TAG, "onDrawerClickSettingsButton");
        SettingsActivity_.intent(this).start();
    }

    @Override
    public void onDrawerClickBrowserButton() {
        BrowserActivity_.intent(this).start();
    }

    @Override
    public void onDrawerClickFlashcardsButton() {
        FlashcardActivity_.intent(this).start();
    }


    private void addDictionary(final String path) {
        Log.d(TAG, "addDictionary: " + path);
        Idice dice = DiceFactory.getInstance();
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
            reloadDictionaries();
        } else {
            dice.close(dicInfo);
            activityHelper.showToastLong(getResources().getString(R.string.message_item_loading_failed, path));
        }
        activityHelper.hideProgressDialog();
    }

    @Override
    public void onSearchViewFocusChange(boolean b) {
        //
    }

    @Override
    public void onSearchViewQueryTextSubmit(String query) {
        search(query, 0);
    }

    @Override
    public void onSearchViewQueryTextChange(String s) {
        String text = DiceFactory.convert(s);
        String lastSearchQuery = state.getLastSearchQuery();
        if (text.length() > 0 && !lastSearchQuery.equals(text)) {
            int delay = 0;
            if (lastSearchQuery.length() > 0 &&
                    lastSearchQuery.charAt(lastSearchQuery.length() - 1) != text.charAt(text.length() - 1)) {
                delay = 10;
            }
            search(text, delay);
        }
    }
}
