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
import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.Preferences_;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by wak on 5/26/14.
 */
@EBean
public class SpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = SpeechHelper.class.getSimpleName();
    public static final int REQUEST_CODE = 20300;

    // TODO: Use kakasi for speaking Japanese with English TTS?
    private static final List<Pair<String, String>> LANGUAGE_TESTS = Arrays.asList(
            new Pair<String, String>("ja", "^["
                    + "a-zA-Z\\d()\\[\\].,\\s"
                    + "\\p{InHiragana}"
                    + "\\p{InKatakana}"
                    + "\\p{InCJKUnifiedIdeographs}"
                    + "]+$"
            ),
            new Pair<String, String>("en", "^[\\p{ASCII}]+$")
    );

    @RootContext
    Activity activity;

    @Pref
    Preferences_ preferences;

    private TextToSpeech tts = null;

    private HashMap<String, String> params = new HashMap<String, String>();

    private String suspendedText = null;

    private void init() {
        showMessage();
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @UiThread
    void showMessage() {
        Toast.makeText(activity, R.string.message_in_processing, Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            tts = new TextToSpeech(activity, this);
        } else {
            Intent installIntent = new Intent();
            installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            activity.startActivity(installIntent);
        }
    }

    @UiThread
    void showLanguageIsNotSupported(String locale) {
        Toast.makeText(
                activity,
                activity.getResources().getString(R.string.message_item_is_not_supported, locale),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInit(int i) {
        Log.d(TAG, "initialized");
        tts.setLanguage(Locale.ENGLISH);

        String settingsLocale = preferences.speechLocale().get().toUpperCase();
        boolean initializedBySettings = false;
        for (Locale locale : Locale.getAvailableLocales()) {
            if (settingsLocale.equals(locale.getDisplayName().toUpperCase())
                    && tts.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(locale);
                initializedBySettings = true;
            }
        }
        if (!initializedBySettings) {
            showLanguageIsNotSupported(settingsLocale);
            return;
        }

        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
        if (suspendedText != null) {
            speech(new String(suspendedText));
            suspendedText = null;
        }
    }

    public boolean isProcessing() {
        if (tts == null) {
            return false;
        }
        return tts.isSpeaking() || suspendedText != null;
    }


    @Background
    public void speech(String text) {
        if (tts == null) {
            if (suspendedText == null) {
                suspendedText = text;
                init();
                return;
            }
            Log.d(TAG, "TextToSpeech couldn't be initialized");
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_ADD, params);
    }

    public void finish() {
        if (tts != null) {
            tts.shutdown();
        }
    }
}
