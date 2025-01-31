package com.example.arlifelink;


import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MotionEvent;

import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.arFragment);
        if (fragment instanceof ArFragment) {
            arFragment = (ArFragment) fragment;
        } else {
            Log.e("MainActivity", "Fragment is not an instance of ArFragment. Check XML.");
            return;
        }

        if (arFragment != null) {
            arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                com.google.ar.core.Anchor anchor = hitResult.createAnchor();
                ModelRenderable.builder()
                        .setSource(this, Uri.parse("file:///android_asset/model.glb")) // Correct path
                        .build()
                        .thenAccept(modelRenderable -> placeModel(anchor, modelRenderable))
                        .exceptionally(throwable -> {
                            Toast.makeText(this, "Error loading model: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            return null;
                        });
            });
        } else {
            Log.e("MainActivity", "arFragment could not be initialized.");
        }
    }




    private void placeModel(com.google.ar.core.Anchor anchor, ModelRenderable modelRenderable) {
        // Create an AnchorNode for the anchor
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create a TransformableNode for the 3D model
        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
        modelNode.setParent(anchorNode);
        modelNode.setRenderable(modelRenderable);
        modelNode.select();
    }
}
