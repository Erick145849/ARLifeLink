/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.arlifelink;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import java.util.Queue;
import java.util.ArrayDeque;

/**
 * Helper to detect tap gestures. Attach this as an OnTouchListener or forward events via onTouchEvent().
 */
public class TapHelper implements View.OnTouchListener {
    private final Queue<MotionEvent> singleTapQueue = new ArrayDeque<>();

    public TapHelper(Context context, View view) {
        view.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Queue a copy of the tap event
            singleTapQueue.offer(MotionEvent.obtain(event));
        }
        return true;
    }

    /**
     * Call this from your fragment to forward raw touch events when not using a view listener.
     */
    public void onTouchEvent(MotionEvent event) {
        onTouch(null, event);
    }

    /**
     * Polls and returns the oldest tap, or null if none.
     */
    public MotionEvent poll() {
        return singleTapQueue.poll();
    }
}
