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

final public class Element {
    public IdicInfo mDic = null;
    public byte mAttr = 0;
    public String mIndex = null;
    public String mDisp = null;
    public String mTrans = null;
    public String mSample = null;
    public String mPhone = null;

    public Element(IdicInfo parent) {
        mDic = parent;
    }

    public Element() {
    }

}

