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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.wakhub.monodict.R;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.DimensionRes;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wak on 5/19/14.
 */
@EBean
public class ActivityHelper {

    //private static final int REQUEST_CODE = 20100;

    @RootContext
    Activity activity;

    @DimensionRes
    float spaceRelax;

    @DimensionRes
    float spaceWell;

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

    public void showToast(int resId) {
        String message = activity.getResources().getString(resId);
        showToast(message);
    }

    @UiThread
    public void showToast(String message) {
        Toast toast = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public void showToastLong(int resId) {
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
        showToast(e.toString());
    }

    /**
     * Build dialog with user input
     *
     * @param onClickListener
     * @return AlertDialog.Builder
     */
    public AlertDialog.Builder buildInputDialog(DialogInterface.OnClickListener onClickListener) {
        return buildInputDialog(null, onClickListener);
    }

    public AlertDialog.Builder buildInputDialog(CharSequence text, DialogInterface.OnClickListener onClickListener) {
        final EditText editText = new EditText(activity);
        if (text != null) {
            editText.setText(text);
        }
        editText.setId(android.R.id.text1);
        return new AlertDialog.Builder(activity).setView(editText)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(android.R.string.cancel, null);
    }

    /**
     * Build confirm dialog
     *
     * @param onClickListener
     * @return AlertDialog.Builder
     */
    public AlertDialog.Builder buildConfirmDialog(DialogInterface.OnClickListener onClickListener) {
        return new AlertDialog.Builder(activity).setPositiveButton(android.R.string.ok,
                onClickListener).setNegativeButton(android.R.string.cancel, null);
    }

    public AlertDialog.Builder buildNoticeDialog(CharSequence text) {
        TextView textView = new TextView(activity);
        textView.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        textView.setPadding((int) spaceRelax, (int) spaceRelax, (int) spaceRelax, (int) spaceRelax);
        textView.setText(text);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);
        return new AlertDialog.Builder(activity).setView(scrollView).setPositiveButton(android.R.string.ok, null);
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

    public Spanned getHtmlFromRaw(int resId) {
        return Html.fromHtml(getStringFromRaw(resId));
    }
    public String getStringFromRaw(int resId) {
        InputStream stream = activity.getResources().openRawResource(resId);
        String text = "";
        try {
            text = IOUtils.toString(stream);
        } catch (IOException e) {
            return "";
        }
        return text;
    }

    // TODO: Open BrowserActivity ?
    public void searchByGoogleCom(String word) {
        Uri uri = Uri.parse(activity.getResources().getString(R.string.url_google_search, word));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }

    public void searchByAlc(String word) {
        Uri uri = Uri.parse(activity.getResources().getString(R.string.url_alc_search, word));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }
}
