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

package com.github.wakhub.monodict.ui;

import android.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.activity.bean.ActivityHelper_;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

/**
 * Created by wak on 6/28/14.
 */
@EFragment(R.layout.fragment_translate_panel)
public class TranslatePanelFragment extends Fragment {

    private static final String TAG = TranslatePanelFragment.class.getSimpleName();

    private static final int TRANSLATE_PANEL_DURATION = 150;

    @ViewById
    TextView displayText;

    @ViewById
    TextView translateText;

    @ViewById
    TextView dictionaryNameText;

    private DicItemListView.Data data;

    private Listener listener;

    public interface Listener {
        void onClickTranslatePanelAddToFlashcardButton(DicItemListView.Data data);

        void onClickTranslatePanelSpeechButton(DicItemListView.Data data);
    }

    public void show() {
        Log.d(TAG, "show");
        View view = getView();
        if (view == null) {
            return;
        }
        view.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.makeInChildBottomAnimation(getActivity());
        animation.setDuration(TRANSLATE_PANEL_DURATION);
        view.startAnimation(animation);
    }

    public void hide() {
        Log.d(TAG, "hide");
        View view = getView();
        if (view == null) {
            return;
        }
        view.setVisibility(View.GONE);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(DicItemListView.Data data) {
        this.data = data;
        displayText.setText(data.Index.toString());
        translateText.setScrollY(0);
        translateText.setText(data.Trans.toString());
    }

    public void setDictionaryName(String dictionaryName) {
        dictionaryNameText.setText(dictionaryName);
    }

    @Click(R.id.more_button)
    void onClickTranslatePanelMoreButton() {
        ActivityHelper_.getInstance_(getActivity()).searchOnMainActivity(data.Index.toString());
    }

    @Click(R.id.speech_button)
    void onClickTranslatePanelSpeechButton() {
        listener.onClickTranslatePanelSpeechButton(data);
    }

    @Click(R.id.close_button)
    void onClickTranslatePanelCloseButton() {
        hide();
    }

    @Click(R.id.add_to_flashcard_button)
    void onClickTranslatePanelAddToFlashcardButton() {
        listener.onClickTranslatePanelAddToFlashcardButton(data);
    }

    // Possible Memory Leaks and missing onDestroyView cleanup
    // https://github.com/excilys/androidannotations/issues/933
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        displayText = null;
        translateText = null;
        dictionaryNameText = null;
        listener = null;
    }
}
