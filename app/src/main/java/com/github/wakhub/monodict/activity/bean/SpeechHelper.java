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
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.utils.DateTimeUtils;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by wak on 5/26/14.
 */
@EBean
public class SpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = SpeechHelper.class.getSimpleName();
    public static final int REQUEST_CODE_INIT_DEFAULT_ENGINE = 20300;
    public static final int REQUEST_CODE_INIT_JAPANESE_ENGINE = 20301;

    private static final String JAPANESE_TEST = ".*["
            + "\\p{InHiragana}"
            + "\\p{InKatakana}"
            + "\\p{InCJKUnifiedIdeographs}"
            + "]+.*";

    public static final class ENGINE {
        public static final String GOOGLE_TTS = "com.google.android.tts";
        public static final String SAMSUNG_TTS = "com.samsung.SMT";
        public static final String AQUESTALK_TTS = "com.a_quest.aquestalka";
        public static final String N2_TTS = "jp.kddilabs.n2tts";

        public static final String[] DEFAULT_ENGINES = new String[]{SAMSUNG_TTS, GOOGLE_TTS};
        public static final String[] JAPANESE_ENGINES = new String[]{AQUESTALK_TTS, N2_TTS};
    }

    @RootContext
    Activity activity;

    private TextToSpeech tts = null;

    private TextToSpeech japaneseTts = null;

    private Locale initializingLocale = null;

    private String suspendedText = null;

    private List<TextToSpeech.EngineInfo> availableEngines;

    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String s) {
            Log.d(TAG, "onStart: " + s);
        }

        @Override
        public void onDone(String s) {
            Log.d(TAG, "onDone: " + s);
        }

        @Override
        public void onError(String s) {
            Log.d(TAG, "onError: " + s);
        }
    };

    private void init(int requestCode) {
        showMessage();
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        activity.startActivityForResult(intent, requestCode);
    }

    @UiThread
    void showMessage() {
        Toast.makeText(activity, R.string.message_in_processing, Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            if (requestCode == REQUEST_CODE_INIT_JAPANESE_ENGINE) {
                for (String engine : ENGINE.JAPANESE_ENGINES) {
                    initializingLocale = Locale.JAPANESE;
                    japaneseTts = new TextToSpeech(activity, this, engine);
                    if (japaneseTts != null) {
                        japaneseTts.setOnUtteranceProgressListener(utteranceProgressListener);
                        break;
                    }
                }
            } else {
                for (String engine : ENGINE.DEFAULT_ENGINES) {
                    initializingLocale = Locale.ENGLISH;
                    tts = new TextToSpeech(activity, this, engine);
                    if (tts != null) {
                        tts.setOnUtteranceProgressListener(utteranceProgressListener);
                        break;
                    }
                }
            }
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

    public boolean isProcessing() {
        if (suspendedText != null) {
            return true;
        }
        if (tts != null && tts.isSpeaking()) {
            return true;
        }
        if (japaneseTts != null && japaneseTts.isSpeaking()) {
            return true;
        }
        return false;
    }

    public void speech(String text) {
        if (text.matches(JAPANESE_TEST)) {
            speech(text, Locale.JAPANESE);
        } else {
            speech(text, Locale.ENGLISH);
        }
    }

    private HashMap<String, String> buildSpeackParams() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DateTimeUtils.getInstance().getCurrentDateTimeString());
        return params;
    }

    @Background
    public void speech(String text, Locale locale) {
        if (locale == Locale.JAPANESE) {
            if (japaneseTts == null) {
                if (suspendedText == null) {
                    suspendedText = text;
                    init(REQUEST_CODE_INIT_JAPANESE_ENGINE);
                    return;
                }
                Log.d(TAG, "Japanese TextToSpeech couldn't be initialized");
                return;
            }
            japaneseTts.speak(text, TextToSpeech.QUEUE_ADD, buildSpeackParams());
            return;
        }

        if (tts == null) {
            if (suspendedText == null) {
                suspendedText = text;
                init(REQUEST_CODE_INIT_DEFAULT_ENGINE);
                return;
            }
            Log.d(TAG, "TextToSpeech couldn't be initialized");
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_ADD, buildSpeackParams());
    }

    public void finish() {
        if (tts != null) {
            tts.shutdown();
        }
        if (japaneseTts != null) {
            japaneseTts.shutdown();
        }
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "onInit: " + status);
        if (status == TextToSpeech.ERROR) {
            return;
        }

        if (initializingLocale.equals(Locale.JAPANESE)) {
            japaneseTts.setLanguage(Locale.JAPANESE);
        } else {
            tts.setLanguage(Locale.ENGLISH);
        }

        if (suspendedText != null) {
            speech(new String(suspendedText), initializingLocale);
            suspendedText = null;
        }
    }
}
