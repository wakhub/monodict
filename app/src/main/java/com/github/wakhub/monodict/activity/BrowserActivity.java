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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.db.Bookmark;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.preferences.BrowserActivityState;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.search.DictionaryService;
import com.github.wakhub.monodict.search.DictionaryServiceConnection;
import com.github.wakhub.monodict.ui.DicItemListView;
import com.github.wakhub.monodict.ui.TranslatePanelFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;

@EActivity(R.layout.activity_browser)
@OptionsMenu({R.menu.browser})
public class BrowserActivity extends Activity
        implements DictionaryService.Listener,
        TranslatePanelFragment.Listener,
        TextView.OnEditorActionListener {

    private static final String TAG = BrowserActivity.class.getSimpleName();

    private static final int REQUEST_CODE_BOOKMARKS = 10020;
    private static final int REQUEST_CODE_OPEN_LOCAL_FILE = 10021;
    private static final String ENCODING = "utf-8";
    private static final String JAVASCRIPT_CALLBACK_SEARCH = "search";
    private static final String JAVASCRIPT_CALLBACK_SPEECH = "speech";
    private static final String[] PROTOCOLS = new String[]{"http://", "https://", "ftp://", "file://"};

    @Extra
    String extraUrlOrKeywords = "";

    @ViewById
    EditText urlText;

    @ViewById
    WebView webView;

    @ViewById
    ProgressBar progressBar;

    @FragmentById
    TranslatePanelFragment translatePanelFragment;

    @OptionsMenuItem
    MenuItem actionBack;

    @OptionsMenuItem
    MenuItem actionForward;

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @Bean
    DatabaseHelper databaseHelper;

    @Bean
    BrowserActivityState state;

    @Bean
    SpeechHelper speechHelper;

    @Bean
    Dictionaries dictionaries;

    private DictionaryServiceConnection dictionaryServiceConnection;

    @AfterViews
    void afterViews() {
        Log.d(TAG, "state: " + state.toString());

        commonActivityTrait.initActivity(preferences);

        urlText.setSelectAllOnFocus(true);
        urlText.setOnEditorActionListener(this);

        webView.setWebViewClient(new BrowserWebViewClient(this));
        webView.addJavascriptInterface(new BrowserJavaScriptInterface(this), BrowserJavaScriptInterface.NAME);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName(ENCODING);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        translatePanelFragment.setListener(this);

        if (extraUrlOrKeywords.isEmpty()) {
            loadUrl(state.getLastUrl());
        } else {
            loadUrl(extraUrlOrKeywords);
        }
    }

    private void loadUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        for (String protocol : PROTOCOLS) {
            if (url.startsWith(protocol)) {
                Log.d(TAG, "loadUrl: " + url.toString());
                webView.loadUrl(url);
                return;
            }
        }
        webView.loadUrl(getResources().getString(R.string.url_google_com_search, url));
    }

    @UiThread
    void reloadViews() {
        if (actionBack != null) {
            actionBack.setEnabled(webView.canGoBack());
        }
        if (actionForward != null) {
            actionForward.setEnabled(webView.canGoForward());
        }
        if (translatePanelFragment != null) {
            translatePanelFragment.hide();
        }
    }

    @OnActivityResult(REQUEST_CODE_BOOKMARKS)
    void onActivityResultBookmarks(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        String url = data.getExtras().getString(BrowserBookmarksActivity.EXTRA_URL);
        webView.loadUrl(url);
    }

    @OnActivityResult(REQUEST_CODE_OPEN_LOCAL_FILE)
    void onActivityResultOpenLocalFile(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        Bundle extras = data.getExtras();
        String path = extras.getString(FileSelectorActivity.RESULT_INTENT_PATH);
        String filename = extras.getString(FileSelectorActivity.RESULT_INTENT_FILENAME);
        loadUrl(String.format("file://%s/%s", path, filename));
    }

    @OptionsItem(R.id.action_back)
    void onActionBack() {
        Log.d(TAG, "onActionBack");
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    @OptionsItem(R.id.action_forward)
    void onActionForward() {
        Log.d(TAG, "onActionForward");
        if (webView.canGoForward()) {
            webView.goForward();
        }
    }

    @OptionsItem(R.id.action_reload)
    void onActionReload() {
        Log.d(TAG, "onActionReload");
        webView.reload();
    }

    @OptionsItem(R.id.action_add_to_bookmarks)
    void onActionAddToBookmarks() {
        Log.d(TAG, "onActionAddToBookmarks");
        Bookmark bookmark;

        String title = getTitle().toString();
        if (title == null) {
            title = "";
        }

        CharSequence descriptionCharSequence = webView.getContentDescription();
        String description = "";
        if (descriptionCharSequence != null) {
            description = descriptionCharSequence.toString();
        }

        try {
            bookmark = databaseHelper.createBookmark(webView.getUrl(), title, description);
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
        Resources resources = getResources();
        String message = resources.getString(R.string.message_item_added_to,
                bookmark.getTitle(),
                resources.getString(R.string.title_activity_browser_bookmarks));
        activityHelper.showToastLong(message);
    }

    @OptionsItem(R.id.action_show_bookmarks)
    void onActionShowBookmarks() {
        Log.d(TAG, "onActionShowBookmarks");
        BrowserBookmarksActivity_.intent(this).startForResult(REQUEST_CODE_BOOKMARKS);
    }

    @OptionsItem(R.id.action_open_local_file)
    void onActionOpenLocalFile() {
        Log.d(TAG, "onActionOpenLocalFile");
        FileSelectorActivity_.intent(this).startForResult(REQUEST_CODE_OPEN_LOCAL_FILE);
    }

    private void startGettingSelectionInBrowser(String callback) {
        String script = String.format(
                "%s.onBrowserJavaScriptGetSelection(\"%s\", document.getSelection().toString());",
                BrowserJavaScriptInterface.NAME,
                callback);
        webView.loadUrl("javascript:" + script);
    }

    private void onJavaScriptGetSelection(String callback, String selection) {
        Log.d(TAG, "onJavaScriptGetSelection");
        if (callback.equals(JAVASCRIPT_CALLBACK_SEARCH)) {
            search(selection);
        }
        if (callback.equals(JAVASCRIPT_CALLBACK_SPEECH)) {
            speechHelper.speech(selection);
        }
    }

    @Background
    void search(String query) {
        dictionaryServiceConnection.search(query);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dictionaryServiceConnection == null) {
            dictionaryServiceConnection = new DictionaryServiceConnection(this);
        }

        bindService(
                new Intent(this, DictionaryService.class),
                dictionaryServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(dictionaryServiceConnection);
    }

    @Override
    protected void onDestroy() {
        speechHelper.finish();
        webView.setWebViewClient(null);
        webView.removeJavascriptInterface(BrowserJavaScriptInterface.NAME);
        webView.stopLoading();
        webView.destroy();
        translatePanelFragment.setListener(null);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (commonActivityTrait.isGoingBack()) {
            super.onBackPressed();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
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
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * https://android.googlesource.com/platform/frameworks/base/+/cd92588/core/java/android/webkit/SelectActionModeCallback.java
     *
     * @param callback
     * @return
     */
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        ActionMode actionMode = super.onWindowStartingActionMode(callback);
        Menu menu = actionMode.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        }

        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.browser_action_mode, menu);
        translatePanelFragment.hide();

        menu.findItem(R.id.action_search).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                startGettingSelectionInBrowser(JAVASCRIPT_CALLBACK_SEARCH);
                return false;
            }
        });
        menu.findItem(R.id.action_speech).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                startGettingSelectionInBrowser(JAVASCRIPT_CALLBACK_SPEECH);
                return false;
            }
        });

        return actionMode;
    }

    @Override
    public void onDictionaryServiceInitialized() {
        // pass
    }

    @Override
    public void onDictionaryServiceUpdateDictionaries() {
        // pass
    }

    @Override
    @UiThread
    public void onDictionaryServiceResult(String query, ArrayList<DicItemListView.Data> result) {
        String dictionaryName = "";
        DicItemListView.Data firstWordData = null;
        for (DicItemListView.Data data : result) {
            if (dictionaryName.equals("") && data.getMode() == DicItemListView.Data.FOOTER) {
                dictionaryName = data.Index.toString();
            }
            if (firstWordData == null && data.getMode() == DicItemListView.Data.WORD) {
                firstWordData = data;
            }
        }

        if (firstWordData == null) {
            activityHelper.showToast(R.string.message_no_result);
            return;
        }

        translatePanelFragment.setDictionaryName(dictionaryName);
        translatePanelFragment.setData(firstWordData);
        translatePanelFragment.show();
    }

    @Override
    public void onDictionaryServiceError(String query, Exception e) {
        activityHelper.showError(e);
    }

    @Override
    public void onClickTranslatePanelAddToFlashcardButton(DicItemListView.Data data) {
        Card card;
        try {
            final Card duplicate = databaseHelper.getCardByDisplay(data.Index.toString());
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
        String message = resources.getString(
                R.string.message_item_added_to,
                card.getDisplay(),
                resources.getString(R.string.title_activity_flashcard));
        activityHelper.showToast(message);
    }

    @Override
    public void onClickTranslatePanelSpeechButton(DicItemListView.Data data) {
        speechHelper.speech(data.Index.toString());
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            loadUrl(v.getText().toString());
        }
        return false;
    }

    private static final class BrowserWebViewClient extends WebViewClient {

        private final WeakReference<BrowserActivity> activityRef;

        private BrowserWebViewClient(BrowserActivity activity) {
            this.activityRef = new WeakReference<BrowserActivity>(activity);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            BrowserActivity activity = this.activityRef.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.VISIBLE);
                activity.urlText.setText(url);
                activity.state.setLastUrl(url);
                activity.reloadViews();
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            BrowserActivity activity = this.activityRef.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.GONE);
                if (url.startsWith("file://")) {
                    String[] split = url.split("/");
                    String filename = split[split.length - 1];
                    activity.setTitle(filename);
                } else {
                    activity.setTitle(activity.webView.getTitle());
                }
                activity.reloadViews();
            }
            super.onPageFinished(view, url);
        }
    }

    private static final class BrowserJavaScriptInterface {

        private static final String NAME = "__com_github_wakhub_monodict";

        private final WeakReference<BrowserActivity> activityRef;

        private BrowserJavaScriptInterface(BrowserActivity activity) {
            this.activityRef = new WeakReference<BrowserActivity>(activity);
        }

        @JavascriptInterface
        public void onBrowserJavaScriptGetSelection(String callback, String selection) {
            if (this.activityRef.get() != null) {
                this.activityRef.get().onJavaScriptGetSelection(callback, selection);
            }
        }
    }
}
