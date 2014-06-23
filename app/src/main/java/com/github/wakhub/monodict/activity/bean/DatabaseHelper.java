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

package com.github.wakhub.monodict.activity.bean;

import android.app.Activity;

import com.github.wakhub.monodict.db.Bookmark;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.DatabaseOpenHelper;
import com.github.wakhub.monodict.db.Model;
import com.github.wakhub.monodict.preferences.Dictionaries;
import com.github.wakhub.monodict.preferences.Dictionary;
import com.github.wakhub.monodict.ui.DicItemListView;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.QueryBuilder;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wak on 6/14/14.
 */
@EBean
public class DatabaseHelper {

    //private static final int REQUEST_CODE = 20200;

    @RootContext
    Activity activity;

    @Bean
    Dictionaries dictionaries;

    @OrmLiteDao(helper = DatabaseOpenHelper.class, model = Card.class)
    Dao<Card, Long> cardDao;

    @OrmLiteDao(helper = DatabaseOpenHelper.class, model = Bookmark.class)
    Dao<Bookmark, Long> bookmarkDao;

    // { Card

    public Card createCard(DicItemListView.Data data) throws SQLException {
        dictionaries.reload();
        String display = data.Index.toString().trim();
        String translate = data.Trans.toString().trim();
        Dictionary dictionary = dictionaries.getDictionary(data.getDic());
        return createCard(display, translate, dictionary.getName());
    }

    public Card createCard(String display, String translate, String dictionary) throws SQLException {
        return createCard(display, translate, dictionary, 1);
    }

    public Card createCard(Card card) throws SQLException {
        cardDao.create(card);
        return card;
    }

    public Card createCard(String display, String translate, String dictionary, int box) throws SQLException {
        Card card = getCardByDisplay(display);
        if (card != null) {
            return card;
        }
        card = new Card();
        card.setDisplay(display);
        card.setTranslate(translate);
        card.setDictionary(dictionary != null ? dictionary : "");
        card.setBox(box);
        cardDao.create(card);
        return card;
    }

    public void updateCard(Card card) throws SQLException {
        cardDao.update(card);
    }

    public Card getCardByDisplay(String display) throws SQLException {
        display = display.trim();
        QueryBuilder<Card, Long> queryBuilder = cardDao.queryBuilder();
        queryBuilder.where().like(Card.Column.DISPLAY, display);
        return queryBuilder.queryForFirst();
    }

    public List<Card> findAllCards() throws SQLException {
        return cardDao.queryForAll();
    }

    public List<Card> findCardInBoxAlphabetically(int box) throws SQLException {
        QueryBuilder<Card, Long> queryBuilder = cardDao.queryBuilder();
        queryBuilder.where().eq(Card.Column.BOX, box);
        queryBuilder.orderBy(Card.Column.DISPLAY, true);
        return queryBuilder.query();
    }

    public List<Card> findCardInBoxRandomly(int box, int randomSeed) throws SQLException {
        QueryBuilder<Card, Long> queryBuilder = cardDao.queryBuilder();
        queryBuilder.where().eq(Card.Column.BOX, box);
        queryBuilder.orderByRaw(Model.getRandomOrderSql(randomSeed));
        return queryBuilder.query();
    }

    public void deleteCard(Card card) throws SQLException {
        cardDao.delete(card);
    }

    public void deleteAllCards() throws SQLException {
        cardDao.deleteBuilder().delete();
    }

    // Card }

    // { Bookmark

    public Map<Integer, Integer> getCountsForBoxes() throws SQLException {
        Map<Integer, Integer> countsForBoxes = new HashMap<Integer, Integer>();
        GenericRawResults results = cardDao.queryBuilder()
                .groupBy(Card.Column.BOX)
                .selectRaw(String.format("%s, count(%s) AS count", Card.Column.BOX, Card.Column.ID))
                .queryRaw();
        List<String[]> rows = results.getResults();
        for (String[] row : rows) {
            countsForBoxes.put(Integer.valueOf(row[0]), Integer.valueOf(row[1]));
        }
        return countsForBoxes;
    }

    public Bookmark createBookmark(String url, String title, String description) throws SQLException {
        Bookmark bookmark = new Bookmark();
        bookmark.setUrl(url.trim());
        bookmark.setTitle(title.trim());
        bookmark.setDescription(description.trim());
        bookmarkDao.create(bookmark);
        return bookmark;
    }

    public List<Bookmark> findAllBookmarks() throws SQLException {
        QueryBuilder<Bookmark, Long> queryBuilder = bookmarkDao.queryBuilder();
        queryBuilder.orderBy(Bookmark.Column.ID, false);
        return queryBuilder.query();
    }

    public void deleteBookmark(Bookmark bookmark) throws SQLException {
        bookmarkDao.delete(bookmark);
    }

    // Bookmark }
}
