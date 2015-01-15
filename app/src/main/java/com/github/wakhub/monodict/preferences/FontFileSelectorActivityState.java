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

import org.androidannotations.annotations.EBean;
import org.androidannotations.api.sharedpreferences.StringPrefEditorField;

/**
 * Created by wak on 6/28/14.
 */
@EBean
public class FontFileSelectorActivityState extends AbsFileManagerActivityState {

    @Override
    public String loadJson() {
        return preferences.fontFileSelectorActivityState().get();
    }

    @Override
    public StringPrefEditorField<?> edit() {
        return preferences.edit().fontFileSelectorActivityState();
    }
}
