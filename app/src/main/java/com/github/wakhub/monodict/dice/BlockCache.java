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

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockCache extends LinkedHashMap<Integer, SoftReference<byte[]>> {

    private static final long serialVersionUID = -1140373677231662504L;

    private final static int CACHESIZE = 1000;

    /**
     * 一番古いエントリを消すかどうか
     *
     * @param entry 最も古いエントリ
     */
    protected boolean removeEldestEntry(Map.Entry<Integer, SoftReference<byte[]>> envtry) {
        if (size() > CACHESIZE) {
            return true;
        } else {
            return false;
        }
    }

    public byte[] getBuff(int key) {
        final SoftReference<byte[]> ref = get(key);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    public void putBuff(int key, byte[] data) {
        put(key, new SoftReference<byte[]>(data));
    }


}
