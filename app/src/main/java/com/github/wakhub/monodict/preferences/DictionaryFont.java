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
package com.github.wakhub.monodict.preferences;

import android.content.Context;
import android.graphics.Typeface;

import com.github.wakhub.monodict.utils.FontHelper_;
import com.google.common.base.Optional;
import com.google.gson.annotations.Expose;

/**
 * Created by wak on 6/8/14.
 */
public class DictionaryFont {

    private static final String TAG = DictionaryFont.class.getSimpleName();

    private static final Typeface[] systemTypefaces = new Typeface[]{
            Typeface.SANS_SERIF,
            Typeface.SERIF,
            Typeface.MONOSPACE
    };

    public static DictionaryFont getDefaultIndexFont() {
        return new DictionaryFont(Typeface.BOLD, 20);
    }

    public static DictionaryFont getDefaultPhoneFont() {
        return new DictionaryFont(Typeface.NORMAL, 14);
    }

    public static DictionaryFont getDefaultTransFont() {
        return new DictionaryFont(Typeface.NORMAL, 14);
    }

    public static DictionaryFont getDefaultSampleFont() {
        return new DictionaryFont(Typeface.ITALIC, 14);
    }

    @Expose
    private String path = "";

    @Expose
    private int style = Typeface.NORMAL;

    @Expose
    private int size = 14;

    private Typeface typefaceCache;

    public DictionaryFont(int style, int size) {
        this.style = style;
        this.size = size;
    }

    public DictionaryFont(String path) {
        this.path = path;
    }

    public String getName() {
        if (path.isEmpty()) {
            return "Default";
        }
        String[] splitPath = path.split("/");
        return splitPath[splitPath.length - 1];
    }

    private Typeface getBaseTypeface(Context context) {
        if (!path.isEmpty()) {
            Optional<Typeface> optTypeface = FontHelper_.getInstance_(context).loadFromPath(path);
            if (optTypeface.isPresent()) {
                return optTypeface.get();
            }
        }
        return Typeface.SANS_SERIF;
    }

    public Typeface getTypeface(Context context) {
        if (typefaceCache == null) {
            typefaceCache = getBaseTypeface(context);
        }
        return typefaceCache;
    }

    public String getPath() {
        return path;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getSize() {
        return size;
    }
}
