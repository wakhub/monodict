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
package com.github.wakhub.monodict.preferences;

import android.content.Context;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

/**
 * Created by wak on 6/7/14.
 */
@SharedPref(value = SharedPref.Scope.UNIQUE, mode = Context.MODE_PRIVATE)
public interface Preferences {

    // { System

    @DefaultInt(0)
    int lastVersionCode();

    // System }

    // { Settings

    @DefaultBoolean(false)
    boolean clipboardSearch();

    @DefaultBoolean(false)
    boolean fastScroll();

    // Configuration.ORIENTATION_xxx
    @DefaultString("0")
    String orientation();

    @DefaultString("English")
    String ttsDefaultLocale();

    @DefaultString("Japanese")
    String ttsLanguageForTranslate();

    @DefaultString("")
    String ttsDefaultEngine();

    /**
     * @see com.github.wakhub.monodict.preferences.Dictionaries
     */
    @DefaultString("{\"dictionaries\": []}")
    String dictionaries();

    // Settings }

    // { Activity state

    /**
     * @see com.github.wakhub.monodict.preferences.MainActivityState
     */
    @DefaultString("{}")
    String mainActivityState();

    /**
     * @see com.github.wakhub.monodict.preferences.FlashcardActivityState
     */
    @DefaultString("{}")
    String flashcardActivityState();

    /**
     * @see com.github.wakhub.monodict.preferences.BrowserActivityState
     */
    @DefaultString("{}")
    String browserActivityState();

    /**
     * @see com.github.wakhub.monodict.preferences.DirectorySelectorActivityState
     */
    @DefaultString("{}")
    String directorySelectorActivityState();

    /**
     * @see com.github.wakhub.monodict.preferences.FileSelectorActivityState
     */
    @DefaultString("{}")
    String fileSelectorActivityState();

    /**
     * @see com.github.wakhub.monodict.preferences.DictionaryFileSelectorActivityState
     */
    @DefaultString("{}")
    String dictionaryFileSelectorActivityState();

    // Activity state }
}
