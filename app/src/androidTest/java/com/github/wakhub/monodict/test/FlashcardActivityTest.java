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
package com.github.wakhub.monodict.test;

import android.test.ActivityInstrumentationTestCase2;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.FlashcardActivity_;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.DatabaseOpenHelper;
import com.github.wakhub.monodict.preferences.FlashcardActivityState;
import com.github.wakhub.monodict.preferences.FlashcardActivityState_;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.jayway.android.robotium.solo.Solo;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wak on 5/9/14.
 */
public class FlashcardActivityTest extends ActivityInstrumentationTestCase2<FlashcardActivity_> {

    private static Solo solo;

    Dao<Card, Long> cardDao;

    public FlashcardActivityTest() {
        super(FlashcardActivity_.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());

        FlashcardActivityState state = FlashcardActivityState_.getInstance_(solo.getCurrentActivity());
        state.setBox(1);

        DatabaseOpenHelper openHelper = new DatabaseOpenHelper(solo.getCurrentActivity());
        cardDao = openHelper.getDao(Card.class);
        cardDao.queryForAll().size();
        List<List<String>> cards = Arrays.asList(
                Arrays.asList("111", "AAA", "1"),
                Arrays.asList("222", "BBB", "1"),
                Arrays.asList("333", "CCC", "1"),
                Arrays.asList("444", "DDD", "1"),
                Arrays.asList("555", "EEE", "1"),
                Arrays.asList("666", "FFF", "2"),
                Arrays.asList("777", "GGG", "2"),
                Arrays.asList("888", "HHH", "3"),
                Arrays.asList("999", "III", "3")
        );
        for (List<String> card : cards) {
            createCardForTest(card.get(0), card.get(1), Integer.valueOf(card.get(2)));
        }
    }

    void createCardForTest(String display, String translate, int box) throws SQLException {
        Card card = new Card();
        card.setDisplay("__test__" + display);
        card.setTranslate("__test__" + translate);
        card.setBox(box);
        card.setTags(Arrays.asList("test"));
        cardDao.create(card);
    }

    @Override
    public void tearDown() throws Exception {
        DeleteBuilder<Card, Long> deleteBuilder = cardDao.deleteBuilder();
        deleteBuilder.where().like(Card.Column.TAGS, "test");
        deleteBuilder.delete();
    }

    public void testActionBar() throws Exception {
        solo.clickOnActionBarItem(R.id.action_shuffle);
        solo.clickOnActionBarItem(R.id.action_shuffle);
        solo.clickOnActionBarItem(R.id.action_shuffle);
        solo.clickOnActionBarItem(R.id.action_shuffle);
        solo.clickOnActionBarItem(R.id.action_order_alphabetically);
        solo.clickOnActionBarItem(R.id.action_order_alphabetically);
        solo.clickOnActionBarItem(R.id.action_shuffle);
        solo.clickOnActionBarItem(R.id.action_order_alphabetically);
        solo.clickOnActionBarItem(R.id.action_shuffle);
        solo.clickOnActionBarItem(R.id.action_order_alphabetically);
    }

    public void testList() throws Exception {
        solo.clickInList(1);
        solo.goBack();
        solo.clickInList(2);
        solo.goBack();
        solo.clickInList(3);
        solo.goBack();
    }
}

