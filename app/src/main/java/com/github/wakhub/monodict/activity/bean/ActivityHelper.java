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
package com.github.wakhub.monodict.activity.bean;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.BrowserActivity_;
import com.github.wakhub.monodict.activity.MainActivity_;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.Model;
import com.github.wakhub.monodict.json.SearchEngines;
import com.github.wakhub.monodict.json.SearchEnginesItem;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.stream.MalformedJsonException;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.DimensionRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by wak on 5/19/14.
 */
@EBean
public class ActivityHelper {

    //private static final int REQUEST_CODE = 20100;

    @RootContext
    Activity activity;

    @DimensionRes
    float spaceSuperRelax;

    @Pref
    Preferences_ preferences;

    @Bean
    Dictionaries dictionaries;

    private ProgressDialog progressDialog = null;

    /**
     * http://stackoverflow.com/questions/6547969/android-refresh-current-activity
     */
    public void restartActivity() {
        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.finish();
        activity.startActivity(intent);
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public void showToast(@StringRes int resId) {
        String message = activity.getResources().getString(resId);
        showToast(message);
    }

    @UiThread
    public void showToast(String message) {
        Toast toast = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    public void showToastLong(@StringRes int resId) {
        showToastLong(activity.getResources().getString(resId));
    }

    @UiThread
    public void showToastLong(String message) {
        Toast toast = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public void showError(Exception e) {
        Log.d("ERROR", e.toString());
        showToastLong(e.toString());
    }

    /**
     * Build dialog with user input
     *
     * @param text
     * @param callback
     * @return MaterialDialog.Builder
     */
    public MaterialDialog.Builder buildInputDialog(CharSequence text, MaterialDialog.ButtonCallback callback) {
        final EditText editText = new EditText(activity);
        if (text != null) {
            editText.setText(text);
        }
        editText.setId(android.R.id.text1);
        return new MaterialDialog.Builder(activity)
                .customView(editText)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .callback(callback);
    }

    /**
     * Build confirm dialog
     *
     * @param callback
     * @return MaterialDialog.Builder
     */
    public MaterialDialog.Builder buildConfirmDialog(MaterialDialog.ButtonCallback callback) {
        return new MaterialDialog.Builder(activity)
                .positiveText(android.R.string.ok)
                .callback(callback)
                .negativeText(android.R.string.cancel);
    }

    public MaterialDialog.Builder buildNoticeDialog(CharSequence text) {
        TextView textView = new TextView(activity);
        textView.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        textView.setText(text);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);
        return new MaterialDialog.Builder(activity)
                .customView(scrollView)
                .positiveText(android.R.string.ok);
    }

    @Nullable
    public PopupMenu popupSearchEngines(@NonNull final CharSequence query, @NonNull View anchor) {
        final SearchEngines searchEngines;
        try {
            InputStream inputStream = activity.getAssets().open("search_engines.json");
            searchEngines = (new Gson()).fromJson(
                    CharStreams.toString(new InputStreamReader(inputStream)),
                    SearchEngines.class);
        } catch (MalformedJsonException e) {
            showError(e);
            return null;
        } catch (IOException e) {
            showError(e);
            return null;
        }
        PopupMenu popupMenu = new PopupMenu(activity, anchor);
        for (SearchEnginesItem item : searchEngines.getItems()) {
            popupMenu.getMenu().add(item.getName());
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                for (SearchEnginesItem searchEngine : searchEngines.getItems()) {
                    if (item.getTitle().equals(searchEngine.getName())) {
                        BrowserActivity_.intent(activity)
                                .extraUrlOrKeywords(String.format(searchEngine.getUrl(), query))
                                .start();
                    }
                }
                return false;
            }
        });
        return popupMenu;
    }

    public void onDuplicatedCardFound(final Card duplicateCard, final String newTranslate, final String newDictionary) {
        int box = duplicateCard.getBox();
        Resources resources = activity.getResources();
        String boxName = String.format("BOX%d", box);
        if (box <= 1) {
            boxName = "INBOX";
        }

        String message = resources.getString(
                R.string.message_item_already_registered_and_confirm,
                duplicateCard.getDisplay(),
                boxName);
        final DatabaseHelper databaseHelper = DatabaseHelper_.getInstance_(activity);

        buildConfirmDialog(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                duplicateCard.setBox(1);
                duplicateCard.setTranslate(newTranslate);
                duplicateCard.setDictionary(newDictionary);
                duplicateCard.notifyChangeRequest(Model.ModelChangeRequestEvent.TYPE_UPDATE);
            }
        }).title(duplicateCard.getDisplay()).content(message).show();
    }

    /**
     * Show ProgressDialog
     */
    @UiThread
    public void showProgressDialog(String message) {
        if (progressDialog != null) {
            return;
        }
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    public void showProgressDialog(@StringRes int resId) {
        showProgressDialog(activity.getResources().getString(resId));
    }

    /**
     * http://stackoverflow.com/questions/19538282/view-not-attached-to-window-manager-dialog-dismiss
     */
    @UiThread
    public void hideProgressDialog() {
        if (progressDialog != null) {
            if (!activity.isFinishing()) {
                try {
                    progressDialog.dismiss();
                    progressDialog = null;
                } catch (Exception e) {
                    // pass
                }
            }
        }
    }

    /**
     * Show progressBar
     *
     * @see #hideProgressBar
     */
    @UiThread
    public void showProgressBar() {
        ProgressBar progressBar = (ProgressBar) activity.findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide progressBar
     *
     * @see #showProgressBar
     */
    @UiThread
    public void hideProgressBar() {
        ProgressBar progressBar = (ProgressBar) activity.findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    public void hideProgressBar(int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
            }
        }, delay);
    }

    public Spanned getHtmlFromRaw(@RawRes int resId) {
        return Html.fromHtml(getStringFromRaw(resId));
    }

    public String getStringFromRaw(@RawRes int resId) {
        InputStream stream = activity.getResources().openRawResource(resId);
        String text;
        try {
            text = CharStreams.toString(new InputStreamReader(stream));
        } catch (IOException e) {
            return "";
        }
        return text;
    }

    public void searchOnMainActivity(String query) {
        Intent intent = MainActivity_.intent(activity).get();
        intent.putExtra(SearchManager.QUERY, query);
        intent.setAction(Intent.ACTION_SEARCH);
        activity.startActivity(intent);
    }

    public void clear() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @UiThread
    public void setListPosition(int position) {
        ((ListActivity) activity).getListView().setSelection(position);
    }
}
