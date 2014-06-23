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

import java.util.ArrayList;


final class Result extends ArrayList<Element> implements IdicResult {

    private static final long serialVersionUID = 1L;

    @Override
    public int getCount() {
        return size();
    }

    @Override
    public final String getIndex(int idx) {
        return get(idx).mIndex;
    }

    @Override
    public final String getDisp(int idx) {
        return get(idx).mDisp;
    }

    @Override
    public byte getAttr(int idx) {
        return get(idx).mAttr;
    }

    @Override
    public final String getTrans(int idx) {
        return get(idx).mTrans;
    }

    @Override
    public final String getPhone(int idx) {
        return get(idx).mPhone;
    }

    @Override
    public final String getSample(int idx) {
        return get(idx).mSample;
    }

    @Override
    public IdicInfo getDicInfo(int idx) {
        return get(idx).mDic;
    }
}
