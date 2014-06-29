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
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.preferences.FlashcardActivityState;
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
import org.androidannotations.annotations.UiThread;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
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
        CardContextDialogBuilder.OnContextActionListener {

    private static final String TAG = FlashcardActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SELECT_DIRECTORY_TO_EXPORT = 10010;
    private static final int REQUEST_CODE_SELECT_FILE_TO_IMPORT = 10011;

    private static final String JSON_KEY_CARDS = "cards";

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

    private ArrayAdapter<Card> listAdapter = null;

    private boolean isTabInitialized = false;

    @AfterViews
    void afterViews() {
        Log.d(TAG, "state: " + state.toString());

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        listAdapter = new ListAdapter(this);
        setListAdapter(listAdapter);
        reloadTabs();
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

        if (!isTabInitialized) {
            isTabInitialized = true;
            int index = state.getBox() - 1;
            if (index < 0 || actionBar.getTabCount() < index) {
                index = 0;
            }
            actionBar.getTabAt(index).select();
        }
    }

    @Background
    void loadContents() {
        Log.d(TAG, "loadContents: box=" + state.getBox());
        activityHelper.showProgressDialog(R.string.message_in_processing);

        List<Card> cardList;
        try {
            if (state.getOrder() == FlashcardActivityState.ORDER_SHUFFLE) {
                cardList = databaseHelper.findCardInBoxRandomly(state.getBox(), state.getRandomSeed());
            } else {
                cardList = databaseHelper.findCardInBoxAlphabetically(state.getBox());
            }
        } catch (SQLException e) {
            activityHelper.showError(e);
            activityHelper.hideProgressDialog();
            return;
        }
        if (cardList != null) {
            onLoadContents(cardList);
        } else {
            activityHelper.hideProgressDialog();
        }
    }

    @UiThread
    void onLoadContents(List<Card> cardList) {
        listAdapter.clear();
        listAdapter.addAll(cardList);
        reloadTabs();
        activityHelper.hideProgressDialog();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getListView().setSelection(0);
            }
        }, 100);
    }

    @OnActivityResult(REQUEST_CODE_SELECT_DIRECTORY_TO_EXPORT)
    void onActivityResultSelectDirectoryToExport(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        String path = data.getExtras().getString(DirectorySelectorActivity.RESULT_INTENT_PATH);
        Calendar now = Calendar.getInstance();

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

    @OnActivityResult(SpeechHelper.REQUEST_CODE_INIT_DEFAULT_ENGINE)
    void onActivityResultSpeechHelper(int resultCode, Intent data) {
        speechHelper.onActivityResult(SpeechHelper.REQUEST_CODE_INIT_DEFAULT_ENGINE, resultCode, data);
    }

    @OnActivityResult(SpeechHelper.REQUEST_CODE_INIT_JAPANESE_ENGINE)
    void onActivityResultSpeechHelperJapanese(int resultCode, Intent data) {
        speechHelper.onActivityResult(SpeechHelper.REQUEST_CODE_INIT_JAPANESE_ENGINE, resultCode, data);
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
        // TODO: This code might use many memory
        activityHelper.showProgressDialog(R.string.message_in_processing);
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            for (Card card : databaseHelper.findAllCards()) {
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
        if (isTabInitialized || (index == 0 && state.getBox() == 1)) {
            state.setBox(index + 1);
            loadContents();
        }
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
            return false;
        }
        try {
            card.setBox(state.getBox() - 1);
            databaseHelper.updateCard(card);
            listAdapter.remove(card);
            reloadTabs();
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
            listAdapter.remove(card);
            reloadTabs();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return false;
        }
        return true;
    }

    @Override
    public void onContextActionSearch(Card card) {
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
            listAdapter.remove(card);
            reloadTabs();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
    }

    @Override
    public void onContextActionEdit(Card card) {
        CardEditDialog dialog = new CardEditDialog(this, card);
        dialog.setListener(this);
        dialog.show();
    }

    @Override
    public void onContextActionDelete(Card card) {
        // TODO: confirm dialog
        try {
            databaseHelper.deleteCard(card);
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
        for (int i = 0; i < listAdapter.getCount(); i++) {
            Card cardInList = listAdapter.getItem(i);
            if (card.equals(cardInList)) {
                listAdapter.remove(cardInList);
            }
        }
        reloadTabs();
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
                Card duplicated = databaseHelper.getCardByDisplay(card.getDisplay());
                if (duplicated != null) {
                    activityHelper.onDuplicatedCardFound(duplicated);
                    return;
                }
                databaseHelper.createCard(card);
            } catch (SQLException e) {
                activityHelper.showError(e);
                return;
            }
            activityHelper.showToast(getResources().getString(R.string.message_item_added, card.getDisplay()));
        }

        dialog.dismiss();
        loadContents();
    }

    private class ListAdapter extends ArrayAdapter<Card> {
        ListAdapter(Context context) {
            super(context, R.layout.list_item_card, android.R.id.text1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.findViewById(R.id.space_top).setVisibility(
                    position == 0
                            ? View.VISIBLE : View.GONE
            );
            view.findViewById(R.id.space_bottom).setVisibility(
                    position == listAdapter.getCount() - 1
                            ? View.VISIBLE : View.GONE
            );

            final Card card = getItem(position);

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
                    speechHelper.speech(card.getDisplay());
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CardDialog dialog = new CardDialog(getContext(), card);
                    dialog.setListener(FlashcardActivity.this);
                    dialog.setContextActionContext(FlashcardActivity.this);
                    dialog.setContextActionListener(FlashcardActivity.this);
                    dialog.show();
                }
            });

            return view;
        }
    }
}
