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

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AbsListView;

/**
 * RecyclerView
 * http://stackoverflow.com/questions/26543131/how-to-implement-endless-list-with-recyclerview
 *
 * ListVIew
 * https://github.com/makovkastar/FloatingActionButton/blob/master/library/src/main/java/com/melnykov/fab/AbsListViewScrollDetector.java
 *
 * Created by wak on 12/17/14.
 */
public class OnScrollUpDownListener extends RecyclerView.OnScrollListener implements AbsListView.OnScrollListener {

    private static final String TAG = OnScrollUpDownListener.class.getSimpleName();

    private final int scrollThreshold;

    private int lastScrollY = 0;

    private int lastDirection = 0;

    private int lastVisibleItem = 0;

    public OnScrollUpDownListener(int scrollThreshold) {
        this.scrollThreshold = scrollThreshold;
    }

    public void onScrollUp() {
    }

    public void onScrollDown() {
    }

    public void onScrollHitEnd() {
    }

    public void resetDirection() {
        lastDirection = 0;
    }

    private void onScroll(int firstVisibleItem, int scrollY) {
        if (lastVisibleItem == firstVisibleItem) {
            boolean isSignificantDelta = Math.abs(lastScrollY - scrollY) > scrollThreshold;
            if (isSignificantDelta) {
                if (scrollY < lastScrollY) {
                    if (lastDirection != 1) {
                        onScrollDown();
                    }
                    lastDirection = 1;
                } else {
                    if (lastDirection != -1) {
                        onScrollUp();
                    }
                    lastDirection = -1;
                }
            }
        } else {
            if (lastVisibleItem < firstVisibleItem) {
                // scroll down
                if (lastDirection != 1) {
                    onScrollDown();
                }
                lastDirection = 1;
            } else {
                if (lastDirection != -1) {
                    onScrollUp();
                }
                lastDirection = -1;
            }
            lastVisibleItem = firstVisibleItem;
        }
        lastScrollY = scrollY;
    }

    private LinearLayoutManager getLayoutManager(RecyclerView recyclerView) {
        RecyclerView.LayoutManager absLayoutManager = recyclerView.getLayoutManager();
        if (!(absLayoutManager instanceof LinearLayoutManager)) {
            return null;
        }
        return (LinearLayoutManager) absLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        LinearLayoutManager layoutManager = getLayoutManager(recyclerView);
        if (layoutManager == null) {
            return;
        }
        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
        int scrollY = recyclerView.getScrollY();
        onScroll(firstVisibleItem, scrollY);
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        LinearLayoutManager layoutManager = getLayoutManager(recyclerView);
        if (layoutManager == null) {
            return;
        }
        View lastView = layoutManager.findViewByPosition(layoutManager.findLastVisibleItemPosition());
        int diff = layoutManager.getDecoratedBottom(lastView) - recyclerView.getBottom();
        if (diff == 0) {
            onScrollHitEnd();
        }
    }

    private int getScrollY(AbsListView view) {
        int scrollY = 0;
        View topChild = view.getChildAt(0);
        if (topChild != null) {
            scrollY = topChild.getTop();
        }
        return scrollY;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO: not implemented onScrollStateChnged for onScrollHitEnd
        if (getScrollY(view) == lastScrollY) {
            onScrollHitEnd();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int scrollY = 0;
        View topChild = view.getChildAt(0);
        if (topChild != null) {
            scrollY = topChild.getTop();
        }
        onScroll(firstVisibleItem, scrollY);
    }
}
