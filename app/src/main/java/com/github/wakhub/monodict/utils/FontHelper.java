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

package com.github.wakhub.monodict.utils;

import android.graphics.Typeface;

import com.google.common.base.Optional;

import org.androidannotations.annotations.EBean;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wak on 6/26/14.
 */
@EBean(scope = EBean.Scope.Singleton)
public class FontHelper {

    private final Map<String, Typeface> cache = new HashMap<>();

    public Optional<Typeface> loadFromPath(String path) {
        synchronized (cache) {
            if (!cache.containsKey(path)) {
                File fontFile = new File(path);
                if (!fontFile.isFile()) {
                    return Optional.absent();
                }
                Typeface tf = Typeface.createFromFile(fontFile);
                cache.put(path, tf);
            }
            return Optional.of(cache.get(path));
        }
    }

    public void clearCache() {
        cache.clear();
    }
}
