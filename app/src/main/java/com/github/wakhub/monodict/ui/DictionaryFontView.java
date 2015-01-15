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

package com.github.wakhub.monodict.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.DictionaryFont;
import com.google.common.base.Optional;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by wak on 6/21/14.
 */
@EViewGroup(R.layout.view_dictionary_font)
public class DictionaryFontView extends RelativeLayout {

    public interface Listener {
        void onDictionaryFontViewClickResetButton();

        void onDictionaryFontViewClickSettingsButton(DictionaryFont font);

        void onDictionaryFontViewChangeFont(DictionaryFont font);
    }

    @ViewById
    TextView text;

    @ViewById
    TextView fontNameText;

    @ViewById
    TextView loremText;

    @ViewById
    ImageButton resetButton;

    @ViewById
    ImageButton boldButton;

    @ViewById
    ImageButton italicButton;

    @ViewById
    ImageButton sizeButton;

    @ViewById
    ImageButton settingsButton;

    private DictionaryFont font;

    private Optional<Listener> optListener = Optional.absent();

    public DictionaryFontView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Listener listener) {
        this.optListener = Optional.of(listener);
    }

    private int getSelectedSizeIndex() {
        int index = -1;
        for (String sizeText : getResources().getStringArray(R.array.font_sizes)) {
            index++;
            int size = Integer.valueOf(sizeText);
            if (size == font.getSize()) {
                return index;
            }
        }
        return index;
    }

    @AfterViews
    void afterViews() {
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optListener.isPresent()) {
                    optListener.get().onDictionaryFontViewClickResetButton();
                }
            }
        });

        boldButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int style = font.getStyle();
                if ((style & Typeface.BOLD) > 0) {
                    font.setStyle(style ^ Typeface.BOLD);
                } else {
                    font.setStyle(style | Typeface.BOLD);
                }
                refresh();
                if (optListener.isPresent()) {
                    optListener.get().onDictionaryFontViewChangeFont(font);
                }
            }
        });

        italicButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int style = font.getStyle();
                if ((style & Typeface.ITALIC) > 0) {
                    font.setStyle(style ^ Typeface.ITALIC);
                } else {
                    font.setStyle(style | Typeface.ITALIC);
                }
                refresh();
                if (optListener.isPresent()) {
                    optListener.get().onDictionaryFontViewChangeFont(font);
                }
            }
        });

        sizeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(getContext())
                        .items(R.array.font_sizes)
                        .itemsCallbackSingleChoice(getSelectedSizeIndex(), new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                font.setSize(Integer.valueOf(charSequence.toString()));
                                refresh();
                                if (optListener.isPresent()) {
                                    optListener.get().onDictionaryFontViewChangeFont(font);
                                }
                            }
                        })
                        .positiveText(android.R.string.ok)
                        .show();
            }
        });

        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optListener.isPresent()) {
                    optListener.get().onDictionaryFontViewClickSettingsButton(font);
                }
            }
        });
    }

    private void refresh() {
        if (font == null) {
            return;
        }
        fontNameText.setText(font.getName());
        loremText.setTypeface(font.getTypeface(getContext()), font.getStyle());
        loremText.setTextSize(font.getSize());
    }

    public void setText(String textValue) {
        text.setText(textValue);
    }

    public void setDictionaryFont(DictionaryFont font) {
        this.font = font;
        refresh();
    }
}
