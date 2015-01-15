/*
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

package com.github.wakhub.monodict.search;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.dice.DiceFactory;
import com.github.wakhub.monodict.dice.IdicInfo;
import com.github.wakhub.monodict.dice.IdicResult;
import com.github.wakhub.monodict.dice.Idice;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionaries_;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.ui.DicItemListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * TODO: refactoring
 * Created by wak on 6/17/14.
 */
public final class DictionaryService extends Service implements SearchThread.Listener {

    private static final String TAG = DictionaryService.class.getSimpleName();

    // TODO: same as Data.MODE ?
    public static final int DISP_MODE_RESULT = 0;
    public static final int DISP_MODE_NORESULT = 3;
    public static final int DISP_MODE_HEADER = 5;
    public static final int DISP_MODE_END = 10;

    private final Idice dice = DiceFactory.getInstance();

    private final HashMap<String, String> irreg = new HashMap<String, String>();

    private final Binder binder;

    private SearchThread searchThread;

    private WeakReference<Listener> listenerRef = new WeakReference<Listener>(null);

    private boolean isInitialized = false;

    private ArrayList<WeakReference<DicItemListView.Data>> resultData = new ArrayList<WeakReference<DicItemListView.Data>>();

    private Dictionaries dictionaries;

    public interface Listener {

        void onDictionaryServiceInitialized();

        void onDictionaryServiceUpdateDictionaries();

        void onDictionaryServiceResult(String query, ArrayList<DicItemListView.Data> result);

        void onDictionaryServiceError(String query, Exception e);
    }

    final static class DictionaryServiceBinder extends Binder {

        private WeakReference<DictionaryService> dictionaryServiceRef = new WeakReference<DictionaryService>(null);

        public DictionaryServiceBinder(DictionaryService service) {
            super();
            dictionaryServiceRef = new WeakReference<>(service);
        }

        DictionaryService getService() {
            return dictionaryServiceRef.get();
        }

        void search(String query) {
            Log.d(TAG, "search");
            if (dictionaryServiceRef.get() != null) {
                dictionaryServiceRef.get().searchInThread(query);
            }
        }

        void delete(Dictionary dictionary) {
            Log.d(TAG, "delete");
            if (dictionaryServiceRef.get() != null) {
                dictionaryServiceRef.get().delete(dictionary);
            }
        }

        void swap(Dictionary dictionary, int direction) {
            Log.d(TAG, "swap");
            if (dictionaryServiceRef.get() != null) {
                dictionaryServiceRef.get().swap(dictionary, direction);
            }
        }

        void reload() {
            Log.d(TAG, "reload");
            if (dictionaryServiceRef.get() != null) {
                dictionaryServiceRef.get().reload();
            }
        }

        void setListener(Listener listener) {
            DictionaryService service = getService();
            if (service == null) {
                return;
            }
            service.listenerRef = new WeakReference<>(listener);
            if (service.isInitialized) {
                listener.onDictionaryServiceInitialized();
            }
        }

        void removeListener() {
            if (getService() != null) {
                getService().listenerRef = new WeakReference<>(null);
            }
        }
    }

    public DictionaryService() {
        super();
        binder = new DictionaryServiceBinder(this);
    }

    @Override
    public void onCreate() {
        dictionaries = Dictionaries_.getInstance_(getBaseContext());
        super.onCreate();
        initDice();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // 英語向けIRREG読込
    private HashMap<String, String> loadIrreg() {
        final String name = "IrregDic.txt";

        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(getAssets().open(name)));

            String str;
            while ((str = in.readLine()) != null) {
                int s = str.indexOf('\t');
                if (s != -1) {
                    String s0 = str.substring(0, s);
                    String s1 = str.substring(s + 1);
                    irreg.put(s0, s1);
                }
            }
            in.close();
            Log.i(TAG, "Open OK:" + name);
            dice.setIrreg(irreg);
        } catch (IOException e) {
            Log.i(TAG, "Open NG:" + name);
        }
        return irreg;
    }

    private synchronized void initDice() {
        dictionaries.reload();
        Log.d(TAG, String.format(
                "dice: %s\ndictionaries: %s",
                dice.toString(),
                dictionaries.toString()));

        loadIrreg();
        for (int i = 0; i < dictionaries.getDictionaryCount(); i++) {
            Dictionary dictionary = dictionaries.getDictionary(i);
            String path = dictionary.getPath();
            IdicInfo dicinfo = dice.open(path);
            if (dicinfo == null) {
                Log.d(TAG, "Already exists: " + path);
                dicinfo = dice.getDicInfo(path);
                if (dicinfo == null) {
                    Log.e(TAG, "Couldn't load dictionary: " + path);
                } else {
                    dicinfo.SetDicName(dictionary.getName());
                    dicinfo.SetEnglish(dictionary.isEnglishIndex());
                    dicinfo.SetNotuse(!dictionary.isEnabled());
                }
            } else {
                Log.d(TAG, "Opened: " + path);
                if (!dicinfo.readIndexBlock(dictionary.createIndexCacheFile(this))) {
                    Log.d(TAG, "Failed to create index");
                    dice.close(dicinfo);
                } else {
                    dicinfo.SetDicName(dictionary.getName());
                    dicinfo.SetEnglish(dictionary.isEnglishIndex());
                    dicinfo.SetNotuse(!dictionary.isEnabled());
                }
            }
        }
        if (!isInitialized) {
            isInitialized = true;
            if (listenerRef.get() != null) {
                listenerRef.get().onDictionaryServiceInitialized();
            }
        }
        Log.d(TAG, String.format(
                "Initialized dice: %s",
                dice.toString()));
        if (listenerRef.get() != null) {
            listenerRef.get().onDictionaryServiceUpdateDictionaries();
        }
    }

    private void searchInThread(String query) {
        Log.d(TAG, "searchInThread: " + query);
        if (!isInitialized) {
            return;
        }
        if (searchThread != null) {
            searchThread.interrupt();
            try {
                searchThread.join();
            } catch (InterruptedException e) {
                // pass
            }
            searchThread = null;
        }
        final String searchQuery = query.toLowerCase();
        searchThread = new SearchThread(this, dice, searchQuery, 10);
        searchThread.start();
    }

    private void delete(Dictionary dictionary) {
        final IdicInfo dicInfo = dice.getDicInfo(dictionary.getPath());
        if (dicInfo != null) {
            dice.close(dicInfo);
        }
        String cacheFilePath = dictionary.getIndexCacheFilePath(this);
        if (!new File(cacheFilePath).delete()) {
            Log.d(TAG, String.format("Failed to delete dictionary: %s", cacheFilePath));
            return;
        }

        MonodictApp.getEventBus().post(new DictionaryDeletedEvent(dictionary));
    }

    private void swap(Dictionary dictionary, int direction) {
        final IdicInfo dicInfo = dice.getDicInfo(dictionary.getPath());
        dice.swap(dicInfo, direction);
        MonodictApp.getEventBus().post(new DictionarySwappedEvent(dictionary, direction));
    }

    private void reload() {
        initDice();
    }

    // { SearchThread.Interface

    @Override
    public void generateViewForSearch(int mode, int dic, IdicResult pr, ArrayList<DicItemListView.Data> result, int po) {
        switch (mode) {
            case DISP_MODE_RESULT: {
                int count = pr.getCount();
                for (int i = 0; i < count; i++) {
                    DicItemListView.Data data = new DicItemListView.Data(DicItemListView.Data.WORD, dic);

                    String idx = pr.getDisp(i);
                    data.Index = idx;
                    if (idx == null || idx.length() == 0) {
                        data.Index = pr.getIndex(i);
                    }
                    data.Phone = pr.getPhone(i);
                    data.Trans = pr.getTrans(i);
                    data.Sample = pr.getSample(i);

                    if (i == count - 1) {
                        data.LastItem = true;
                    }

                    result.add(data);
                }

                // 結果がまだあるようならmoreボタンを表示
//                    if (dice.hasMoreResult(dic)) {
//                        DicItemListView.Data data = new DicItemListView.Data(DicItemListView.Data.MORE, dic);

//                        data.Index = moreButtonString;

//                            result.add(data);
                break;
            }
            case DISP_MODE_HEADER: {
                DicItemListView.Data data = new DicItemListView.Data(DicItemListView.Data.FOOTER, dic);

                IdicInfo info = dice.getDicInfo(dic);
                Dictionary dictionary = dictionaries.getDictionaryByPath(info.GetFilename());
                if (dictionary != null) {
                    data.Index = dictionary.getName();
                } else {
                    data.Index = info.GetDicName();
                }
                result.add(data);
                break;
            }
            case DISP_MODE_NORESULT: {
                DicItemListView.Data data = new DicItemListView.Data(DicItemListView.Data.NONE, 0);
                data.Index = getResources().getString(R.string.message_no_result);
                result.add(data);
                break;
            }
            case DISP_MODE_END: {
                DicItemListView.Data data = new DicItemListView.Data(DicItemListView.Data.END, 0);
                result.add(data);
                break;
            }
        }
    }

    @Override
    public void onSearchFinished(String query, ArrayList<DicItemListView.Data> result) {
        resultData.clear();
        for (DicItemListView.Data d : result) {
            resultData.add(new WeakReference<>(d));
        }

        if (result.size() < 1) {
            return;
        }
        if (listenerRef.get() != null) {
            listenerRef.get().onDictionaryServiceResult(query, result);
        }
    }

    @Override
    public void onSearchError(String query, Exception e) {
        if (listenerRef.get() != null) {
            listenerRef.get().onDictionaryServiceError(query, e);
        }
    }

    // SearchThread.Interface }

    public static final class DictionaryDeletedEvent {

        private final Dictionary dictionary;

        public DictionaryDeletedEvent(Dictionary dictionary) {
            this.dictionary = dictionary;
        }

        public Dictionary getDictionary() {
            return dictionary;
        }
    }

    public static final class DictionarySwappedEvent {

        private final Dictionary dictionary;

        private final int direction;

        public DictionarySwappedEvent(Dictionary dictionary, int direction) {
            this.dictionary = dictionary;
            this.direction = direction;
        }

        public Dictionary getDictionary() {
            return dictionary;
        }

        public int getDirection() {
            return direction;
        }
    }
}
