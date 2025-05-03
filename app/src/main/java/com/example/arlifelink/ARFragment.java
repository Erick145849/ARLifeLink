package com.example.arlifelink;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.ar.core.codelabs.hellogeospatial.HelloGeoActivity;
import com.google.ar.core.exceptions.CameraNotAvailableException;


public class ARFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ar_geospatial, container, false);

        // … your existing setup …

        // find your button in the layout
        Button launchGeo = root.findViewById(R.id.btn_launch_geo);
        launchGeo.setOnClickListener(v -> {
            // start the original HelloGeoActivity
            Intent i = new Intent(getActivity(), HelloGeoActivity.class);
            startActivity(i);
        });

        return root;
    }

}