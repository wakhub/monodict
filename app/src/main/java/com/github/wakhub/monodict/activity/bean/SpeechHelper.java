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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.utils.DateTimeUtils;
import com.github.wakhub.monodict.utils.EasyLocaleDetector;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by wak on 5/26/14.
 * <p/>
 * memo:
 * - UtteranceProgressListener is available since api 15
 * <p/>
 */
@EBean
public class SpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = SpeechHelper.class.getSimpleName();

    public static final int REQUEST_CODE_TTS = 20300;

    private WeakReference<OnUtteranceListener> onUtteranceListenerRef = new WeakReference<OnUtteranceListener>(null);

    public interface OnUtteranceListener {
        void onUtteranceDone(String utteranceId);

        void onUtteranceError(String utteranceId);
    }

    @Pref
    Preferences_ preferences;

    @RootContext
    Activity activity;

    private TextToSpeech tts = null;

    private String suspendedText = null;

    private boolean autoLanguageDetection = false;

    public void init() {
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        activity.startActivityForResult(intent, REQUEST_CODE_TTS);
    }

    public void setAutoLanguageDetection(boolean autoLanguageDetection) {
        this.autoLanguageDetection = autoLanguageDetection;
    }

    @UiThread
    void showProgressMessage() {
        Toast.makeText(activity, R.string.message_in_processing, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            Log.d(TAG, "TTS doesn't have voice data");
            Intent installIntent = new Intent();
            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            activity.startActivity(installIntent);
            return;
        }


        final WeakReference<SpeechHelper> helperRef = new WeakReference<SpeechHelper>(this);
        int listenerResult = -1;

        if (tts == null) {
            tts = new TextToSpeech(activity, this);
            if (Build.VERSION.SDK_INT >= 15) {
                Log.d(TAG, "setUtteranceProgressListener");
                listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "onStart: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "onDone: " + utteranceId);
                        if (helperRef.get() != null) {
                            helperRef.get().onUtteranceDone(utteranceId);
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.d(TAG, "onError: " + utteranceId);
                        if (helperRef.get() != null) {
                            helperRef.get().onUtteranceError(utteranceId);
                        }
                    }
                });
                if (listenerResult != TextToSpeech.SUCCESS) {
                    tts.setOnUtteranceProgressListener(null);
                    Log.d(TAG, "failed to add utterance progress listener");
                }
            }
            if (listenerResult != TextToSpeech.SUCCESS) {
                Log.d(TAG, "setOnUtteranceCompletedListener");
                listenerResult = tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        Log.d(TAG, "onUtteranceCompleted: " + utteranceId);
                        if (helperRef.get() != null) {
                            helperRef.get().onUtteranceDone(utteranceId);
                        }
                    }
                });
                if (listenerResult != TextToSpeech.SUCCESS) {
                    tts.setOnUtteranceCompletedListener(null);
                    Log.d(TAG, "failed to add utterance completed listener");
                }
            }
        }
    }

    @UiThread
    void showTtsIsNotSupported() {
        Toast.makeText(activity, "TTS is not supported", Toast.LENGTH_LONG).show();
    }

    public boolean isProcessing() {
        if (suspendedText != null) {
            return true;
        }
        return tts != null && tts.isSpeaking();
    }

    public void speech(String text) {
        speech(text, null);
    }

    public void speech(String text, HashMap<String, String> params) {
        String defaultLocaleText = preferences.ttsDefaultLocale().get().substring(0, 2).toLowerCase();
        Locale defaultLocale = new Locale(defaultLocaleText);

        Locale locale = EasyLocaleDetector.detectLocale(text);
        if (locale.equals(Locale.ENGLISH)) {
            locale = defaultLocale;
        }
        Log.d(TAG, "Locale selected: " + locale.getDisplayName());
        speech(text, locale, params);
    }

    @Background
    public void speech(String text, Locale speechLocale, HashMap<String, String> params) {
        Log.d(TAG, String.format("speech: (%s) %s", speechLocale.getDisplayName(), text));
        if (tts == null) {
            if (suspendedText == null) {
                suspendedText = text;
                init();
                return;
            }
            showTtsIsNotSupported();
            return;
        }
        tts.setLanguage(speechLocale);
        Log.d(TAG, "TextToSpeech.speak: " + tts.speak(text, TextToSpeech.QUEUE_ADD, buildSpeakParams(params)));
    }

    private HashMap<String, String> buildSpeakParams(HashMap<String, String> additional) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DateTimeUtils.getInstance().getCurrentDateTimeString());
        if (additional != null) {
            for (String key : additional.keySet()) {
                params.put(key, additional.get(key));
            }
        }
        return params;
    }

    public void finish() {
        Log.d(TAG, "finish");
        if (tts != null) {
            tts.shutdown();
            tts = null;
            suspendedText = null;
        }
    }

    public void setOnUtteranceListener(OnUtteranceListener listener) {
        this.onUtteranceListenerRef = new WeakReference<OnUtteranceListener>(listener);
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "onInit: " + status);
        if (status == TextToSpeech.ERROR) {
            suspendedText = null;
            showTtsIsNotSupported();
            return;
        }
        if (suspendedText != null) {
            speech(suspendedText);
            suspendedText = null;
        }
    }

    private void onUtteranceDone(String utteranceId) {
        Log.d(TAG, "onUtteranceDone: " + utteranceId);
        if (onUtteranceListenerRef.get() != null) {
            onUtteranceListenerRef.get().onUtteranceDone(utteranceId);
        }
    }

    private void onUtteranceError(String utteranceId) {
        Log.d(TAG, "onUtteranceError: " + utteranceId);
        if (onUtteranceListenerRef.get() != null) {
            onUtteranceListenerRef.get().onUtteranceError(utteranceId);
        }
    }
}
