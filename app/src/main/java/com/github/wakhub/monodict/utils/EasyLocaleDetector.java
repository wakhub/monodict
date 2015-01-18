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

import java.util.Locale;

/**
 * Created by wak on 6/26/14.
 */
public class EasyLocaleDetector {

    private static final String HIRAGANA_KATAKANA_TEST = ".*[\\p{InHiragana}\\p{InKatakana}]+.*";
    private static final String CHINESE_CHARACTERS_TEST = ".*[\\p{InCJKUnifiedIdeographs}]+.*";
    private static final String HANGEUL_TEST = ".*[가-힣]+.*";
    private static final String THAI_TEST = ".*[\\p{InThai}]+.*";
    private static final String HINDI_TEST = ".*[\\p{InDevanagari}]+.*";
    private static final String ACCENT_CODES_TEST = ".*[ÉÀÈÙÂÊÎÔÛËÏÜÇŒÆéàèùâêîôûëïüçœæ]+.*";
    private static final String CYRILLIC_TEST = ".*[\\p{InCyrillic}]+.*";

    public static final Locale THAI_LOCALE = new Locale("tha");
    public static final Locale RUSSIAN_LOCALE = new Locale("rus");
    public static final Locale HINDI_LOCALE = new Locale("hin");

    public static Locale detectLocale(String text) {
        String[] split = text.split("\\n");
        text = split[0].trim();

        if (text.matches(HIRAGANA_KATAKANA_TEST) || text.matches(CHINESE_CHARACTERS_TEST)) {
            return Locale.JAPANESE;
        }
        if (text.matches(HANGEUL_TEST)) {
            return Locale.KOREAN;
        }
        if (text.matches(THAI_TEST)) {
            return THAI_LOCALE;
        }
        if (text.matches(ACCENT_CODES_TEST)) {
            return Locale.FRENCH;
        }
        if (text.matches(CYRILLIC_TEST)) {
            return RUSSIAN_LOCALE;
        }
        if (text.matches(HINDI_TEST)) {
            return HINDI_LOCALE;
        }
        return Locale.ENGLISH;
    }
}
