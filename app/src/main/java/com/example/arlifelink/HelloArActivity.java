/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.arlifelink;

import com.google.ar.core.exceptions.NotTrackingException;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.example.arlifelink.common.helpers.CameraPermissionHelper;
import com.example.arlifelink.common.helpers.DepthSettings;
import com.example.arlifelink.common.helpers.DisplayRotationHelper;
import com.example.arlifelink.common.helpers.FullScreenHelper;
import com.example.arlifelink.common.helpers.InstantPlacementSettings;
import com.example.arlifelink.common.helpers.SnackbarHelper;
import com.example.arlifelink.common.helpers.TapHelper;
import com.example.arlifelink.common.helpers.TrackingStateHelper;
import com.example.arlifelink.common.samplerender.Framebuffer;
import com.example.arlifelink.common.samplerender.GLError;
import com.example.arlifelink.common.samplerender.Mesh;
import com.example.arlifelink.common.samplerender.SampleRender;
import com.example.arlifelink.common.samplerender.Shader;
import com.example.arlifelink.common.samplerender.Texture;
import com.example.arlifelink.common.samplerender.VertexBuffer;
import com.example.arlifelink.common.samplerender.arcore.BackgroundRenderer;
import com.example.arlifelink.common.samplerender.arcore.PlaneRenderer;
import com.example.arlifelink.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {
  private static final String KEY_PREFIX = "pose_";
  private boolean anchorsLoaded = false;
  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
  private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";
  // Tracks whether we’ve captured the world origin yet
  private boolean originSet = false;
  private Pose worldOriginPose;

  // In prefs: a Set<String> of CSV‐encoded relative‐poses
  private static final String PREFS_NAME     = "arl_notes";
  private static final String KEY_REL_POSES  = "relative_poses";

  // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
  // constants.
  private static final float[] sphericalHarmonicFactors = {
    0.282095f,
    -0.325735f,
    0.325735f,
    -0.325735f,
    0.273137f,
    -0.273137f,
    0.078848f,
    -0.273137f,
    0.136569f,
  };
  private Pose   pendingLoadPose;
  private boolean hasPendingLoad = false;

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;
  private boolean hasPendingPlacement = false;
  private Anchor pendingAnchor;
  private final DepthSettings depthSettings = new DepthSettings();
  private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];
  private Button submitButton;
  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
  // Assumed distance from the device camera to the surface on which user will try to place objects.
  // This value affects the apparent scale of objects while the tracking method of the
  // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
  // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
  // values for AR experiences where users are expected to place objects on surfaces close to the
  // camera. Use larger values for experiences where the user will likely be standing and trying to
  // place an object on the ground or floor in front of them.
  private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
  // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
  private long lastPointCloudTimestamp = 0;

  // Virtual object (ARCore pawn)
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;
  private Texture virtualObjectAlbedoTexture;
  private Texture virtualObjectAlbedoInstantPlacementTexture;
  private List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

  // Environmental HDR
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16]; // view x model
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
  private final float[] viewInverseMatrix = new float[16];
  private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
  private final float[] viewLightDirection = new float[4]; // view x world light direction
  private String noteText;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/* context= */ this);
    noteText = getIntent().getStringExtra("NOTE_TEXT");
    // Set up touch listener.
    tapHelper = new TapHelper(/* context= */ this);
    surfaceView.setOnTouchListener(tapHelper);
    submitButton = findViewById(R.id.btn_submit);
    submitButton.setOnClickListener(v -> onSubmit());
    // Set up renderer.
    render = new SampleRender(surfaceView, this, getAssets());

    installRequested = false;

    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);
    ImageButton settingsButton = findViewById(R.id.settings_button);

      settingsButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PopupMenu popup = new PopupMenu(HelloArActivity.this, v);
            popup.setOnMenuItemClickListener(HelloArActivity.this::settingsMenuClick);
            popup.inflate(R.menu.settings_menu);
            popup.show();
          }
        });
  }


  private String poseToCsv(Pose p) {
    float[] t = p.getTranslation();
    float[] q = p.getRotationQuaternion();
    return String.format(Locale.US,
            "%f,%f,%f,%f,%f,%f,%f",
            t[0],t[1],t[2], q[0],q[1],q[2],q[3]);
  }

  @Nullable
  private Pose csvToPose(String csv) {
    String[] parts = csv.split(",");
    if (parts.length != 7) return null;
    try {
      float tx = Float.parseFloat(parts[0]);
      float ty = Float.parseFloat(parts[1]);
      float tz = Float.parseFloat(parts[2]);
      float qx = Float.parseFloat(parts[3]);
      float qy = Float.parseFloat(parts[4]);
      float qz = Float.parseFloat(parts[5]);
      float qw = Float.parseFloat(parts[6]);
      return new Pose(new float[]{tx,ty,tz}, new float[]{qx,qy,qz,qw});
    } catch (NumberFormatException e) {
      return null;
    }
  }
  private void loadSavedAnchors() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    Set<String> csvSet = prefs.getStringSet(KEY_REL_POSES, Collections.emptySet());
    for (String csv : csvSet) {
      Pose rel = csvToPose(csv);
      if (rel == null) continue;
      // worldPose = origin ∘ relative
      Pose world = worldOriginPose.compose(rel);
      Anchor a = session.createAnchor(world);
      wrappedAnchors.add(new WrappedAnchor(a));
    }
  }

  private void onSubmit() {
    if (!hasPendingPlacement || pendingAnchor == null) {
      setResult(RESULT_CANCELED);
    } else {
      // persist new pose
      savePoseLocally(noteText, pendingAnchor.getPose());
      setResult(RESULT_OK);
    }
    finish();
  }

  private void savePoseLocally(String noteTitle, Pose p) {
    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + noteTitle, poseToCsv(p))
            .apply();
  }

  @Nullable
  private Pose loadPoseLocally(String noteTitle) {
    String csv = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_PREFIX + noteTitle, null);
    return (csv != null) ? csvToPose(csv) : null;
  }
  /** Menu button to launch feature specific settings. */
  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.depth_settings) {
      launchDepthSettingsMenuDialog();
      return true;
    } else if (item.getItemId() == R.id.instant_placement_settings) {
      launchInstantPlacementSettingsMenuDialog();
      return true;
    }
    return false;
  }
  private static Bitmap textToBitmap(
          String text,
          int width,
          int height,
          float textSizePx,
          @ColorInt int textColor,
          @ColorInt int bgColor
  ) {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextSize(textSizePx);
    paint.setColor(textColor);
    paint.setTextAlign(Paint.Align.CENTER);

    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas c  = new Canvas(bmp);
    c.drawColor(bgColor);

    // Rotate the canvas 90° counter-clockwise around its center
    c.save();
    c.rotate(-90f, width * 0.5f, height * 0.5f);

    // draw text centered in the rotated canvas
    float x = width  * 0.5f;
    float y = height * 0.5f - (paint.descent() + paint.ascent()) * 0.5f;
    c.drawText(text, x, y, paint);

    c.restore();
    return bmp;
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        // Always check the latest availability.
        Availability availability = ArCoreApk.getInstance().checkAvailability(this);

        // In all other cases, try to install ARCore and handle installation failures.
        if (availability != Availability.SUPPORTED_INSTALLED) {
          switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            case INSTALL_REQUESTED:
              installRequested = true;
              return;
            case INSTALLED:
              break;
          }
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }
    Pose saved = loadPoseLocally(noteText);
    if (saved != null) {
      pendingLoadPose = saved;
      hasPendingLoad  = true;
    }

    try {
      configureSession();
      // To record a live camera session for later playback, call
      // `session.startRecording(recordingConfig)` at anytime. To playback a previously recorded AR
      // session instead of using the live camera feed, call
      // `session.setPlaybackDatasetUri(Uri)` before calling `session.resume()`. To
      // learn more about recording and playback, see:
      // https://developers.google.com/ar/develop/java/recording-and-playback
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }
    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

      cubemapFilter =
          new SpecularCubemapFilter(
              render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // Load DFG lookup table for environmental lighting
      dfgTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /* useMipmaps= */ false);
      // The dfg.raw file is a raw half-float texture with two channels.
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
          ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      // SampleRender abstraction leaks here.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /* level= */ 0,
          GLES30.GL_RG16F,
          /* width= */ dfgResolution,
          /* height= */ dfgResolution,
          /* border= */ 0,
          GLES30.GL_RG,
          GLES30.GL_HALF_FLOAT,
          buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

      // Point cloud
      pointCloudShader =
          Shader.createFromAssets(
                  render,
                  "shaders/point_cloud.vert",
                  "shaders/point_cloud.frag",
                  /* defines= */ null)
              .setVec4(
                  "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
              .setFloat("u_PointSize", 5.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
          new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
          new Mesh(
              render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);
      Bitmap textBmp = textToBitmap(
              noteText,   // your placeholder text
              512,               // texture resolution
              512,
              64f,               // font size in px
              Color.BLACK,       // text color
              Color.YELLOW       // background color
      );
      Texture textTexture = Texture.createFromBitmap(
              render,
              textBmp,
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      virtualObjectAlbedoTexture =
              Texture.createFromBitmap(
                      render,
                      textBmp,
                      Texture.WrapMode.CLAMP_TO_EDGE,
                      Texture.ColorFormat.SRGB);
      virtualObjectAlbedoInstantPlacementTexture =
              Texture.createFromBitmap(
                      render,
                      textBmp,
                      Texture.WrapMode.CLAMP_TO_EDGE,
                      Texture.ColorFormat.SRGB);
      Texture virtualObjectPbrTexture =
              Texture.createFromBitmap(
                      render,
                      textBmp,
                      Texture.WrapMode.CLAMP_TO_EDGE,
                      Texture.ColorFormat.SRGB);
      Texture white = Texture.createFromAsset(render,
              "models/white_1x1.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.LINEAR);

// **Swap ONLY this line** — point Mesh.createFromAsset at your OBJ:
      virtualObjectMesh = Mesh.createFromAsset(render, "models/sticky_note_double.obj");

      virtualObjectShader =
              Shader.createFromAssets(
                              render,
                              "shaders/environmental_hdr.vert",
                              "shaders/environmental_hdr.frag",
                              Collections.singletonMap(
                                      "NUMBER_OF_MIPMAP_LEVELS",
                                      Integer.toString(cubemapFilter.getNumberOfMipmapLevels())))
                      .setTexture("u_AlbedoTexture", textTexture)
                      // this is the line you asked about — keep it as-is if you still have a PBR map:
                      .setTexture(
                              "u_RoughnessMetallicAmbientOcclusionTexture", white)
//                      .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
                      .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
                      .setTexture("u_DfgTexture", dfgTexture);


      if (loadPoseLocally(noteText) != null) {
        Pose saved = loadPoseLocally(noteText);
        Anchor a = session.createAnchor(saved);
        wrappedAnchors.clear();
        wrappedAnchors.add(new WrappedAnchor(a));
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  @Override
  public void onDrawFrame(SampleRender render) {
    if (session == null) {
      return;
    }

    // Texture names should only be set once on a GL thread unless they change. This is done during
    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
    // initialized during the execution of onSurfaceCreated.
    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
          new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    // -- Update per-frame state

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    // Obtain the current frame from the AR Session. When the configuration is set to
    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
    // camera framerate.
    Frame frame;
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }

    Camera camera = frame.getCamera();
    if (!originSet && camera.getTrackingState() == TrackingState.TRACKING) {
      worldOriginPose = camera.getPose();
      originSet = true;
      loadSavedAnchors();
    }
    // Update BackgroundRenderer state to match the depth settings.
    try {
      backgroundRenderer.setUseDepthVisualization(
          render, depthSettings.depthColorVisualizationEnabled());
      backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
      return;
    }
    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(frame);

    if (camera.getTrackingState() == TrackingState.TRACKING
        && (depthSettings.useDepthForOcclusion()
            || depthSettings.depthColorVisualizationEnabled())) {
      try (Image depthImage = frame.acquireDepthImage16Bits()) {
        backgroundRenderer.updateCameraDepthTexture(depthImage);
      } catch (NotYetAvailableException e) {
        // This normally means that depth data is not available yet. This is normal so we will not
        // spam the logcat with this.
      }
    }

    // Handle one tap per frame.
    handleTap(frame, camera);

    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    // Show a message based on whether tracking has failed, if planes are detected, and if the user
    // has placed any objects.
    String message = null;
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
        message = SEARCHING_PLANE_MESSAGE;
      } else {
        message = TrackingStateHelper.getTrackingFailureReasonString(camera);
      }
    } else if (hasTrackingPlane()) {
      if (wrappedAnchors.isEmpty()) {
        message = WAITING_FOR_TAP_MESSAGE;
      }
    } else {
      message = SEARCHING_PLANE_MESSAGE;
    }
    if (message == null) {
      messageSnackbarHelper.hide(this);
    } else {
      messageSnackbarHelper.showMessage(this, message);
    }

    // -- Draw background

    if (frame.getTimestamp() != 0) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render);
    }

    // If not tracking, don't draw 3D objects.
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return;
    }

    // -- Draw non-occluded virtual objects (planes, point cloud)

    // Get projection matrix.
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

    // Get camera matrix and draw.
    camera.getViewMatrix(viewMatrix, 0);

    // Visualize tracked points.
    // Use try-with-resources to automatically release the point cloud.
    try (PointCloud pointCloud = frame.acquirePointCloud()) {
      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
        pointCloudVertexBuffer.set(pointCloud.getPoints());
        lastPointCloudTimestamp = pointCloud.getTimestamp();
      }
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
      render.draw(pointCloudMesh, pointCloudShader);
    }

    // Visualize planes.
    planeRenderer.drawPlanes(
        render,
        session.getAllTrackables(Plane.class),
        camera.getDisplayOrientedPose(),
        projectionMatrix);

    // -- Draw occluded virtual objects

    // Update lighting parameters in the shader
    updateLightEstimation(frame.getLightEstimate(), viewMatrix);

    // Visualize anchors created by touch.
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
    for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
      Anchor anchor = wrappedAnchor.getAnchor();
      if (anchor.getTrackingState() != TrackingState.TRACKING) {
        continue;
      }

      // Get the current pose of an Anchor in world space. The Anchor pose is updated
      // during calls to session.update() as ARCore refines its estimate of the world.
      anchor.getPose().toMatrix(modelMatrix, 0);

      // Calculate model/view/projection matrices
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

      // Update shader properties and draw
      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);

        virtualObjectShader.setTexture(
            "u_AlbedoTexture", virtualObjectAlbedoInstantPlacementTexture);

      render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);
    }

    // Compose the virtual scene with the background.
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
  }
  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap == null || camera.getTrackingState() != TrackingState.TRACKING) return;

    List<HitResult> hits = instantPlacementSettings.isInstantPlacementEnabled()
            ? frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS)
            : frame.hitTest(tap);

    for (HitResult hit : hits) {
      Trackable t = hit.getTrackable();
      boolean valid = (t instanceof Plane && ((Plane)t).isPoseInPolygon(hit.getHitPose()))
              || t instanceof InstantPlacementPoint || t instanceof DepthPoint;
      if (!valid) continue;

      // detach old
      for (WrappedAnchor wa : wrappedAnchors) wa.getAnchor().detach();
      wrappedAnchors.clear();

      // make world-space anchor
      Pose hitPose = hit.getHitPose();
      Pose camPose = camera.getPose();
      Pose faceYou = new Pose(
              new float[]{hitPose.tx(), hitPose.ty(), hitPose.tz()},
              new float[]{camPose.qx(), camPose.qy(), camPose.qz(), camPose.qw()}
      );
      Anchor newAnchor = session.createAnchor(faceYou);
      wrappedAnchors.add(new WrappedAnchor(newAnchor));

      // compute relative = origin⁻¹ ∘ faceYou
      Pose rel = worldOriginPose.inverse().compose(faceYou);
      String csv = poseToCsv(rel);

      // save into prefs set
      SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
      Set<String> set = new HashSet<>(prefs.getStringSet(KEY_REL_POSES, Collections.emptySet()));
      set.clear();             // you said only one at a time
      set.add(csv);
      prefs.edit().putStringSet(KEY_REL_POSES, set).apply();

      break;
    }
  }





  // Save a single Anchor’s pose (translation + rotation) into SharedPreferences as a small CSV string
//  private void saveAnchorPose(Anchor anchor) {
//    Pose p = anchor.getPose();
//    float[] t = p.getTranslation();
//    float[] q = p.getRotationQuaternion();
//    // Format: "tx,ty,tz,qx,qy,qz,qw"
//    String csv = String.format(Locale.US, "%f,%f,%f,%f,%f,%f,%f",
//            t[0], t[1], t[2], q[0], q[1], q[2], q[3]);
//    SharedPreferences prefs = getSharedPreferences("arlifelink", MODE_PRIVATE);
//    Set<String> set = new HashSet<>(prefs.getStringSet("anchors", new HashSet<>()));
//    set.add(csv);
//    prefs.edit().putStringSet("anchors", set).apply();
//  }
//
//  // Read back all saved poses and re-create Anchors
//  private void loadSavedAnchors() {
//    SharedPreferences prefs = getSharedPreferences("arlifelink", MODE_PRIVATE);
//    Set<String> set = prefs.getStringSet("anchors", new HashSet<>());
//    for (String csv : set) {
//      String[] parts = csv.split(",");
//      try {
//        float tx = Float.parseFloat(parts[0]);
//        float ty = Float.parseFloat(parts[1]);
//        float tz = Float.parseFloat(parts[2]);
//        float qx = Float.parseFloat(parts[3]);
//        float qy = Float.parseFloat(parts[4]);
//        float qz = Float.parseFloat(parts[5]);
//        float qw = Float.parseFloat(parts[6]);
//        Pose pose = new Pose(
//                new float[]{tx, ty, tz},
//                new float[]{qx, qy, qz, qw}
//        );
//        Anchor restored = session.createAnchor(pose);
//        wrappedAnchors.add(new WrappedAnchor(restored, null));
//      } catch (NotTrackingException | NumberFormatException e) {
//        // session not tracking yet or bad data → skip this anchor for now
//      }
//    }
//  }


  /**
   * Shows a pop-up dialog on the first call, determining whether the user wants to enable
   * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
   */
  private void showOcclusionDialogIfNeeded() {
    boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
      return; // Don't need to show dialog.
    }

    // Asks the user whether they want to use depth-based occlusion.
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_with_depth)
        .setMessage(R.string.depth_use_explanation)
        .setPositiveButton(
            R.string.button_text_enable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(true);
            })
        .setNegativeButton(
            R.string.button_text_disable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(false);
            })
        .show();
  }

  private void launchInstantPlacementSettingsMenuDialog() {
    resetSettingsMenuDialogCheckboxes();
    Resources resources = getResources();
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_instant_placement)
        .setMultiChoiceItems(
            resources.getStringArray(R.array.instant_placement_options_array),
            instantPlacementSettingsMenuDialogCheckboxes,
            (DialogInterface dialog, int which, boolean isChecked) ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
        .setPositiveButton(
            R.string.done,
            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
        .setNegativeButton(
            android.R.string.cancel,
            (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
        .show();
  }

  /** Shows checkboxes to the user to facilitate toggling of depth-based effects. */
  private void launchDepthSettingsMenuDialog() {
    // Retrieves the current settings to show in the checkboxes.
    resetSettingsMenuDialogCheckboxes();

    // Shows the dialog to the user.
    Resources resources = getResources();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      // With depth support, the user can select visualization options.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_with_depth)
          .setMultiChoiceItems(
              resources.getStringArray(R.array.depth_options_array),
              depthSettingsMenuDialogCheckboxes,
              (DialogInterface dialog, int which, boolean isChecked) ->
                  depthSettingsMenuDialogCheckboxes[which] = isChecked)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .setNegativeButton(
              android.R.string.cancel,
              (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
          .show();
    } else {
      // Without depth support, no settings are available.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_without_depth)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .show();
    }
  }

  private void applySettingsMenuDialogCheckboxes() {
    depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
    depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
    instantPlacementSettings.setInstantPlacementEnabled(
        instantPlacementSettingsMenuDialogCheckboxes[0]);
    configureSession();
  }

  private void resetSettingsMenuDialogCheckboxes() {
    depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
    depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
    instantPlacementSettingsMenuDialogCheckboxes[0] =
        instantPlacementSettings.isInstantPlacementEnabled();
  }

  /** Checks if we detected at least one plane. */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  /** Update state based on the current frame's light estimation. */
  private void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
    if (lightEstimate.getState() != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false);
      return;
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true);

    Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix);

    updateMainLight(
        lightEstimate.getEnvironmentalHdrMainLightDirection(),
        lightEstimate.getEnvironmentalHdrMainLightIntensity(),
        viewMatrix);
    updateSphericalHarmonicsCoefficients(
        lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
  }

  private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
    // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
    worldLightDirection[0] = direction[0];
    worldLightDirection[1] = direction[1];
    worldLightDirection[2] = direction[2];
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection);
    virtualObjectShader.setVec3("u_LightIntensity", intensity);
  }

  private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
    // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
    // constants in sphericalHarmonicFactors were derived from three terms:
    //
    // 1. The normalized spherical harmonics basis functions (y_lm)
    //
    // 2. The lambertian diffuse BRDF factor (1/pi)
    //
    // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
    // of all incoming light over a hemisphere for a given surface normal, which is what the shader
    // (environmental_hdr.frag) expects.
    //
    // You can read more details about the math here:
    // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

    if (coefficients.length != 9 * 3) {
      throw new IllegalArgumentException(
          "The given coefficients array must be of length 27 (3 components per 9 coefficients");
    }

    // Apply each factor to every component of each coefficient
    for (int i = 0; i < 9 * 3; ++i) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
    }
    virtualObjectShader.setVec3Array(
        "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
  }

  /** Configures the session with feature settings. */
  private void configureSession() {
    Config config = session.getConfig();
    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }
    if (instantPlacementSettings.isInstantPlacementEnabled()) {
      config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
    } else {
      config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
    }
    session.configure(config);
  }
    private void clearAllNotes() {
        // 1) Detach every anchor so ARCore frees it
        for (WrappedAnchor wa : wrappedAnchors) {
            wa.getAnchor().detach();
        }

        // 2) Remove them from our in-memory list
        wrappedAnchors.clear();

        // 3) Clear saved anchors in prefs so they don’t reload next time
        SharedPreferences prefs = getSharedPreferences("arlifelink", MODE_PRIVATE);
        prefs.edit().remove("anchors").apply();
    }

}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
class WrappedAnchor {
  private final Anchor anchor;
  public WrappedAnchor(Anchor a) { anchor = a; }
  public Anchor getAnchor() { return anchor; }
}
