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
import android.util.Log;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.dice.IIndexCacheFile;
import com.github.wakhub.monodict.dice.IdicInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wak on 6/8/14.
 */
public class Dictionary {

    private static final String TAG = Dictionary.class.getSimpleName();

    public static final String EMOJI = "📔";

    private String name;
    private String path;
    private boolean isEnglishIndex;
    private boolean enabled;

    static class Template {
        public String pattern;
        public int resourceDicname;
        public boolean englishFlag;

        public Template(String a, int b, boolean c) {
            pattern = a;
            resourceDicname = b;
            englishFlag = c;
        }
    }

    private final static Template TEMPLATES[] = {
            new Template("/EIJI-([0-9]+)U?.*\\.DIC", R.string.title_dictionary_eijiro, true),
            new Template("/WAEI-([0-9]+)U?.*\\.DIC", R.string.title_dictionary_waeijiro, false),
            new Template("/REIJI([0-9]+)U?.*\\.DIC", R.string.title_dictionary_reijiro, false),
            new Template("/RYAKU([0-9]+)U?.*\\.DIC", R.string.title_dictionary_ryakujiro, false),
            new Template("/PDEJ2005U?.dic", R.string.title_dictionary_pdej, true),
            new Template("/PDEDICTU?.dic", R.string.title_dictionary_edict, false),
            new Template("/PDWD1913U?.dic", R.string.title_dictionary_webster, true),
            new Template("/f2jdic.dic", R.string.title_dictionary_ichirofj, false),
            new Template("/ine([0-9]+)U.dic", R.string.title_dictionary_dienine, false),
    };

    public static String getIndexCacheFilePath(Context context, String path) {
        return context.getCacheDir() + "/" + path.replace("/", ".") + ".idx";
    }

    public static IIndexCacheFile createIndexCacheFile(Context context, String path) {
        final String cachePath = getIndexCacheFilePath(context, path);
        Log.d(TAG, "createIndexCacheFile: " + cachePath);
        return new IIndexCacheFile() {

            @Override
            public FileInputStream getInput() throws FileNotFoundException {
                return new FileInputStream(cachePath);
            }

            @Override
            public FileOutputStream getOutput() throws FileNotFoundException {
                return new FileOutputStream(cachePath);
            }
        };
    }

    public Dictionary() {
        enabled = true;
    }

    public Dictionary(Context context, IdicInfo dicInfo) {
        path = dicInfo.GetFilename();
        name = dicInfo.GetFilename();
        isEnglishIndex = dicInfo.GetEnglish();
        enabled = true;

        for (Template TEMPLATE : TEMPLATES) {
            Pattern p = Pattern.compile(TEMPLATE.pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(name);
            if (m.find()) {
                String dicname;
                if (m.groupCount() > 0) {
                    String edt = m.group(1);
                    dicname = context.getResources().getString(TEMPLATE.resourceDicname, edt);
                } else {
                    dicname = context.getResources().getString(TEMPLATE.resourceDicname);
                }
                name = dicname;
                isEnglishIndex = TEMPLATE.englishFlag;
            }
        }
    }

    public boolean equals(Dictionary dictionary) {
        return dictionary.getPath().equals(path);
    }

    public IIndexCacheFile createIndexCacheFile(Context context) {
        return Dictionary.createIndexCacheFile(context, path);
    }

    public String getIndexCacheFilePath(Context context) {
        return Dictionary.getIndexCacheFilePath(context, path);
    }

    public String getName() {
        return name;
    }

    public String getNameWithEmoji() {
        return EMOJI + name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public boolean isEnglishIndex() {
        return isEnglishIndex;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
