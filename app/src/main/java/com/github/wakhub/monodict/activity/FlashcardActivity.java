/**
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.preferences.FlashcardActivityState;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.ui.CardContextDialogBuilder;
import com.github.wakhub.monodict.ui.CardDialog;
import com.github.wakhub.monodict.ui.CardEditDialog;
import com.github.wakhub.monodict.utils.DateTimeUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * @see com.github.wakhub.monodict.activity.FlashcardActivity_
 */
@EActivity(R.layout.activity_flashcard)
@OptionsMenu({R.menu.flashcard})
public class FlashcardActivity extends ListActivity
        implements ActionBar.TabListener,
        CardDialog.OnCardDialogListener,
        CardEditDialog.Listener,
        CardContextDialogBuilder.OnContextActionListener,
        SpeechHelper.OnUtteranceListener {

    private static final String TAG = FlashcardActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SELECT_DIRECTORY_TO_EXPORT = 10010;
    private static final int REQUEST_CODE_SELECT_FILE_TO_IMPORT = 10011;

    private static final String JSON_KEY_CARDS = "cards";

    @SystemService
    PowerManager powerManager;

    @Pref
    Preferences_ preferences;

    @Bean
    CommonActivityTrait commonActivityTrait;

    @Bean
    ActivityHelper activityHelper;

    @Bean
    SpeechHelper speechHelper;

    @Bean
    DatabaseHelper databaseHelper;

    @Bean
    FlashcardActivityState state;

    private AlertDialog autoPlayDialog = null;
    private TextView autoPlayTranslateText = null;
    private TextView autoPlayDisplayText = null;

    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 80);

    private static enum AutoPlayProgress {
        START, WAIT_FOR_TRANSLATE, TRANSLATE, WAIT_FOR_DISPLAY, DISPLAY, WAIT_FOR_NEXT, WAIT_FOR_STOP, STOP
    }

    private AutoPlayProgress autoPlayProgress = AutoPlayProgress.STOP;

    private ListAdapter listAdapter = null;

    private boolean isTabInitialized = false;

    private boolean isReloadRequired = false;

    @AfterViews
    void afterViews() {
        Log.d(TAG, "state: " + state.toString());
        commonActivityTrait.initActivity(preferences);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @UiThread
    void reloadTabs() {
        Map<Integer, Integer> countsForBoxes;
        try {
            countsForBoxes = databaseHelper.getCountsForBoxes();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }

        ActionBar actionBar = getActionBar();
        for (int i = 0; i < Card.BOX_MAX; i++) {
            ActionBar.Tab tab;
            if (actionBar.getTabCount() > i) {
                tab = actionBar.getTabAt(i);
            } else {
                tab = actionBar.newTab().setTag(i).setTabListener(this);
                actionBar.addTab(tab);
            }
            int box = i + 1;
            String label = String.format("BOX%d", box);
            if (i == 0) {
                label = "INBOX";
            }
            if (countsForBoxes.keySet().contains(box)) {
                label += String.format("(%d)", countsForBoxes.get(box));
            }
            tab.setText(label);
        }

        if (!isTabInitialized || isReloadRequired) {
            isTabInitialized = true;
            isReloadRequired = false;
            // TODO: not working
            int index = state.getBox() - 1;
            if (index < 0 || actionBar.getTabCount() < index) {
                index = 0;
            }
            actionBar.getTabAt(index).select();
            loadContents();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        reloadTabs();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isReloadRequired) {
            reloadTabs();
        }
    }

    @Background
    void loadContents() {
        Log.d(TAG, "loadContents: box=" + state.getBox());
        activityHelper.showProgressDialog(R.string.message_in_processing);
        int box = getActionBar().getSelectedTab().getPosition() + 1;

        Cursor cursor;
        try {
            if (state.getOrder() == FlashcardActivityState.ORDER_SHUFFLE) {
                cursor = databaseHelper.findCardInBoxRandomly(box, state.getRandomSeed());
            } else {
                cursor = databaseHelper.findCardInBoxAlphabetically(box);
            }
        } catch (SQLException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }
        if (cursor != null) {
            onLoadContents(cursor);
        } else {
            activityHelper.hideProgressDialog();
        }
    }

    @UiThread
    void onLoadContents(Cursor cursor) {
        Log.d(TAG, "onLoadContents: " + cursor.getCount());

        // how to maintain scroll position of listview when it updates
        // http://stackoverflow.com/questions/10196079
        ListView listView = getListView();
        int lastPosition = listView.getFirstVisiblePosition();
        int lastTopOffset = 0;
        if (listView.getCount() > 0) {
            View view = listView.getChildAt(0);
            if (view != null) {
                lastTopOffset = view.getTop();
            }
        }

        listAdapter = new ListAdapter(this, cursor);
        setListAdapter(listAdapter);

        if (lastPosition > listView.getCount()) {
            listView.setSelection(listView.getCount() - 1);
        } else {
            listView.setSelectionFromTop(lastPosition, lastTopOffset);
        }

        reloadTabs();
        activityHelper.hideProgressDialog();
    }

    @OnActivityResult(SpeechHelper.REQUEST_CODE_TTS)
    void onActivityResultSpeechHelper(int resultCode, Intent data) {
        speechHelper.onActivityResult(SpeechHelper.REQUEST_CODE_TTS, resultCode, data);
    }

    @OnActivityResult(REQUEST_CODE_SELECT_DIRECTORY_TO_EXPORT)
    void onActivityResultSelectDirectoryToExport(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        String path = data.getExtras().getString(DirectorySelectorActivity.RESULT_INTENT_PATH);

        String defaultPath = String.format("%s/%s-%s.json",
                path,
                getResources().getString(R.string.app_name),
                DateTimeUtils.getInstance().getCurrentDateTimeString());
        activityHelper
                .buildInputDialog(defaultPath, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TextView textView = (TextView) ((Dialog) dialogInterface).findViewById(android.R.id.text1);
                        String outputPath = textView.getText().toString();
                        exportCardsTo(outputPath);
                    }
                })
                .setTitle(R.string.action_export)
                .show();
    }

    @OnActivityResult(REQUEST_CODE_SELECT_FILE_TO_IMPORT)
    void onActivityResultSelectFileToImport(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        Bundle extras = data.getExtras();
        String path = extras.getString(FileSelectorActivity.RESULT_INTENT_PATH);
        String filename = extras.getString(FileSelectorActivity.RESULT_INTENT_FILENAME);
        importCardsFrom(path + "/" + filename);
    }

    @OptionsItem(R.id.action_shuffle)
    void onActionShuffle() {
        Log.d(TAG, "onActionShuffle");
        state.refreshRandomSeed();
        state.setOrder(FlashcardActivityState.ORDER_SHUFFLE);
        loadContents();
    }

    @OptionsItem(R.id.action_order_alphabetically)
    void onActionOrderAlphabetically() {
        Log.d(TAG, "onActionOrderAlphabetically");
        state.setOrder(FlashcardActivityState.ORDER_ALPHABETICALLY);
        loadContents();
    }

    @OptionsItem(R.id.action_auto_play)
    void onActionAutoPlay() {
        Log.d(TAG, "onActionAutoPlay");
        startAutoPlay();
    }

    @UiThread
    void showAutoPlayAlertDialog() {
        if (autoPlayDialog != null) {
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setPadding(20, 20, 20, 20);
        autoPlayDisplayText = new TextView(this);
        autoPlayDisplayText.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(autoPlayDisplayText);
        autoPlayTranslateText = new TextView(this);
        autoPlayTranslateText.setText(R.string.message_in_processing);
        layout.addView(autoPlayTranslateText);

        autoPlayDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.action_auto_play)
                .setIcon(R.drawable.ic_action_play)
                .setView(layout)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        autoPlayDisplayText = null;
                        autoPlayTranslateText = null;
                        stopAutoPlay();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .show();
    }

    @UiThread
    void setAutoPlayCard(Card card) {
        if (autoPlayDialog == null || autoPlayDisplayText == null || autoPlayTranslateText == null) {
            return;
        }
        String autoPlayText = getResources().getString(R.string.action_auto_play);
        autoPlayDialog.setTitle(String.format(
                "%s: %d/%d",
                autoPlayText,
                listAdapter.getCursor().getPosition() + 1,
                listAdapter.getCount()));
        autoPlayDisplayText.setText(card.getDisplay());
        autoPlayTranslateText.setText(card.getTranslate());
    }

    private boolean autoPlayLoop() {
        Cursor cursor = listAdapter.getCursor();
        Card card = null;
        switch (autoPlayProgress) {
            case START:
                speechHelper.init();
                showAutoPlayAlertDialog();
                speechHelper.setOnUtteranceListener(this);
                cursor.moveToFirst();
                autoPlayProgress = AutoPlayProgress.WAIT_FOR_DISPLAY;
                try {
                    card = new Card(cursor);
                } catch (CursorIndexOutOfBoundsException e) {
                    activityHelper.showError(e);
                    return false;
                }
                setAutoPlayCard(card);
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK);
                activityHelper.sleep(2000);
                break;
            case WAIT_FOR_DISPLAY:
                autoPlayProgress = AutoPlayProgress.DISPLAY;
                activityHelper.sleep(500);
                speechHelper.speech(new Card(cursor).getDisplay());
                break;
            case WAIT_FOR_TRANSLATE:
                autoPlayProgress = AutoPlayProgress.TRANSLATE;
                String languageForTranslate = preferences.ttsLanguageForTranslate().get();
                Locale localeForTranslate = new Locale(languageForTranslate.substring(0, 2));

                activityHelper.sleep(500);
                try {
                    card = new Card(cursor);
                } catch (CursorIndexOutOfBoundsException e) {
                    activityHelper.showError(e);
                    return false;
                }
                speechHelper.speech(
                        card.getTranslate().substring(0, Math.min(card.getTranslate().length(), 100)),
                        localeForTranslate, null);
                break;
            case WAIT_FOR_NEXT:
                cursor.moveToNext();
                setAutoPlayCard(new Card(cursor));
                autoPlayProgress = AutoPlayProgress.WAIT_FOR_DISPLAY;

                activityHelper.sleep(500);
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                activityHelper.sleep(1000);
                break;
            case WAIT_FOR_STOP:
                stopAutoPlay();
                return false;
        }
        return true;
    }

    @Background
    void startAutoPlay() {
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        autoPlayProgress = AutoPlayProgress.START;

        while (autoPlayLoop()) {
            activityHelper.sleep(500);
        }
        wakeLock.release();
    }

    private void stopAutoPlay() {
        Log.d(TAG, "stopAutoPlay");
        if (autoPlayProgress == AutoPlayProgress.STOP) {
            return;
        }
        autoPlayProgress = AutoPlayProgress.STOP;
        speechHelper.finish();
        speechHelper.setOnUtteranceListener(null);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK);
        if (autoPlayDialog != null) {
            autoPlayDialog.dismiss();
            autoPlayDialog = null;
        }
    }

    @OptionsItem(R.id.action_add)
    void onActionAdd() {
        CardEditDialog dialog = new CardEditDialog(this, null);
        dialog.setListener(this);
        dialog.show();
    }

    @OptionsItem(R.id.action_delete_all)
    void onActionDeleteAll() {
        activityHelper
                .buildConfirmDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteAllCards();
                    }
                })
                .setTitle(R.string.action_delete_all)
                .setIcon(R.drawable.ic_action_discard)
                .setMessage(R.string.message_confirm_delete)
                .show();

    }

    @OptionsItem(R.id.action_import)
    void onActionImport() {
        FileSelectorActivity_.intent(this)
                .extraTitle(getResources().getString(R.string.action_import))
                .extraExtensions(new String[]{".json"})
                .startForResult(REQUEST_CODE_SELECT_FILE_TO_IMPORT);
    }

    @OptionsItem(R.id.action_export)
    void onActionExport() {
        DirectorySelectorActivity_.intent(this)
                .startForResult(REQUEST_CODE_SELECT_DIRECTORY_TO_EXPORT);
    }

    @Background
    void deleteAllCards() {
        activityHelper.showProgressDialog(R.string.message_in_processing);
        try {
            databaseHelper.deleteAllCards();
        } catch (SQLException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }
        activityHelper.hideProgressDialog();
        loadContents();
    }

    @Background
    void importCardsFrom(String jsonPath) {
        activityHelper.showProgressDialog(R.string.message_in_processing);
        String json;
        try {
            FileInputStream inputStream = new FileInputStream(jsonPath);
            json = TextUtils.join("", IOUtils.readLines(inputStream));
        } catch (FileNotFoundException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        } catch (IOException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }
        JsonArray cards;
        try {
            JsonElement jsonElement = new JsonParser().parse(json);
            JsonObject object = jsonElement.getAsJsonObject();
            cards = object.getAsJsonArray(JSON_KEY_CARDS);
        } catch (JsonIOException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }

        for (JsonElement element : cards) {
            JsonObject cardData = element.getAsJsonObject();
            Card card = new Card(cardData);
            try {
                databaseHelper.createCard(card);
            } catch (SQLException e) {
                activityHelper.showError(e);
                activityHelper.hideProgressDialog();
                return;
            }
        }
        activityHelper.hideProgressDialog();
        activityHelper.showToast(R.string.message_success);
        loadContents();
    }

    @Background
    void exportCardsTo(String path) {
        activityHelper.showProgressDialog(R.string.message_in_processing);
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            Cursor cursor = databaseHelper.findAllCards();
            for (int i = 0; i < cursor.getCount(); i++) {
                Card card = new Card(cursor);
                JSONObject cardData = new JSONObject();
                try {
                    cardData.put(Card.Column.DISPLAY, card.getDisplay());
                    cardData.put(Card.Column.TRANSLATE, card.getTranslate());
                    cardData.put(Card.Column.BOX, card.getBox());
                    cardData.put(Card.Column.DICTIONARY, card.getDictionary());
                    jsonArray.put(cardData);
                } catch (JSONException e) {
                    activityHelper.showError(e);
                    activityHelper.hideProgressDialog();
                    return;
                }
                cursor.moveToNext();
            }
        } catch (SQLException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }

        try {
            jsonObject.put(JSON_KEY_CARDS, jsonArray);
        } catch (JSONException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            IOUtils.write(jsonObject.toString(), outputStream);
        } catch (FileNotFoundException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        } catch (IOException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }
        activityHelper.hideProgressDialog();
        activityHelper.showToast(R.string.message_success);
    }

    @Override
    protected void onDestroy() {
        stopAutoPlay();
        speechHelper.finish();
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        Integer index = (Integer) tab.getTag();
        Log.d(TAG, "onTabSelected: " + index);
        stopAutoPlay();
        state.setBox(index + 1);
        getListView().setSelection(0);
        loadContents();
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // pass
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // pass
    }

    @Override
    public boolean onCardDialogClickSpeechButton(Card card) {
        speechHelper.speech(card.getDisplay());
        return true;
    }

    @Override
    public boolean onCardDialogClickBackButton(Card card) {
        if (state.getBox() - 1 < 1) {
            return true;
        }
        try {
            card.setBox(state.getBox() - 1);
            databaseHelper.updateCard(card);
            loadContents();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean onCardDialogClickForwardButton(Card card) {
        try {
            card.setBox(state.getBox() + 1);
            databaseHelper.updateCard(card);
            loadContents();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return false;
        }
        return true;
    }

    @Override
    public void onContextActionSearch(Card card) {
        isReloadRequired = true;
        activityHelper.searchOnMainActivity(card.getDisplay());
    }

    @Override
    public void onContextActionMoveIntoInbox(Card card) {
        if (card.getBox() == 1) {
            return;
        }
        try {
            card.setBox(1);
            databaseHelper.updateCard(card);
            loadContents();
        } catch (SQLException e) {
            activityHelper.showError(e);
        }
    }

    @Override
    public void onContextActionEdit(Card card) {
        CardEditDialog dialog = new CardEditDialog(this, card);
        dialog.setListener(this);
        dialog.show();
    }

    @Override
    public void onContextActionDelete(final Card card) {
        activityHelper
                .buildConfirmDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int buttonPosition) {

                        try {
                            databaseHelper.deleteCard(card);
                        } catch (SQLException e) {
                            activityHelper.showError(e);
                            return;
                        }
                        loadContents();
                    }
                })
                .setIcon(R.drawable.ic_action_discard)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.message_confirm_delete)
                .show();

    }

    @Override
    public void onCardEditDialogSave(CardEditDialog dialog, Card card) {
        if (card.getId() != null) {
            try {
                databaseHelper.updateCard(card);
            } catch (SQLException e) {
                activityHelper.showError(e);
                return;
            }
            activityHelper.showToast(R.string.message_modified);
        } else {
            try {
                Card duplicate = databaseHelper.getCardByDisplay(card.getDisplay());
                if (duplicate != null) {
                    activityHelper.onDuplicatedCardFound(duplicate, card.getDisplay(), card.getDictionary());
                    return;
                }
                databaseHelper.createCard(card);
            } catch (SQLException e) {
                activityHelper.showError(e);
                return;
            }
            activityHelper.showToast(getResources().getString(R.string.message_item_added_to, card.getDisplay(), "INBOX"));
        }
        dialog.dismiss();
        if (card.getBox() != state.getBox()) {
            return;
        }
        loadContents();
    }

    // { OnUtteranceListener

    @Override
    public void onUtteranceDone(String utteranceId) {
        switch (autoPlayProgress) {
            case DISPLAY:
                autoPlayProgress = AutoPlayProgress.WAIT_FOR_TRANSLATE;
                return;
            case TRANSLATE:
                if (listAdapter.getCursor().getPosition() >= listAdapter.getCount() - 1) {
                    autoPlayProgress = AutoPlayProgress.WAIT_FOR_STOP;
                } else {
                    autoPlayProgress = AutoPlayProgress.WAIT_FOR_NEXT;
                }
        }
    }

    @Override
    public void onUtteranceError(String utteranceId) {
        activityHelper.showToast("Canceled");
        onUtteranceDone(utteranceId);
    }

    // OnUtteranceListener }

    private static class ListAdapter extends CursorAdapter {

        private final WeakReference<FlashcardActivity> activityRef;

        ListAdapter(FlashcardActivity activity, Cursor cursor) {
            super(activity, cursor, true);
            activityRef = new WeakReference<FlashcardActivity>(activity);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final FlashcardActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            int position = cursor.getPosition();
            final Card card = new Card(cursor);

            view.findViewById(R.id.space_top).setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.space_bottom).setVisibility(position == getCount() - 1 ? View.VISIBLE : View.GONE);

            ((TextView) view.findViewById(android.R.id.text1)).setText(card.getDisplay());

            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            String dictionary = card.getDictionary();
            if (dictionary == null || dictionary.isEmpty()) {
                text2.setVisibility(View.GONE);
            } else {
                text2.setVisibility(View.VISIBLE);
                text2.setText(dictionary);
            }
            view.findViewById(R.id.speech_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.speechHelper.speech(card.getDisplay());
                }
            });
            view.findViewById(R.id.action_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new CardContextDialogBuilder(activity, null, card)
                            .setContextActionListener(activity)
                            .show();
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CardDialog dialog = new CardDialog(activity, card);
                    dialog.setListener(activity);
                    dialog.setContextActionContext(activity);
                    dialog.setContextActionListener(activity);
                    dialog.show();
                }
            });
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            FlashcardActivity activity = activityRef.get();
            if (activity == null) {
                return null;
            }
            LinearLayout view = new LinearLayout(activity);
            activity.getLayoutInflater().inflate(R.layout.list_item_card, view);
            bindView(view, context, cursor);
            return view;
        }
    }
}
