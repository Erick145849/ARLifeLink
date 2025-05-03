package com.example.arlifelink;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.gms.maps.MapView;

public class CustomMapView extends MapView {
    public CustomMapView(Context context) {
        super(context);
    }

    public CustomMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}