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
package com.github.wakhub.monodict.activity.settings;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.BrowserActivity;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.json.Downloads;
import com.github.wakhub.monodict.json.DownloadsItem;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.google.gson.Gson;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.HttpsClient;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.res.DimensionRes;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// TODO: refactoring
@EActivity(R.layout.activity_downloads)
@OptionsMenu({R.menu.downloads})
public class DownloadsActivity extends ListActivity {

    private static final String TAG = BrowserActivity.class.getSimpleName();

    final public static String RESULT_INTENT_ENGLISH = "english";
    final public static String RESULT_INTENT_FILENAME = "filename";
    final public static String RESULT_INTENT_PATH = "path";

    private static final int REQUEST_CODE_DOWNLOAD = 10120;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @HttpsClient
    HttpClient httpsClient;

    @DimensionRes
    float spaceRelax;

    private File sdCard;

    private Intent resultIntent;

    private DownloadTask dlTask;

    private ArrayAdapter<DownloadsItem> listAdapter;

    private ProgressDialog progressDialog;

    @AfterViews
    void afterViews() {
        commonActivityTrait.initActivity();
        getActionBar().setDisplayHomeAsUpEnabled(true);

        sdCard = Environment.getExternalStorageDirectory();

        resultIntent = getIntent();
        dlTask = new DownloadTask();

        listAdapter = new ArrayAdapter<DownloadsItem>(this, android.R.layout.simple_list_item_2, android.R.id.text1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                DownloadsItem item = getItem(position);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(Dictionary.EMOJI + item.getName());

                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text2.setText(String.format("%s\nsize: %s", item.getDescription(), item.getKBString()));

                return view;
            }
        };
        setListAdapter(listAdapter);

        Downloads downloads = null;
        try {
            InputStream inputStream = getAssets().open("downloads.json");
            JSONObject json = new JSONObject();
            downloads = (new Gson()).fromJson(IOUtils.toString(inputStream), Downloads.class);
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
                .setTitle(R.string.title_help)
                .show();
    }

    @ItemClick(android.R.id.list)
    void onClickListItem(int position) {
        final DownloadsItem item = listAdapter.getItem(position);
        TextView textView = new TextView(this);
        textView.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        textView.setPadding((int) spaceRelax, (int) spaceRelax, (int) spaceRelax, (int) spaceRelax);
        textView.setText(String.format(
                "%s\nsize: %s",
                item.getDescription(),
                item.getKBString()));
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);

        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.action_download, new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resultIntent.putExtra(RESULT_INTENT_ENGLISH, item.isEnglish());
                        resultIntent.putExtra(RESULT_INTENT_FILENAME, item.getName());
                        startDownload(item);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(R.drawable.ic_action_download)
                .setTitle(item.getName())
                .setView(scrollView)
                .show();
    }

    void startDownload(DownloadsItem downloadsItem) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(R.string.message_in_processing);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        dlTask.execute(downloadsItem.getUrl());
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    class DownloadTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String filename = downloadDicfile(params[0], true);
            return filename;
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

        private String downloadDicfile(String url, boolean zip) {

            String ret = null;
            Log.d(TAG, "downloadDicfile: " + url);
            try {
                // HTTP GET リクエスト
                HttpUriRequest httpGet = new HttpGet(url);
                HttpClient httpClient = null;
                if (url.startsWith("https://")) {
                    httpClient = httpsClient;
                } else {
                    httpClient = new DefaultHttpClient();
                }

                HttpConnectionParams.setSoTimeout(httpClient.getParams(), 30000);
                HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 30000);

                HttpResponse httpResponse = httpClient.execute(httpGet);
                int status = httpResponse.getStatusLine().getStatusCode();
                Log.d(TAG, "HTTP status: " + status);
                if (status != HttpStatus.SC_OK) {
                    return ret;
                }
                InputStream is = httpResponse.getEntity().getContent();
                int length = (int) httpResponse.getEntity().getContentLength();
                String filePath = sdCard.getPath() + "/adice/" + getName(url);
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
            } catch (IOException e) {
                Log.d(TAG, e.toString());
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
                        File nf = new File(sdCard.getPath() + "/adice/" + getName(name));
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
