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
import android.content.DialogInterface;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.db.Model;
import com.google.common.eventbus.Subscribe;

/**
 * Created by wak on 6/12/14.
 */
public class CardDialog extends Dialog {

    private static final String TAG = CardDialog.class.getSimpleName();

    private static final int DURATION = 150;

    private static final int[] ignoredContextItemIds =
            new int[]{R.string.action_speech, R.string.action_edit, R.string.action_delete};

    private final View innerContentView;

    private final TextView displayText;

    private final ImageButton actionButton;

    private final ImageButton speechButton;

    private final ImageButton editButton;

    private final ImageButton deleteButton;

    private final TextView translateText;

    private final TextView noteText;

    private final TextView dictionaryText;

    private final ImageButton backButton;

    private final ImageButton forwardButton;

    private final Button keepButton;

    private final Card card;

    private OnCardDialogListener listener;

    private CardContextActionListener contextActionListener;

    private Context contextActionContext;

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
    }

    public CardDialog(Context context, final Card card) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.card = card;

        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                MonodictApp.getEventBus().register(dialog);
            }
        });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                MonodictApp.getEventBus().unregister(dialog);
            }
        });

        setContentView(R.layout.dialog_card);

        innerContentView = findViewById(R.id.dialog_content);
        displayText = (TextView) findViewById(R.id.display_text);
        translateText = (TextView) findViewById(R.id.translate_text);
        noteText = (TextView) findViewById(R.id.note_text);
        dictionaryText = (TextView) findViewById(R.id.dictionary_text);
        actionButton = (ImageButton) findViewById(R.id.action_button);
        speechButton = (ImageButton) findViewById(R.id.speech_button);
        editButton = (ImageButton) findViewById(R.id.edit_button);
        deleteButton = (ImageButton) findViewById(R.id.delete_button);

        backButton = (ImageButton) findViewById(R.id.back_button);
        forwardButton = (ImageButton) findViewById(R.id.forward_button);
        keepButton = (Button) findViewById(R.id.keep_button);

        initViews();
    }

    private void initViews() {
        displayText.setText(card.getDisplay());

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CardContextMenu contextMenu =
                        new CardContextMenu(getContext(), view, card, ignoredContextItemIds);
                contextMenu.setContextActionListener(contextActionListener);
                contextMenu.show();
            }
        });

        speechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contextActionListener.onCardContextActionSpeech(card);
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contextActionListener.onCardContextActionEdit(card);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contextActionListener.onCardContextActionDelete(card);
            }
        });

        translateText.setText(card.getTranslate());
        translateText.setMovementMethod(ScrollingMovementMethod.getInstance());

        noteText.setText(card.getNote());
        if (noteText.getText() == null || noteText.getText().toString().isEmpty()) {
            noteText.setVisibility(View.GONE);
        }

        String dictionary = card.getDictionary();
        if (dictionary == null || dictionary.isEmpty()) {
            dictionaryText.setVisibility(View.GONE);
        } else {
            dictionaryText.setVisibility(View.VISIBLE);
            dictionaryText.setText(card.getDictionary());
        }

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

    @Subscribe
    public void onEvent(Model.ModelChangeRequestEvent event) {
        Log.d(TAG, "onEvent: " + event);
        dismiss();
    }


    public void setListener(OnCardDialogListener listener) {
        this.listener = listener;
    }

    public void setContextActionListener(CardContextActionListener contextActionListener) {
        this.contextActionListener = contextActionListener;
    }

    public void setContextActionContext(Context contextActionContext) {
        this.contextActionContext = contextActionContext;
    }

    public Card getCard() {
        return card;
    }
}
