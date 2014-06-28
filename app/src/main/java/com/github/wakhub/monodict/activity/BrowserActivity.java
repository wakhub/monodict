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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.db.Bookmark;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.preferences.BrowserActivityState;
import com.github.wakhub.monodict.search.DictionaryService;
import com.github.wakhub.monodict.search.DictionaryServiceConnection;
import com.github.wakhub.monodict.ui.DicItemListView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EActivity(R.layout.activity_browser)
@OptionsMenu({R.menu.browser})
public class BrowserActivity extends Activity implements DictionaryService.Listener {

    private static final String TAG = BrowserActivity.class.getSimpleName();

    private static final int REQUEST_CODE_BOOKMARKS = 10020;
    private static final int TRANSLATE_PANEL_DURATION = 150;
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

    // TODO: create class
    @ViewById
    RelativeLayout translatePanel;
    @ViewById
    TextView translatePanelDisplayText;
    @ViewById
    TextView translatePanelTranslateText;
    @ViewById
    TextView translatePanelDictionaryNameText;

    @OptionsMenuItem
    MenuItem actionBack;

    @OptionsMenuItem
    MenuItem actionForward;

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

    private DictionaryServiceConnection dictionaryServiceConnection;

    private List<String> currentDisplayAndTranslateAndDictionary;

    private String selectedText = "";

    @AfterViews
    void afterViews() {
        Log.d(TAG, "state: " + state.toString());

        commonActivityTrait.initActivity();

        urlText.setSelectAllOnFocus(true);
        urlText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    loadUrl(textView.getText().toString());
                }
                return false;
            }
        });

        webView.setWebViewClient(new BrowserWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new BrowserJavaScriptInterface(), BrowserJavaScriptInterface.NAME);

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
        webView.loadUrl(getResources().getString(R.string.url_google_search, url));
    }

    @UiThread
    void reloadViews() {
        if (actionBack != null) {
            actionBack.setEnabled(webView.canGoBack());
        }
        if (actionForward != null) {
            actionForward.setEnabled(webView.canGoForward());
        }
        if (translatePanel != null) {
            translatePanel.setVisibility(View.GONE);
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

    @OnActivityResult(SpeechHelper.REQUEST_CODE)
    void onActivityResultSpeechHelper(int resultCode, Intent data) {
        speechHelper.onActivityResult(resultCode, data);
    }

    @Click(R.id.translate_panel_more_button)
    void onClickTranslatePanelMoreButton() {
        Intent intent = MainActivity_.intent(this).get();
        intent.putExtra(SearchManager.QUERY, selectedText);
        intent.setAction(Intent.ACTION_SEARCH);
        startActivity(intent);
    }

    @Click(R.id.translate_panel_speech_button)
    void onClickTranslatePanelSpeechButton() {
        speechHelper.speech(selectedText);
    }

    @Click(R.id.translate_panel_close_button)
    void onClickTranslatePanelCloseButton() {
        translatePanel.setVisibility(View.GONE);
    }

    @Click(R.id.translate_panel_add_to_flashcard_button)
    void onClickTranslatePanelAddToFlashcardButton() {
        if (currentDisplayAndTranslateAndDictionary == null) {
            return;
        }
        Card card;

        try {
            card = databaseHelper.getCardByDisplay(currentDisplayAndTranslateAndDictionary.get(0));
            if (card != null) {
                activityHelper.showToast(getResources().getString(R.string.message_item_already_registered, card.getDisplay()));
                return;
            }
            card = databaseHelper.createCard(currentDisplayAndTranslateAndDictionary.get(0),
                    currentDisplayAndTranslateAndDictionary.get(1),
                    currentDisplayAndTranslateAndDictionary.get(2));
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
        activityHelper.showToast(getResources().getString(R.string.message_item_added, card.getDisplay()));
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

        String title = webView.getTitle();

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
        activityHelper.showToastLong(getResources().getString(R.string.message_item_added, bookmark.getTitle()));
    }

    @OptionsItem(R.id.action_show_bookmarks)
    void onActionShowBookmarks() {
        Log.d(TAG, "onActionShowBookmarks");
        BrowserBookmarksActivity_.intent(this).startForResult(REQUEST_CODE_BOOKMARKS);
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
        selectedText = selection;
        if (callback.equals(JAVASCRIPT_CALLBACK_SEARCH)) {
            search(selectedText);
        }
        if (callback.equals(JAVASCRIPT_CALLBACK_SPEECH)) {
            speechHelper.speech(selectedText);
        }
    }

    @Background
    void search(String query) {
        currentDisplayAndTranslateAndDictionary = null;
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
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        ActionMode actionMode = super.onWindowStartingActionMode(callback);
        Menu menu = actionMode.getMenu();
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.browser_action_mode, menu);

        translatePanel.setVisibility(View.GONE);
        currentDisplayAndTranslateAndDictionary = null;

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

        translatePanelDisplayText.setText(firstWordData.Index.toString());
        translatePanelTranslateText.setScrollY(0);
        translatePanelTranslateText.setText(firstWordData.Trans.toString());
        translatePanelDictionaryNameText.setText(dictionaryName);
        translatePanel.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.makeInChildBottomAnimation(this);
        animation.setDuration(TRANSLATE_PANEL_DURATION);
        translatePanel.startAnimation(animation);

        currentDisplayAndTranslateAndDictionary = Arrays.asList(
                firstWordData.Index.toString(),
                firstWordData.Trans.toString(),
                dictionaryName);
    }

    private final class BrowserWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            urlText.setText(url);
            state.setLastUrl(url);
            reloadViews();
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            setTitle(webView.getTitle());
            reloadViews();
            super.onPageFinished(view, url);
        }
    }

    private final class BrowserJavaScriptInterface {

        public static final String NAME = "__com_github_wakhub_monodict";

        @JavascriptInterface
        public void onBrowserJavaScriptGetSelection(String callback, String selection) {
            onJavaScriptGetSelection(callback, selection);
        }
    }
}
