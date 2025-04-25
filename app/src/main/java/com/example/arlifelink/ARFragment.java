// ARFragment.java
package com.example.arlifelink;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dewakoding.arlocationbased.model.Place;

import java.util.ArrayList;

public class ARFragment extends Fragment {
    private Button button;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ar, container, false);

            ArrayList<Place> list = new ArrayList<>();
            list.add(new Place(
                    "1",
                    "Coffee Shop",
                    -6.174870735058176,
                    106.82620041234728,
                    0f,
                    0f,
                    0.0,
                    "Promotion available here",           // description
                    50f,                                  // distance (radius)
                    ""
            ));
            list.add(new Place(
                    "2",
                    "Restaurant",
                    -6.122310891453182,
                    106.83357892611079,
                    0f,
                    0f,
                    0.0,
                    "Good Resto",
                    50f,                                  // distance (radius)
                    ""
            ));
        double radius = getIntent().getDoubleExtra("radius", 50.0);
        return view;
    }
}
