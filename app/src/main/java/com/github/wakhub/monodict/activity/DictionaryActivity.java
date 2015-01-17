/*
 * Copyright (C) 2015 wak
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

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.preferences.DictionaryFont;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.search.DictionaryService;
import com.github.wakhub.monodict.search.DictionaryServiceConnection;
import com.github.wakhub.monodict.ui.DictionaryFontView;
import com.github.wakhub.monodict.utils.FontHelper;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.Arrays;
import java.util.List;

@EActivity(R.layout.activity_dictionary)
@OptionsMenu(R.menu.dictionary)
public class DictionaryActivity extends AbsListActivity {

    private static final String TAG = DictionaryActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SELECT_INDEX_FONT = 1001;

    private static final int REQUEST_CODE_SELECT_PHONE_FONT = 1002;

    private static final int REQUEST_CODE_SELECT_TRANS_FONT = 1003;

    private static final int REQUEST_CODE_SELECT_SAMPLE_FONT = 1004;

    private static final List<Integer> SELECT_FONT_REQUEST_CODES = Arrays.asList(
            REQUEST_CODE_SELECT_INDEX_FONT,
            REQUEST_CODE_SELECT_PHONE_FONT,
            REQUEST_CODE_SELECT_TRANS_FONT,
            REQUEST_CODE_SELECT_SAMPLE_FONT);

    @Extra
    String extraDictionaryPath;

    @ViewById
    TextView nameText;

    @ViewById
    TextView pathText;

    @ViewById
    ImageButton renameButton;

    @ViewById
    CheckBox enableCheckBox;

    @ViewById
    DictionaryFontView indexFontView;

    @ViewById
    DictionaryFontView transFontView;

    @ViewById
    DictionaryFontView sampleFontView;

    @ViewById
    DictionaryFontView phoneFontView;

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @Bean
    Dictionaries dictionaries;

    @Bean
    FontHelper fontHelper;

    private DictionaryServiceConnection dictionaryServiceConnection;

    private int getMaxResultsIndex() {
        String[] maxResultsArray = getResources().getStringArray(R.array.max_results);
        int index = -1;
        for (String maxResults : maxResultsArray) {
            index++;
            if (Integer.valueOf(maxResults) == getDictionary().getMaxResults()) {
                return index;
            }
        }
        return 0;
    }

    @AfterViews
    void afterViews() {
        commonActivityTrait.initActivity(preferences);

        Dictionary dictionary = getDictionary();

        enableCheckBox.setChecked(dictionary.isEnabled());

//        indexFontView.setTitle("Index font");
        indexFontView.setListener(new DictionaryFontView.Listener() {
            @Override
            public void onDictionaryFontViewClickResetButton() {
                Dictionary dictionary = getDictionary();
                dictionary.setIndexFont(DictionaryFont.getDefaultIndexFont());
                dictionaries.updateDictionary(dictionary);
                reload();
            }

            @Override
            public void onDictionaryFontViewClickSettingsButton(DictionaryFont font) {
                FontFileSelectorActivity_.intent(DictionaryActivity.this).startForResult(REQUEST_CODE_SELECT_INDEX_FONT);
            }

            @Override
            public void onDictionaryFontViewChangeFont(DictionaryFont font) {
                Dictionary dictionary = getDictionary();
                dictionary.setIndexFont(font);
                dictionaries.updateDictionary(dictionary);
                reload();
            }
        });

        phoneFontView.setTitle("Phone font");
        phoneFontView.setListener(new DictionaryFontView.Listener() {
            @Override
            public void onDictionaryFontViewClickResetButton() {
                Dictionary dictionary = getDictionary();
                dictionary.setPhoneFont(DictionaryFont.getDefaultPhoneFont());
                dictionaries.updateDictionary(dictionary);
                reload();
            }

            @Override
            public void onDictionaryFontViewClickSettingsButton(DictionaryFont font) {
                FontFileSelectorActivity_.intent(DictionaryActivity.this)
                        .startForResult(REQUEST_CODE_SELECT_PHONE_FONT);
            }

            @Override
            public void onDictionaryFontViewChangeFont(DictionaryFont font) {
                Dictionary dictionary = getDictionary();
                dictionary.setPhoneFont(font);
                dictionaries.updateDictionary(dictionary);
                reload();
            }
        });

        transFontView.setTitle("Trans font");
        transFontView.setListener(new DictionaryFontView.Listener() {
            @Override
            public void onDictionaryFontViewClickResetButton() {
                Dictionary dictionary = getDictionary();
                dictionary.setTransFont(DictionaryFont.getDefaultTransFont());
                dictionaries.updateDictionary(dictionary);
                reload();
            }

            @Override
            public void onDictionaryFontViewClickSettingsButton(DictionaryFont font) {
                FontFileSelectorActivity_.intent(DictionaryActivity.this).startForResult(REQUEST_CODE_SELECT_TRANS_FONT);
            }

            @Override
            public void onDictionaryFontViewChangeFont(DictionaryFont font) {
                Dictionary dictionary = getDictionary();
                dictionary.setTransFont(font);
                dictionaries.updateDictionary(dictionary);
                reload();
            }
        });

        sampleFontView.setTitle("Sample font");
        sampleFontView.setListener(new DictionaryFontView.Listener() {
            @Override
            public void onDictionaryFontViewClickResetButton() {
                Dictionary dictionary = getDictionary();
                dictionary.setSampleFont(DictionaryFont.getDefaultSampleFont());
                dictionaries.updateDictionary(dictionary);
                reload();
            }

            @Override
            public void onDictionaryFontViewClickSettingsButton(DictionaryFont font) {
                FontFileSelectorActivity_.intent(DictionaryActivity.this).startForResult(REQUEST_CODE_SELECT_SAMPLE_FONT);
            }

            @Override
            public void onDictionaryFontViewChangeFont(DictionaryFont font) {
                Dictionary dictionary = getDictionary();
                dictionary.setSampleFont(font);
                dictionaries.updateDictionary(dictionary);
                reload();
            }
        });

        reload();
    }

    private void reload() {
        Log.d(TAG, "reload");
        Dictionary dictionary = getDictionary();
        nameText.setText(dictionary.getName());
        pathText.setText(dictionary.getPath());
        indexFontView.setDictionaryFont(dictionary.getIndexFont());
        phoneFontView.setDictionaryFont(dictionary.getPhoneFont());
        transFontView.setDictionaryFont(dictionary.getTransFont());
        sampleFontView.setDictionaryFont(dictionary.getSampleFont());
    }

    private Dictionary getDictionary() {
        return dictionaries.getDictionaryByPath(extraDictionaryPath);
    }

    @Click(R.id.rename_button)
    void onClickRenameButton() {
        final Dictionary dictionary = getDictionary();
        activityHelper
                .buildInputDialog(dictionary.getName(), new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        TextView textView = (TextView) materialDialog.findViewById(android.R.id.text1);
                        String dictionaryName = textView.getText().toString().trim();
                        if (dictionaryName.isEmpty()) {
                            return;
                        }
                        dictionary.setName(dictionaryName);
                        dictionaries.updateDictionary(dictionary);
                        onRename();
                    }
                })
                .icon(R.drawable.ic_edit_black_36dp)
                .title(R.string.action_rename)
                .show();
    }

    @UiThread
    void onRename() {
        reload();
    }

    @OptionsItem(R.id.action_delete)
    void onClickDeleteButton() {
        final Dictionary dictionary = getDictionary();
        activityHelper
                .buildConfirmDialog(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        dictionaries.removeDictionary(dictionary);
                        dictionaryServiceConnection.deleteDictionary(dictionary);
                        finish();
                    }
                })
                .icon(R.drawable.ic_delete_black_36dp)
                .title(dictionary.getName())
                .content(R.string.message_confirm_delete)
                .show();
    }

    @CheckedChange(R.id.enable_check_box)
    void onCheckedChangeEnableCheckBox(boolean isChecked) {
        Dictionary dictionary = getDictionary();
        dictionary.setEnabled(isChecked);
        dictionaries.updateDictionary(dictionary);
    }

    @Subscribe
    void onEvent(DictionaryService.DictionaryDeletedEvent event) {
        if (event.getDictionary().equals(getDictionary())) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MonodictApp.getEventBus().register(this);
        if (dictionaryServiceConnection == null) {
            dictionaryServiceConnection = new DictionaryServiceConnection();
        }

        bindService(
                new Intent(this, DictionaryService.class),
                dictionaryServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MonodictApp.getEventBus().unregister(this);
        unbindService(dictionaryServiceConnection);
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
        super.onActivityResult(requestCode, resultCode, data);
        if (!SELECT_FONT_REQUEST_CODES.contains(requestCode)) {
            return;
        }
        Optional<DictionaryFont> font = getFontFromActivityResult(resultCode, data);
        if (!font.isPresent()) {
            return;
        }
        Dictionary dictionary = getDictionary();
        switch (requestCode) {
            case REQUEST_CODE_SELECT_INDEX_FONT:
                dictionary.setIndexFont(font.get());
                break;
            case REQUEST_CODE_SELECT_PHONE_FONT:
                dictionary.setPhoneFont(font.get());
                break;
            case REQUEST_CODE_SELECT_TRANS_FONT:
                dictionary.setTransFont(font.get());
                break;
            case REQUEST_CODE_SELECT_SAMPLE_FONT:
                dictionary.setSampleFont(font.get());
                break;
        }
        dictionaries.updateDictionary(dictionary);
        reload();
    }

    private Optional<DictionaryFont> getFontFromActivityResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return Optional.absent();
        }
        String path = data.getStringExtra(FontFileSelectorActivity.RESULT_INTENT_PATH)
                + "/" + data.getStringExtra(FontFileSelectorActivity.RESULT_INTENT_FILENAME);
        return Optional.of(new DictionaryFont(path));
    }
}
