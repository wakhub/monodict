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
package com.github.wakhub.monodict.activity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.wakhub.monodict.MonodictApp;
import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.db.Card;
import com.github.wakhub.monodict.ui.CardDialog;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @see com.github.wakhub.monodict.activity.FlashcardActivity
 */
public class FlashcardActivityPagerFragment extends Fragment {

    private static final String TAG = FlashcardActivityPagerFragment.class.getSimpleName();

    static final String ARG_BOX = "box";

    private static abstract class FlashcardItemEvent {

        private final Card card;

        public FlashcardItemEvent(Card card) {
            this.card = card;
        }

        public Card getCard() {
            return card;
        }
    }

    public static final class FlashcardItemSpeechEvent extends FlashcardItemEvent {
        public FlashcardItemSpeechEvent(Card card) {
            super(card);
        }
    }

    public static final class FlashcardItemMoreEvent extends FlashcardItemEvent {
        private final View view;

        public FlashcardItemMoreEvent(Card card, View view) {
            super(card);
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }

    public static final class FlashcardItemClickEvent extends FlashcardItemEvent {
        public FlashcardItemClickEvent(Card card) {
            super(card);
        }
    }

    private RecyclerView recyclerView;

    private RecyclerAdapter recyclerAdapter;

    private List<Card> dataSet = new ArrayList<>();

    private int gridSpanCount = 1;

    public interface Listener {
        void onInitPage(FlashcardActivityPagerFragment fragment, int box);
    }

    public static FlashcardActivityPagerFragment create(int box) {
        FlashcardActivityPagerFragment fragment = new FlashcardActivityPagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BOX, box);
        fragment.setArguments(args);
        return fragment;
    }

    public Optional<CardDialog> getDialog() {
        if (recyclerAdapter == null) {
            return Optional.absent();
        }
        return recyclerAdapter.optDialog;
    }

    public void setDataSet(List<Card> dataSet) {
        this.dataSet.clear();
        this.dataSet.addAll(dataSet);
    }

    private int getBox() {
        return getArguments().getInt(ARG_BOX);
    }

    private FlashcardActivity getFlashcardActivity() {
        return (FlashcardActivity) getActivity();
    }

    public void notifyDataSetChanged() {
        int orientation = getFlashcardActivity().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridSpanCount = 2;
            recyclerView.setLayoutManager(new GridLayoutManager(getFlashcardActivity(), gridSpanCount));
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridSpanCount = 1;
            recyclerView.setLayoutManager(new LinearLayoutManager(getFlashcardActivity()));
        }
        if (recyclerAdapter != null) recyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_flashcard_pager, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        FlashcardActivity activity = getFlashcardActivity();
        recyclerAdapter = new RecyclerAdapter(this);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, gridSpanCount));

        Resources res = getResources();
        final int dividerHorizontal = res.getDimensionPixelOffset(R.dimen.card_view_divider_horizontal);
        final int dividerVertical = res.getDimensionPixelOffset(R.dimen.card_view_divider_vertical);
        final int marginVertical = res.getDimensionPixelOffset(R.dimen.card_view_margin_vertical);
        final int marginHorizontal = res.getDimensionPixelOffset(R.dimen.card_view_margin_horizontal);

        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                Resources res = getResources();
                int itemCount = parent.getAdapter().getItemCount();
                int position = parent.getChildPosition(view);
                if (gridSpanCount == 1) {
                    outRect.bottom = dividerVertical;
                    outRect.right = marginHorizontal;
                    outRect.left = marginHorizontal;
                } else {
                    outRect.bottom = dividerVertical;
                    outRect.right = dividerHorizontal;
                    if (position == 0 || position % gridSpanCount == 0) {
                        outRect.left = marginHorizontal;
                    } else {
                        outRect.right = marginHorizontal;
                    }
                }
                if (position == 0 || position <= gridSpanCount - 1) {
                    outRect.top = marginVertical;
                }
                if (itemCount - position <= 1) {
                    outRect.bottom = res.getDimensionPixelSize(R.dimen.space_for_floating_buttons) + dividerVertical;
                }
            }
        });
        activity.onInitPage(this, getBox());
        notifyDataSetChanged();
    }

    private static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

        private final WeakReference<FlashcardActivityPagerFragment> fragmentRef;

        private Optional<CardDialog> optDialog = Optional.absent();

        private RecyclerAdapter(FlashcardActivityPagerFragment fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.list_item_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final FlashcardActivityPagerFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.dataSet.size() <= position) {
                return;
            }
            final Card card = fragment.dataSet.get(position);

            holder.text1.setText(card.getDisplay());
            String dictionary = card.getDictionary();
            if (dictionary == null || dictionary.isEmpty()) {
                holder.text2.setVisibility(View.GONE);
            } else {
                holder.text2.setVisibility(View.VISIBLE);
                holder.text2.setText(dictionary);
            }
            holder.moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MonodictApp.getEventBus().post(new FlashcardItemMoreEvent(card, holder.moreButton));
                }
            });
            holder.speechButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MonodictApp.getEventBus().post(new FlashcardItemSpeechEvent(card));
                }
            });
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MonodictApp.getEventBus().post(new FlashcardItemClickEvent(card));
                }
            });
        }

        @Override
        public int getItemCount() {
            FlashcardActivityPagerFragment fragment = fragmentRef.get();
            return fragment.dataSet.size();
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {

            private CardView cardView;
            private TextView text1;
            private TextView text2;
            private ImageButton moreButton;
            private ImageButton speechButton;

            private ViewHolder(View view) {
                super(view);

                cardView = (CardView) view.findViewById(R.id.card_view);
                text1 = (TextView) view.findViewById(android.R.id.text1);
                text2 = (TextView) view.findViewById(android.R.id.text2);
                moreButton = (ImageButton) view.findViewById(R.id.more_button);
                speechButton = (ImageButton) view.findViewById(R.id.speech_button);
            }
        }
    }
}
