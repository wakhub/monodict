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
package com.github.wakhub.monodict.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.db.Card;

/**
 * Created by wak on 6/12/14.
 */
public class CardDialog extends Dialog {

    private static final int DURATION = 150;

    private final View innerContentView;

    private final TextView displayText;

    private final TextView translateText;

    private final TextView dictionaryText;

    private final ImageButton deleteButton;

    private final ImageButton backButton;

    private final ImageButton forwardButton;

    private final Button keepButton;

    private final Card card;

    private OnCardDialogListener listener;

    private final Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            CardDialog.this.dismiss();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    public interface OnCardDialogListener {
        boolean onCardDialogClickBackButton(Card card);

        boolean onCardDialogClickForwardButton(Card card);

        boolean onCardDialogClickDeleteButton(Card card);
    }

    public CardDialog(Context context, final Card card) {
        super(context, R.style.AppTheme_Flashcard_CardDialog);

        this.card = card;

        setContentView(R.layout.dialog_card);

        innerContentView = findViewById(R.id.dialog_content);
        displayText = (TextView) findViewById(R.id.display_text);
        dictionaryText = (TextView) findViewById(R.id.dictionary_text);
        translateText = (TextView) findViewById(R.id.translate_text);
        deleteButton = (ImageButton) findViewById(R.id.delete_button);
        backButton = (ImageButton) findViewById(R.id.back_button);
        forwardButton = (ImageButton) findViewById(R.id.forward_button);
        keepButton = (Button) findViewById(R.id.keep_button);

        initViews();
    }

    private void initViews() {
        displayText.setText(card.getDisplay());

        String dictionary = card.getDictionary();
        if (dictionary == null || dictionary.isEmpty()) {
            dictionaryText.setVisibility(View.GONE);
        } else {
            dictionaryText.setVisibility(View.VISIBLE);
            dictionaryText.setText(card.getDictionary());
        }

        translateText.setText(card.getTranslate());
        translateText.setMovementMethod(ScrollingMovementMethod.getInstance());

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener.onCardDialogClickDeleteButton(getCard())) {
                    dismiss();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener.onCardDialogClickBackButton(getCard())) {
                    Animation animation = AnimationUtils.makeOutAnimation(getContext(), false);
                    animation.setAnimationListener(animationListener);
                    animation.setDuration(DURATION);
                    innerContentView.startAnimation(animation);
                }
            }
        });

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener.onCardDialogClickForwardButton(getCard())) {
                    Animation animation = AnimationUtils.makeOutAnimation(getContext(), true);
                    animation.setAnimationListener(animationListener);
                    animation.setDuration(DURATION);
                    innerContentView.startAnimation(animation);
                }
            }
        });

        keepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    public void setListener(OnCardDialogListener listener) {
        this.listener = listener;
    }

    public Card getCard() {
        return card;
    }
}
