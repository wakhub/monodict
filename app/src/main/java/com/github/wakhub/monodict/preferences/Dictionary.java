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
import com.google.common.base.MoreObjects;
import com.google.gson.annotations.Expose;

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

    @Expose
    private String name;

    @Expose
    private String path;

    @Expose
    private boolean isEnglishIndex;

    @Expose
    private boolean enabled;

    @Expose
    private int maxResults = 10;

    @Expose
    private DictionaryFont indexFont;

    @Expose
    private DictionaryFont phoneFont;

    @Expose
    private DictionaryFont transFont;

    @Expose
    private DictionaryFont sampleFont;

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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("path", path)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Dictionary) {
            return ((Dictionary) o).getPath().equals(path);
        }
        return super.equals(o);
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

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DictionaryFont getIndexFont() {
        if (indexFont == null) {
            indexFont = DictionaryFont.getDefaultIndexFont();
        }
        return indexFont;
    }

    public void setSampleFont(DictionaryFont sampleFont) {
        this.sampleFont = sampleFont;
    }

    public void setTransFont(DictionaryFont transFont) {
        this.transFont = transFont;
    }

    public void setPhoneFont(DictionaryFont phoneFont) {
        this.phoneFont = phoneFont;
    }

    public void setIndexFont(DictionaryFont indexFont) {
        this.indexFont = indexFont;
    }

    public DictionaryFont getPhoneFont() {
        if (phoneFont == null) {
            phoneFont = DictionaryFont.getDefaultPhoneFont();
        }
        return phoneFont;
    }

    public DictionaryFont getTransFont() {
        if (transFont == null) {
            transFont = DictionaryFont.getDefaultTransFont();
        }
        return transFont;
    }

    public DictionaryFont getSampleFont() {
        if (sampleFont == null) {
            sampleFont = DictionaryFont.getDefaultSampleFont();
        }
        return sampleFont;
    }
}
