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

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.activity.settings.SettingsActivity_;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.Model;
import com.github.wakhub.monodict.dice.DiceFactory;
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
import com.github.wakhub.monodict.utils.StringUtils;
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
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DimensionRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @see com.github.wakhub.monodict.activity.MainActivity_
 */
@EActivity(R.layout.activity_main)
@OptionsMenu({R.menu.main})
public class MainActivity extends ActionBarActivity implements
        MainActivityRootLayout.Listener,
        DicItemListView.Callback,
        DicContextDialogBuilder.OnContextActionListener,
        DictionaryService.Listener,
        DictionaryContextDialogBuilder.OnContextActionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    //private static final int REQUEST_CODE = 10000;

    @App
    MonodictApp app;

    @Pref
    Preferences_ preferences;

    @ViewById
    MainActivityRootLayout rootLayout;

    @ViewById
    DicItemListView dicItemListView;

    @ViewById
    ListView drawerList;

    @ViewById
    FloatingActionButton browserButton;

    @ViewById
    FloatingActionButton flashcardButton;

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

    private DictionarySearchView searchView = null;

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

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.main)));

        final Resources resources = getResources();

        setTitle(String.format("%s %s", resources.getString(R.string.app_name), MonodictApp.getPackageInfo(this).versionName));

        commonActivityTrait.initActivity(preferences);

        rootLayout.setListener(this);

        dicItemListView.setCallback(this);
        resultData = new ArrayList<>();
        resultAdapter = new DicItemListView.ResultAdapter(
                this,
                R.layout.list_item_dic,
                R.id.dic_item_list_view,
                resultData);
        dicItemListView.setAdapter(resultAdapter);
        resultAdapter.notifyDataSetChanged();

        final MainActivity activity = this;
        drawerList.setAdapter(new MainActivityDrawerListAdapter(this));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivityDrawerListAdapter.Item item =
                        (MainActivityDrawerListAdapter.Item) drawerList.getItemAtPosition(position);
                MainActivityDrawerListAdapter.ItemType itemType = item.getType();
                if (itemType.equals(MainActivityDrawerListAdapter.ItemType.DICTIONARY)) {
                    Dictionary dictionary =
                            ((MainActivityDrawerListAdapter.DictionaryItem) item).getDictionary();
                    new DictionaryContextDialogBuilder(activity, dictionary)
                            .setContextActionListener(activity)
                            .show();
                }
                if (itemType.equals(MainActivityDrawerListAdapter.ItemType.SETTINGS)) {
                    SettingsActivity_.intent(activity).start();
                    rootLayout.closeDrawer(drawerList);
                }
            }
        });
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
            SnackbarManager.show(createSnackbar().text(
                    getResources().getString(R.string.message_item_modified, card.getDisplay())), this);
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

    @Click(R.id.flashcard_button)
    void onClickFlashcardButton(View view) {
        startActivityFromNav(view, FlashcardActivity_.intent(this).get());
    }

    @Click(R.id.browser_button)
    void onClickBrowserButton(View view) {
        startActivityFromNav(view, BrowserActivity_.intent(this).get());
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

    private void showNavButtons(boolean animate) {
        if (rootLayout.isSoftKeyboardShown()) {
            return;
        }
        browserButton.show(animate);
        flashcardButton.show(animate);
        searchButton.show(animate);
    }

    private void hideNavButtons(boolean animate) {
        browserButton.hide(animate);
        flashcardButton.hide(animate);
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
        MonodictApp.getEventBus().register(this);
        commonActivityTrait.setOrientation(preferences.orientation().get());
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        MonodictApp.getEventBus().unregister(this);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        speechHelper.finish();

        queryInitialized = false;
        unbindService(dictionaryServiceConnection);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final WeakReference<MainActivity> activityRef = new WeakReference<>(this);

        searchView = new DictionarySearchView(this, new DictionarySearchView.Listener() {
            @Override
            public void onSearchViewFocusChange(boolean b) {
                // TODO: can be removed?
            }

            @Override
            public void onSearchViewQueryTextSubmit(String query) {
                if (activityRef.get() != null) {
                    activityRef.get().search(query, 0);
                }
            }

            @Override
            public void onSearchViewQueryTextChange(String s) {
                String text = DiceFactory.convert(s);
                String lastSearchQuery = "";
                if (activityRef.get() != null) {
                    lastSearchQuery = activityRef.get().state.getLastSearchQuery();
                }
                if (text.length() > 0 && !lastSearchQuery.equals(text)) {
                    int delay = 0;
                    if (lastSearchQuery.length() > 0 &&
                            lastSearchQuery.charAt(lastSearchQuery.length() - 1) != text.charAt(text.length() - 1)) {
                        delay = 10;
                    }
                    if (activityRef.get() != null) {
                        activityRef.get().search(text, delay);
                    }
                }
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        RelativeLayout wrapSearchView = new RelativeLayout(this);
        wrapSearchView.addView(searchView);
        actionBar.setCustomView(wrapSearchView);

        /*
        Configuration configuration = getResources().getConfiguration();

        int screenSize = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL
                || screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        */

        initQuery();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(item.getItemId(), item)) {
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

    private Snackbar createSnackbar() {
        return Snackbar.with(getApplicationContext())
                .swipeToDismiss(true)
                .eventListener(new EventListener() {
                    @Override
                    public void onShow(Snackbar snackbar) {
                        int snackbarHeight = snackbar.getHeight();
                        ViewUtils.addMarginBottom(browserButton, snackbarHeight);
                        ViewUtils.addMarginBottom(flashcardButton, snackbarHeight);
                        ViewUtils.addMarginBottom(searchButton, snackbarHeight);
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        snackbar.animation(false);
                    }

                    @Override
                    public void onDismiss(Snackbar snackbar) {
                        int snackbarHeight = snackbar.getHeight();
                        ViewUtils.addMarginBottom(browserButton, -snackbarHeight);
                        ViewUtils.addMarginBottom(flashcardButton, -snackbarHeight);
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
        final String shortText = StringUtils.ellipse(card.getDisplay(), 10);
        String message = resources.getString(R.string.message_item_added_to,
                shortText,
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
                                        shortText);
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

    @Override
    public void onContextActionDelete(final Dictionary dictionary) {
        activityHelper
                .buildConfirmDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dictionaryServiceConnection.deleteDictionary(dictionary);
                    }
                })
                .setTitle(dictionary.getNameWithEmoji())
                .setMessage(R.string.message_confirm_delete)
                .setIcon(R.drawable.ic_delete_black_36dp)
                .show();
    }

    @Override
    public void onContextActionToggleEnabled(Dictionary dictionary) {
        dictionary.setEnabled(!dictionary.isEnabled());
        if (dictionaries.updateDictionary(dictionary)) {
            dictionaryServiceConnection.reload();
            reloadDictionaries();
        }
    }

    @Override
    public void onContextActionRename(final Dictionary dictionary) {
        activityHelper
                .buildInputDialog(
                        dictionary.getName(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                TextView textView = (TextView) ((AlertDialog) dialogInterface).findViewById(android.R.id.text1);
                                String dictionaryName = textView.getText().toString().trim();
                                if (dictionaryName.isEmpty()) {
                                    return;
                                }
                                dictionary.setName(dictionaryName);
                                dictionaries.updateDictionary(dictionary);
                                dictionaryServiceConnection.reload();
                                reloadDictionaries();
                            }
                        }
                )
                .setIcon(R.drawable.ic_edit_black_36dp)
                .setTitle(R.string.action_rename)
                .show();
    }

    @Override
    public void onContextActionUp(Dictionary dictionary) {
        dictionaryServiceConnection.swap(dictionary, -1);
    }

    @Override
    public void onContextActionDown(Dictionary dictionary) {
        dictionaryServiceConnection.swap(dictionary, 1);
    }
}
