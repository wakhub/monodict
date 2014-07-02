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

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.db.Card;
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.mobsandgeeks.saripaar.annotation.TextRule;

/**
 * Created by wak on 6/29/14.
 */
public class CardEditDialog extends Dialog implements Validator.ValidationListener {

    private static final String TAG = CardEditDialog.class.getSimpleName();

    private final TextView titleText;

    @Required(order = 1, messageResId = R.string.message_validation_required)
    @TextRule(order = 2, trim = true, messageResId =  R.string.message_validation_max_100)
    private final EditText displayText;

    @Required(order = 2, messageResId = R.string.message_validation_required)
    @TextRule(order = 4, trim = true, maxLength = 5000, messageResId = R.string.message_validation_max_5000)
    private final EditText translateText;

    @TextRule(order = 5, trim = true, maxLength = 5000, messageResId = R.string.message_validation_max_5000)
    private final EditText noteText;

    private final Button cancelButton;

    private final Button saveButton;

    private final com.mobsandgeeks.saripaar.Validator validator;

    private final Card card;

    private Listener listener;

    public interface Listener {
        void onCardEditDialogSave(CardEditDialog dialog, Card card);
    }

    public CardEditDialog(Context context, Card card) {
        super(context, R.style.AppTheme_Flashcard_CardDialog);

        this.validator = new com.mobsandgeeks.saripaar.Validator(this);
        this.validator.setValidationListener(this);

        if (card == null) {
            this.card = new Card();
        } else {
            this.card = card;
        }

        setContentView(R.layout.dialog_card_edit);

        titleText = (TextView) findViewById(R.id.title_text);
        displayText = (EditText) findViewById(R.id.display_text);
        translateText = (EditText) findViewById(R.id.translate_text);
        noteText = (EditText) findViewById(R.id.note_text);
        cancelButton = (Button) findViewById(R.id.cancel_button);
        saveButton = (Button) findViewById(R.id.save_button);

        initViews();
    }

    public void initViews() {
        if (card.getId() == null) {
            titleText.setText(R.string.action_add_new_card);
        } else {
            titleText.setText(R.string.action_edit);
        }

        displayText.setHint(Card.Column.DISPLAY);
        displayText.setText(card.getDisplay());
        displayText.requestFocus();

        translateText.setHint(Card.Column.TRANSLATE);
        translateText.setText(card.getTranslate());

        noteText.setHint(Card.Column.NOTE);
        noteText.setText(card.getNote());

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick cancelButton");
                dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick saveButton");
                displayText.setError(null);
                translateText.setError(null);
                noteText.setError(null);
                validator.validate();
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void save() {
        card.setDisplay(displayText.getText().toString());
        card.setTranslate(translateText.getText().toString());
        card.setNote(noteText.getText().toString());

        listener.onCardEditDialogSave(this, card);
    }

    @Override
    public void onValidationSucceeded() {
        save();
    }

    @Override
    public void onValidationFailed(View view, Rule<?> rule) {
        if (view instanceof TextView) {
            ((TextView) view).setError(rule.getFailureMessage());
            view.requestFocus();
        }
    }
}
