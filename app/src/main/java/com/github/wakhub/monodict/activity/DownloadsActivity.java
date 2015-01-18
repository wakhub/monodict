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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.json.Downloads;
import com.github.wakhub.monodict.json.DownloadsItem;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.HttpsClient;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// TODO: refactoring
@EActivity(R.layout.activity_downloads)
@OptionsMenu({R.menu.downloads})
public class DownloadsActivity extends AbsListActivity {

    private static final String TAG = BrowserActivity.class.getSimpleName();

    final public static String RESULT_INTENT_ENGLISH = "english";
    final public static String RESULT_INTENT_FILENAME = "filename";
    final public static String RESULT_INTENT_PATH = "path";

    private static final int REQUEST_CODE_DOWNLOAD = 10120;

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @HttpsClient
    HttpClient httpsClient;

    @StringRes
    String appName;

    private File sdCard;

    private Intent resultIntent;

    private DownloadTask dlTask;

    private ArrayAdapter<DownloadsItem> listAdapter;

    private ProgressDialog progressDialog;

    @AfterViews
    void afterViews() {
        commonActivityTrait.initActivity(preferences);

        sdCard = Environment.getExternalStorageDirectory();

        resultIntent = getIntent();
        dlTask = new DownloadTask();

        listAdapter = new ArrayAdapter<DownloadsItem>(this, R.layout.list_item_download, android.R.id.text1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                DownloadsItem item = getItem(position);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(item.getName());

                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text2.setText(String.format("%s\nsize: %s", item.getDescription(), item.getSize()));

                return view;
            }
        };
        setListAdapter(listAdapter);

        Downloads downloads = null;
        try {
            InputStream inputStream = getAssets().open("downloads.json");
            downloads = (new Gson()).fromJson(
                    CharStreams.toString(new InputStreamReader(inputStream)),
                    Downloads.class);
        } catch (IOException e) {
            activityHelper.showError(e);
            return;
        }
        listAdapter.addAll(downloads.getItems());
    }

    @OnActivityResult(REQUEST_CODE_DOWNLOAD)
    void onActivityResultDownload(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        setResult(RESULT_OK, data);
        finish();
    }

    @OptionsItem(R.id.action_help)
    void onActionHelp() {
        activityHelper.buildNoticeDialog(activityHelper.getStringFromRaw(R.raw.downloads_help))
                .title(R.string.title_help)
                .show();
    }

    @ItemClick(android.R.id.list)
    void onClickListItem(int position) {
        final DownloadsItem item = listAdapter.getItem(position);
        TextView textView = new TextView(this);
        textView.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        textView.setText(String.format(
                "%s\nsize: %s",
                item.getDescription(),
                item.getSize()));
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);

        new MaterialDialog.Builder(this)
                .icon(R.drawable.ic_file_download_black_36dp)
                .title(item.getName())
                .customView(scrollView)
                .positiveText(R.string.action_download)
                .callback(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        resultIntent.putExtra(RESULT_INTENT_ENGLISH, item.isEnglish());
                        resultIntent.putExtra(RESULT_INTENT_FILENAME, item.getName());
                        startDownload(item);
                    }
                })
                .negativeText(android.R.string.cancel)
                .show();
    }

    void startDownload(DownloadsItem downloadsItem) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(R.string.message_in_processing);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        dlTask.execute(downloadsItem.getUrl(), downloadsItem.getUrlMirror());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (commonActivityTrait.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class DownloadTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            String urlMirror = params[1];
            try {
                return downloadDicfile(url, true);
            } catch (IOException e) {
                Log.d(TAG, String.format("Failed to download %s", url));
                if (!urlMirror.isEmpty()) {
                    activityHelper.showToast(R.string.action_retry);
                    try {
                        return downloadDicfile(urlMirror, true);
                    } catch (IOException e2) {
                        Log.d(TAG, String.format("Failed to download %s", urlMirror));
                        activityHelper.showToast(R.string.message_download_failed);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();

            if (result != null) {
                Toast.makeText(DownloadsActivity.this, R.string.message_download_succeed, Toast.LENGTH_LONG).show();
                resultIntent.putExtra(RESULT_INTENT_PATH, result);
                setResult(RESULT_OK, resultIntent);
            } else {
                Toast.makeText(DownloadsActivity.this, R.string.message_download_failed, Toast.LENGTH_LONG).show();
            }
            dlTask = null;
            finish();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
            if (progress[1] > 0) {
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(progress[1]);
            } else {
                progressDialog.setIndeterminate(true);
                progressDialog.setMax(1);
            }
        }

        private String getName(String path) {
            String[] patharr = path.split("/");

            return patharr[patharr.length - 1];
        }

        private String downloadDicfile(String url, boolean zip) throws IOException {

            String ret = null;
            Log.d(TAG, "downloadDicfile: " + url);
            // HTTP GET リクエスト
            HttpUriRequest httpGet = new HttpGet(url);
            HttpClient httpClient = null;
            if (url.startsWith("https://")) {
                httpClient = httpsClient;
            } else {
                httpClient = new DefaultHttpClient();
            }

            HttpConnectionParams.setSoTimeout(httpClient.getParams(), 30000);
            HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 10000);

            HttpResponse httpResponse = httpClient.execute(httpGet);
            int status = httpResponse.getStatusLine().getStatusCode();
            Log.d(TAG, "HTTP status: " + status);
            if (status != HttpStatus.SC_OK) {
                return ret;
            }
            InputStream is = httpResponse.getEntity().getContent();
            int length = (int) httpResponse.getEntity().getContentLength();
            String filePath = sdCard.getPath() + "/" + appName + "/" + getName(url);
            Log.d(TAG, "The file will be saved as " + filePath);
            File f = new File(filePath);
            f.getParentFile().mkdir();

            FileOutputStream fos = new FileOutputStream(f);

            byte[] buff = new byte[4096];
            int len;
            int offset = 0;
            int lastOffset = 0;
            for (; ; ) {
                len = is.read(buff);
                if (len == -1) break;
                fos.write(buff, 0, len);
                offset += len;

                // update progress bar
                if (offset - lastOffset > 1024 * 16) {
                    publishProgress(offset, length);
                    lastOffset = offset;
                }
            }
            fos.close();
            is.close();

            if (url.endsWith(".zip") || zip) {
                ret = extractZip(f);
                f.delete();
            } else {
                ret = f.getPath();
            }

            return ret;
        }


        private String extractZip(File f) {
            Log.d(TAG, "extractZip: " + f.getName());
            String ret = null;
            ZipInputStream zis;
            try {
                zis = new ZipInputStream(new FileInputStream(f));
                ZipEntry ze;
                while (ret == null && (ze = zis.getNextEntry()) != null) {
                    String name = ze.getName();

                    if (name.toLowerCase().endsWith(".dic")) {
                        File nf = new File(sdCard.getPath() + "/" + appName + "/" + getName(name));
                        nf.getParentFile().mkdir();

                        FileOutputStream fos = new FileOutputStream(nf);

                        byte[] buff = new byte[512];
                        int len;
                        int offset = 0;
                        int lastOffset = 0;

                        for (; ; ) {
                            len = zis.read(buff);
                            if (len == -1) break;
                            fos.write(buff, 0, len);
                            offset += len;

                            // update progress bar
                            if (offset - lastOffset > 1024 * 16) {
                                publishProgress(offset, 0);
                                lastOffset = offset;
                            }
                        }
                        fos.close();
                        ret = nf.getPath();
                    }
                    zis.closeEntry();
                }
                zis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Finished extract");
            return ret;
        }
    }
}
