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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class Dice implements Idice {
    private ArrayList<Index> mIndex;
    private HashMap<String, String> mIrreg;

    public Dice() {
        mIndex = new ArrayList<Index>();
    }

    @Override
    public String toString() {
        List<String> fileNames = new ArrayList<String>();
        for (Index index : mIndex) {
            fileNames.add(index.GetFilename());
        }
        return fileNames.toString();
    }

    @Override
    public IdicInfo open(String filename) {
        IdicInfo ret = null;
        final int headerSize = 256;
        Header header; // ヘッダー

        // 辞書の重複をチェック
        for (int i = 0; i < getDicNum(); i++) {
            // 登録済みの辞書であればエラー
            if (getDicInfo(i).GetFilename().compareTo(filename) == 0) {
                return ret;
            }
        }

        File srcFile = new File(filename);
        try {
            FileInputStream srcStream = new FileInputStream(srcFile);
            FileChannel srcChannel = srcStream.getChannel();

            ByteBuffer headerbuff = ByteBuffer.allocate(headerSize);
            try {
                int len = srcChannel.read(headerbuff);
                srcChannel.close(); // ヘッダ読んだら、とりあえず閉じておく

                if (len == headerSize) {
                    header = new Header();

                    if (header.load(headerbuff) != 0) {
                        // Unicode辞書 かつ ver6以上のみ許容
                        if ((header.version & 0xFF00) < 0x0600 ||        // ver6未満
                                header.os != 0x20) {// Unicode以外
                            throw new FileNotFoundException(); // bad dictionary
                        }
                        boolean unicode = true;
                        final Index dic = new Index(filename, header.header_size + header.extheader,
                                header.block_size * header.index_block, header.nindex2, header.index_blkbit,
                                header.block_size, unicode);
                        if (dic != null) {
                            mIndex.add(dic);
                            dic.setIrreg(mIrreg);
                            ret = dic;
                        }
                    }
                }
            } catch (IOException e) {
            }

        } catch (FileNotFoundException e) {
        }

        return ret;
    }

    @Override
    public boolean isEnable(int num) {
        Index idx = mIndex.get(num);
        return (!idx.GetNotuse() /*&& !idx.GetIrreg()*/); // IRREGは通常検索から除外する
    }

    @Override
    public int getDicNum() {
        return mIndex.size();
    }


    @Override
    public void close(IdicInfo info) {
        mIndex.remove(info);
    }

    @Override
    public IdicInfo getDicInfo(int num) {
        return mIndex.get(num);
    }

    @Override
    public IdicInfo getDicInfo(String filename) {
        for (int i = 0; i < mIndex.size(); i++) {
            final IdicInfo di = mIndex.get(i);
            if (di.GetFilename().equals(filename)) {
                return di;
            }
        }
        return null;
    }


    @Override
    public void search(int num, String word) {
        Index idx = mIndex.get(num);
        if (!idx.GetNotuse() /*&& !idx.GetIrreg()*/) { // IRREGは通常検索から除外する
            idx.Search(word);
        }
    }

    @Override
    public boolean isMatch(int num) {
        boolean ret = false;
        Index idx = mIndex.get(num);
        if (!idx.GetNotuse() && idx.IsMatch()) {
            ret = true;
        }
        return ret;
    }

    @Override
    public IdicResult getResult(int num) {
        Index idx = mIndex.get(num);
        return idx.GetResult();
    }

    @Override
    public IdicResult getMoreResult(int num) {
        Index idx = mIndex.get(num);

        return idx.getMoreResult();
    }

    @Override
    public boolean hasMoreResult(int num) {
        Index idx = mIndex.get(num);

        return idx.hasMoreResult(false);
    }

    @Override
    public void swap(IdicInfo info, int dir) {
        int current = mIndex.indexOf(info);
        if (dir == 0) {
            return;
        } else if (dir < 0 && current > 0) {
            mIndex.remove(info);
            mIndex.add(current - 1, (Index) info);
        } else if (dir > 0 && current < mIndex.size() - 1) {
            mIndex.remove(info);
            mIndex.add(current + 1, (Index) info);
        }
    }


    @Override
    public void setIrreg(HashMap<String, String> irreg) {
        mIrreg = irreg;
    }

}
