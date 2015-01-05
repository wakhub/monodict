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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper;
import com.github.wakhub.monodict.activity.bean.CommonActivityTrait;
import com.github.wakhub.monodict.activity.bean.DatabaseHelper;
import com.github.wakhub.monodict.activity.bean.SpeechHelper;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.Model;
import com.github.wakhub.monodict.preferences.FlashcardActivityState;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.ui.CardContextActionListener;
import com.github.wakhub.monodict.ui.CardContextDialogBuilder;
import com.github.wakhub.monodict.ui.CardDialog;
import com.github.wakhub.monodict.ui.CardEditDialog;
import com.github.wakhub.monodict.utils.DateTimeUtils;
import com.github.wakhub.monodict.utils.ViewUtils;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.EventListener;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EventBus
 * - FlashcardActivityPagerFragment
 *
 * @see com.github.wakhub.monodict.activity.FlashcardActivity_
 */
@EActivity(R.layout.activity_flashcard)
@OptionsMenu({R.menu.flashcard})
public class FlashcardActivity extends ActionBarActivity implements
        CardDialog.OnCardDialogListener,
        CardContextActionListener,
        SpeechHelper.OnUtteranceListener,
        ViewPager.OnPageChangeListener,
        FlashcardActivityPagerFragment.Listener {

    private static final String TAG = FlashcardActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SELECT_DIRECTORY_TO_EXPORT = 10010;
    private static final int REQUEST_CODE_SELECT_FILE_TO_IMPORT = 10011;

    private static final String JSON_KEY_CARDS = "cards";


    @ViewById
    PagerSlidingTabStrip tabs;

    @ViewById
    ViewPager pager;

    @ViewById
    FloatingActionButton autoPlayButton;

    @ViewById
    FloatingActionButton alphabeticalOrderButton;

    @ViewById
    FloatingActionButton shuffleButton;

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

    private Optional<CardDialog> cardDialog = Optional.absent();

    private Optional<CardEditDialog> cardEditDialog = Optional.absent();

    private AlertDialog autoPlayDialog = null;

    private TextView autoPlayTranslateText = null;

    private TextView autoPlayDisplayText = null;

    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 80);

    private Optional<Cursor> optCursor = Optional.absent();

    private Optional<CardDialog> optCardDialog = Optional.absent();

    private static enum AutoPlayProgress {
        START,
        WAIT_FOR_TRANSLATE,
        TRANSLATE,
        WAIT_FOR_DISPLAY,
        DISPLAY,
        WAIT_FOR_NEXT,
        WAIT_FOR_STOP,
        STOP
    }

    private AutoPlayProgress autoPlayProgress = AutoPlayProgress.STOP;

    private boolean isReloadRequired = false;

    @Override
    public void onInitPage(FlashcardActivityPagerFragment fragment, int box) {
        reloadCursor(box, state.getOrder());
    }

    private Snackbar createSnackbar() {
        return Snackbar.with(getApplicationContext())
                .swipeToDismiss(true)
                .eventListener(new EventListener() {
                    @Override
                    public void onShow(Snackbar snackbar) {
                        int snackbarHeight = snackbar.getHeight();
                        ViewUtils.addMarginBottom(autoPlayButton, snackbarHeight);
                        ViewUtils.addMarginBottom(alphabeticalOrderButton, snackbarHeight);
                        ViewUtils.addMarginBottom(shuffleButton, snackbarHeight);
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        snackbar.animation(false);
                    }

                    @Override
                    public void onDismiss(Snackbar snackbar) {
                        int snackbarHeight = snackbar.getHeight();
                        ViewUtils.addMarginBottom(autoPlayButton, -snackbarHeight);
                        ViewUtils.addMarginBottom(alphabeticalOrderButton, -snackbarHeight);
                        ViewUtils.addMarginBottom(shuffleButton, -snackbarHeight);
                    }

                    @Override
                    public void onDismissed(Snackbar snackbar) {
                    }
                });
    }

    @AfterViews
    void afterViews() {
        Log.d(TAG, "state: " + state.toString());

        commonActivityTrait.initActivity(preferences);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        pager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), this, getTabLabels()));
        pager.setCurrentItem(state.getBox() - 1);
        tabs.setViewPager(pager);
        tabs.setIndicatorColorResource(R.color.dark_gray);
        tabs.setOnPageChangeListener(this);
    }

    private FlashcardActivityPagerFragment getCurrentFragment() {
        return getPagerFragment(pager.getCurrentItem());
    }

    private FlashcardActivityPagerFragment getPagerFragment(int position) {
        return ((ViewPagerAdapter) pager.getAdapter()).getFragment(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        stopAutoPlay();
        int box = position + 1;
        state.setBox(box);
        reloadCursor(box, state.getOrder());
        // TODO: scroll to top
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    void refreshPager() {
        ViewPagerAdapter pagerAdapter = (ViewPagerAdapter) pager.getAdapter();
        pagerAdapter.titles = getTabLabels();
        tabs.notifyDataSetChanged();
        reloadCursor();
    }

    private void reloadCursor() {
        int box = state.getBox();
        int order = state.getOrder();
        reloadCursor(box, order);
    }

    void reloadCursor(int box, int order) {
        try {
            if (order == FlashcardActivityState.ORDER_SHUFFLE) {
                optCursor = Optional.of(databaseHelper.findCardInBoxRandomly(box, state.getRandomSeed()));
            } else {
                optCursor = Optional.of(databaseHelper.findCardInBoxAlphabetically(box));
            }
        } catch (SQLException e) {
            optCursor = Optional.absent();
            activityHelper.showError(e);
        }

        List<Card> cardList = new ArrayList<>();
        if (optCursor.isPresent()) {
            Cursor cursor = optCursor.get();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                cardList.add(new Card(cursor));
            }
        }
        FlashcardActivityPagerFragment fragment = ((ViewPagerAdapter) pager.getAdapter()).getFragment(box - 1);
        fragment.setDataSet(cardList);
        fragment.notifyDataSetChanged();
    }

    private String[] getTabLabels() {
        Map<Integer, Integer> countsForBoxes;
        try {
            countsForBoxes = databaseHelper.getCountsForBoxes();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return new String[]{};
        }

        String[] titles = new String[Card.BOX_MAX];
        for (int i = 0; i < Card.BOX_MAX; i++) {
            int box = i + 1;
            String label = String.format("BOX%d", box);
            if (i == 0) {
                label = "INBOX";
            }
            if (countsForBoxes.keySet().contains(box)) {
                label += String.format("(%d)", countsForBoxes.get(box));
            }
            titles[i] = label;
        }

        return titles;
    }

    @Override
    protected void onResume() {
        super.onResume();
        MonodictApp.getEventBus().register(this);

        if (isReloadRequired) {
            refreshPager();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MonodictApp.getEventBus().unregister(this);
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

    @Click(R.id.shuffle_button)
    void onActionShuffle() {
        Log.d(TAG, "onActionShuffle");
        state.refreshRandomSeed();
        int order = FlashcardActivityState.ORDER_SHUFFLE;
        state.setOrder(order);
        reloadCursor(state.getBox(), order);
        getCurrentFragment().notifyDataSetChanged();
    }

    @Click(R.id.alphabetical_order_button)
    void onActionOrderAlphabetically() {
        Log.d(TAG, "onActionOrderAlphabetically");
        int order = FlashcardActivityState.ORDER_ALPHABETICALLY;
        state.setOrder(order);
        reloadCursor(state.getBox(), order);
        getCurrentFragment().notifyDataSetChanged();
    }

    @Click(R.id.auto_play_button)
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
        int padding = getResources().getDimensionPixelSize(R.dimen.space_default);
        layout.setPadding(padding, padding, padding, padding);
        autoPlayDisplayText = new TextView(this);
        autoPlayDisplayText.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(autoPlayDisplayText);
        autoPlayTranslateText = new TextView(this);
        autoPlayTranslateText.setText(R.string.message_in_processing);
        layout.addView(autoPlayTranslateText);

        autoPlayDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.action_auto_play)
                .setIcon(R.drawable.ic_play_arrow_black_24dp)
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
        if (!optCursor.isPresent()) {
            return;
        }
        Cursor cursor = optCursor.get();
        String autoPlayText = getResources().getString(R.string.action_auto_play);
        autoPlayDialog.setTitle(String.format(
                "%s: %d/%d",
                autoPlayText,
                cursor.getPosition() + 1,
                cursor.getCount()));
        autoPlayDisplayText.setText(card.getDisplay());
        autoPlayTranslateText.setText(card.getTranslate());
    }

    private boolean autoPlayLoop() {
        if (!optCursor.isPresent()) {
            return false;
        }
        Cursor cursor = optCursor.get();
        Card card;
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
        if (autoPlayProgress == AutoPlayProgress.STOP) {
            return;
        }
        Log.d(TAG, "stopAutoPlay");
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
        new CardEditDialog(this, null).show();
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
                .setIcon(R.drawable.ic_delete_black_36dp)
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
        try {
            databaseHelper.deleteAllCards();
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }
        onDeleteAllCards();
    }

    @UiThread
    void onDeleteAllCards() {
        reloadCursor(1, state.getOrder());
        refreshPager();
    }

    @Background
    void importCardsFrom(String jsonPath) {
        String json;
        try {
            FileInputStream inputStream = new FileInputStream(jsonPath);
            json = TextUtils.join("", CharStreams.readLines(new InputStreamReader(inputStream)));
        } catch (IOException e) {
            activityHelper.showError(e);
            return;
        }
        JsonArray cards;
        try {
            JsonElement jsonElement = new JsonParser().parse(json);
            JsonObject object = jsonElement.getAsJsonObject();
            cards = object.getAsJsonArray(JSON_KEY_CARDS);
        } catch (JsonIOException e) {
            activityHelper.showError(e);
            return;
        }

        for (JsonElement element : cards) {
            JsonObject cardData = element.getAsJsonObject();
            Card card = new Card(cardData);
            try {
                databaseHelper.createCard(card);
            } catch (SQLException e) {
                activityHelper.showError(e);
                return;
            }
        }
        onImportCards();
    }

    @UiThread
    void onImportCards() {
        SnackbarManager.show(createSnackbar().text(R.string.message_success), this);
        reloadCursor(1, state.getOrder());
        refreshPager();
    }

    @Background
    void exportCardsTo(String path) {
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
                    return;
                }
                cursor.moveToNext();
            }
        } catch (SQLException e) {
            activityHelper.showError(e);
            return;
        }

        try {
            jsonObject.put(JSON_KEY_CARDS, jsonArray);
        } catch (JSONException e) {
            activityHelper.showError(e);
            return;
        }

        try {
            // TODO: Make sure it works
            ByteStreams.copy(
                    new ByteArrayInputStream(jsonObject.toString().getBytes()),
                    new FileOutputStream(path));
        } catch (IOException e) {
            activityHelper.showError(e);
            return;
        }
        onExportCards();
    }

    @UiThread
    void onExportCards() {
        SnackbarManager.show(createSnackbar().text(R.string.message_success), this);
    }


    @Override
    protected void onDestroy() {
        stopAutoPlay();
        speechHelper.finish();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (commonActivityTrait.onMenuItemSelected(item.getItemId(), item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCardContextActionSpeech(Card card) {
        Log.d(TAG, "onCardContextActionSpeech: " + card);
        speechHelper.speech(card.getDisplay().trim());
        return true;
    }

    @Override
    public boolean onCardContextActionEdit(Card card) {
        Log.d(TAG, "onCardContextActionEdit: " + card);
        new CardEditDialog(this, card).show();
        return true;
    }

    @Override
    public boolean onCardContextActionDelete(Card card) {
        Log.d(TAG, "onCardContextActionDelete: " + card);
        final Card finalCard = card;
        activityHelper
                .buildConfirmDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int buttonPosition) {
                        try {
                            databaseHelper.deleteCard(finalCard);
                        } catch (SQLException e) {
                            activityHelper.showError(e);
                            return;
                        }
                        if (optCardDialog.isPresent()) {
                            optCardDialog.get().dismiss();
                        }
                        SnackbarManager.show(
                                createSnackbar().text(getResources().getString(
                                        R.string.message_item_removed, finalCard.getDisplay())),
                                FlashcardActivity.this);
                        reloadCursor();
                        refreshPager();
                    }
                })
                .setIcon(R.drawable.ic_delete_black_36dp)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.message_confirm_delete)
                .show();
        return true;
    }

    @Override
    public boolean onCardContextActionMoveIntoInbox(Card card) {
        Log.d(TAG, "onCardContextActionMoveIntoInbox: " + card);
        if (card.getBox() == 1) {
            return false;
        }
        try {
            card.setBox(1);
            databaseHelper.updateCard(card);
        } catch (SQLException e) {
            activityHelper.showError(e);
        }
        reloadCursor();
        refreshPager();
        return true;
    }

    @Override
    public boolean onCardContextActionSearch(Card card) {
        Log.d(TAG, "onCardContextActionSearch: " + card);
        isReloadRequired = true;
        activityHelper.searchOnMainActivity(card.getDisplay());
        return true;
    }

    @Override
    public boolean onCardDialogClickBackButton(Card card) {
        int box = card.getBox();
        if (box < 2) {
            return true;
        }
        try {
            card.setBox(box - 1);
            databaseHelper.updateCard(card);
        } catch (SQLException e) {
            activityHelper.showError(e);
            return false;
        }
        reloadCursor();
        refreshPager();
        return true;
    }

    @Override
    public boolean onCardDialogClickForwardButton(Card card) {
        int box = card.getBox();
        try {
            card.setBox(box + 1);
            databaseHelper.updateCard(card);
        } catch (SQLException e) {
            activityHelper.showError(e);
            return false;
        }
        reloadCursor();
        refreshPager();
        return true;
    }

    @Subscribe
    public void onEvent(FlashcardActivityPagerFragment.FlashcardItemClickEvent event) {
        Log.d(TAG, "onEvent: " + event);
        if (optCardDialog.isPresent()) {
            optCardDialog.get().dismiss();
        }
        CardDialog dialog = new CardDialog(this, event.getCard());
        dialog.setListener(this);
        dialog.setContextActionContext(this);
        dialog.setContextActionListener(this);
        optCardDialog = Optional.of(dialog);
        dialog.show();
    }

    @Subscribe
    public void onEvent(FlashcardActivityPagerFragment.FlashcardItemSpeechEvent event) {
        Log.d(TAG, "onEvent: " + event);
        speechHelper.speech(event.getCard().getDisplay());
    }

    @Subscribe
    public void onEvent(FlashcardActivityPagerFragment.FlashcardItemMoreEvent event) {
        Log.d(TAG, "onEvent: " + event);
        new CardContextDialogBuilder(this, event.getCard(), new int[]{R.string.action_speech})
                .setContextActionListener(this)
                .show();
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
        if (event.getType().equals(Model.ModelChangeRequestEvent.TYPE_INSERT)) {
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
            SnackbarManager.show(createSnackbar().text(
                    getResources().getString(R.string.message_item_added_to, card.getDisplay(), "INBOX")), this);
        }
        reloadCursor();
        refreshPager();
    }

    // { OnUtteranceListener

    @Override
    public void onUtteranceDone(String utteranceId) {
        switch (autoPlayProgress) {
            case DISPLAY:
                autoPlayProgress = AutoPlayProgress.WAIT_FOR_TRANSLATE;
                return;
            case TRANSLATE:
                if (!optCursor.isPresent()) {
                    return;
                }
                Cursor cursor = optCursor.get();
                if (cursor.getPosition() >= cursor.getCount() - 1) {
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

    private static final class ViewPagerAdapter extends FragmentPagerAdapter {

        private final WeakReference<FlashcardActivity> activityRef;

        private String[] titles;

        private final List<FlashcardActivityPagerFragment> fragments;

        private ViewPagerAdapter(FragmentManager fm,
                                 FlashcardActivity activity,
                                 String[] titles) {
            super(fm);
            this.activityRef = new WeakReference<>(activity);
            this.titles = titles;
            fragments = new ArrayList<>();
            for (int i = 0; i < Card.BOX_MAX; i++) {
                fragments.add(FlashcardActivityPagerFragment.create(i + 1));
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (position < fragments.size()) {
                return fragments.get(position);
            }
            return null;
        }

        private FlashcardActivityPagerFragment getFragment(int position) {
            return (FlashcardActivityPagerFragment) getItem(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            FlashcardActivity activity = activityRef.get();
            if (activity == null) {
                return "";
            }
            return titles[position];
        }

        @Override
        public int getCount() {
            return titles.length;
        }
    }
}
