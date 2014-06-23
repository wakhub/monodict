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
package com.github.wakhub.monodict.dice;

import java.util.HashMap;

// TODO: Remove useless properties
public interface IdicInfo {
    String GetFilename();

    int GetSearchMax();

    void SetSearchMax(int m);

    void SetAccent(boolean b);

    boolean GetAccent();

    void SetEnglish(boolean b);

    boolean GetEnglish();

    void SetNotuse(boolean b);

    boolean GetNotuse();

    void SetIndexFont(String b);

    String GetIndexFont();

    void SetIndexSize(int b);

    int GetIndexSize();

    void SetTransFont(String b);

    String GetTransFont();

    void SetTransSize(int b);

    int GetTransSize();

    void SetPhonetic(boolean b);

    boolean GetPhonetic();

    void SetPhoneticFont(String b);

    String GetPhoneticFont();

    void SetPhoneticSize(int b);

    int GetPhoneticSize();

    void SetSample(boolean b);

    boolean GetSample();

    void SetSampleFont(String b);

    String GetSampleFont();

    void SetSampleSize(int b);

    int GetSampleSize();

    void SetDicName(String b);

    String GetDicName();

    void setIrreg(HashMap<String, String> irreg);

    String getIrreg(String key);

    public boolean readIndexBlock(IIndexCacheFile indexcache);


}
