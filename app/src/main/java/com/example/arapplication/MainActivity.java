package com.example.arapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arapplication.R;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private ExternalTexture texture;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private Button button;

    private ArFragment arFragment;
    private Scene scene;
    private ModelRenderable renderable;



    private AnchorNode anchorNode;
    private boolean isVideoPlaying = false;

    private AugmentedImage activeAugmentedImage;


    private HashSet<String> processedImages = new HashSet<>();

    Session session;

    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texture = new ExternalTexture();
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        scene = arFragment.getArSceneView().getScene();
        button = findViewById(R.id.button);
        button.setVisibility(View.GONE);
        scene.addOnUpdateListener(this::onUpdate);


        try {
            session = new Session(this);
        } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException |
                 UnavailableSdkTooOldException | UnavailableDeviceNotCompatibleException e) {
            throw new RuntimeException(e);
        }

        config = new Config(session);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        AugmentedImageDatabase aid = new AugmentedImageDatabase(session);
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        aid.addImage("image", image);
        Bitmap image2 = BitmapFactory.decodeResource(getResources(), R.drawable.image2);
        aid.addImage("image2", image2);
        /*  Bitmap image3 = BitmapFactory.decodeResource(getResources(), R.drawable.image3);
        aid.addImage("image3", image3);
        Bitmap image4 = BitmapFactory.decodeResource(getResources(), R.drawable.image4);
        aid.addImage("image4", image4);
        Bitmap image5 = BitmapFactory.decodeResource(getResources(), R.drawable.image5);
        aid.addImage("image5", image5);
        Bitmap image6 = BitmapFactory.decodeResource(getResources(), R.drawable.image6);
        aid.addImage("image6", image6);
        Bitmap image7 = BitmapFactory.decodeResource(getResources(), R.drawable.image7);
        aid.addImage("image7", image7);
        Bitmap image8 = BitmapFactory.decodeResource(getResources(), R.drawable.image8);
        aid.addImage("image8", image8);
        Bitmap image9 = BitmapFactory.decodeResource(getResources(), R.drawable.image9);
        aid.addImage("image9", image9);
        Bitmap image10 = BitmapFactory.decodeResource(getResources(), R.drawable.image10);
        aid.addImage("image10", image10);
        Bitmap image11 = BitmapFactory.decodeResource(getResources(), R.drawable.image11);
        aid.addImage("image11", image11);
        Bitmap image12 = BitmapFactory.decodeResource(getResources(), R.drawable.image12);

        aid.addImage("image12", image12);
             */

        config.setAugmentedImageDatabase(aid);

        arFragment.getArSceneView().setupSession(session);
        session.configure(config);

        ModelRenderable.builder()
                .setSource(this, Uri.parse("video_screen.sfb"))
                .build()
                .thenAccept(modelRenderable -> {
                    modelRenderable.getMaterial().setExternalTexture("videoTexture", texture);
                    modelRenderable.getMaterial().setFloat4("keyColor", new Color(0.01843f, 1f, 0.098f));
                    renderable = modelRenderable;
                });
        arFragment.getArSceneView().setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();

                    } else {
                        mediaPlayer.start();
                    }
                }
            }
           return true;
        });
    }


    private boolean foundImage = false;
    private boolean isProcessingImage = false;


    private void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if (frame == null) return;

        if (isProcessingImage) {
            return;
        }

        boolean hasUpdatedTrackables = !frame.getUpdatedTrackables(AugmentedImage.class).isEmpty();

        if (hasUpdatedTrackables && !foundImage) {

            foundImage = true;
        } else if (!hasUpdatedTrackables && foundImage) {

            foundImage = false;
        }

        if (!foundImage || isVideoPlaying) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        List<AugmentedImage> fullTrackingImages = updatedAugmentedImages.stream()
                .filter(image -> image.getTrackingState() == TrackingState.TRACKING)
                .collect(Collectors.toList());

        if (fullTrackingImages.isEmpty()) {
            return;
        }
        AugmentedImage newDetectedImage;

        if (!updatedAugmentedImages.isEmpty()) {
            newDetectedImage = fullTrackingImages.iterator().next();

            if (activeAugmentedImage == null || !activeAugmentedImage.equals(newDetectedImage)) {
                try {


                    if (!isVideoPlaying) {
                        arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);

                        mediaPlayer = MediaPlayer.create(this, (newDetectedImage.getName().equals("image")) ? R.raw.video : R.raw.video2);
                        mediaPlayer.setSurface(texture.getSurface());

                        playVideo(newDetectedImage.createAnchor(newDetectedImage.getCenterPose()), newDetectedImage.getExtentX(), newDetectedImage.getExtentZ());
                        processedImages.add(newDetectedImage.getName());
                        button.setVisibility(View.VISIBLE);
                        isVideoPlaying = true;
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                isVideoPlaying = false;
                                stopAndReleaseVideo();
                                configureSessionWithAugmentedImagesInBackground();
                                button.setVisibility(View.GONE);

                            }
                        });
                        activeAugmentedImage = newDetectedImage;


                    }
                } catch (IOException e) {
                    Log.e(TAG, "Could not play video [" + newDetectedImage.getName() + "]", e);
                }
            }
        }else {


            activeAugmentedImage = null;
        }
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                configureSessionWithAugmentedImagesInBackground();
                mediaPlayer.pause();
                mediaPlayer.stop();
                mediaPlayer.reset();
                button.setVisibility(View.GONE);
                isVideoPlaying = false;
                if (anchorNode != null) {
                    scene.removeChild(anchorNode);
                    anchorNode.getAnchor().detach();
                    anchorNode.setParent(null);
                    anchorNode = null;
                }
            }
        });
    }
    private void configureSessionWithAugmentedImagesInBackground() {
        new Thread(() -> {
            AugmentedImageDatabase aid = new AugmentedImageDatabase(session);
            Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.image);
            aid.addImage("image", image);
            Bitmap image2 = BitmapFactory.decodeResource(getResources(), R.drawable.image2);
            aid.addImage("image2", image2);
             /*  Bitmap image3 = BitmapFactory.decodeResource(getResources(), R.drawable.image3);
        aid.addImage("image3", image3);
        Bitmap image4 = BitmapFactory.decodeResource(getResources(), R.drawable.image4);
        aid.addImage("image4", image4);
        Bitmap image5 = BitmapFactory.decodeResource(getResources(), R.drawable.image5);
        aid.addImage("image5", image5);
        Bitmap image6 = BitmapFactory.decodeResource(getResources(), R.drawable.image6);
        aid.addImage("image6", image6);
        Bitmap image7 = BitmapFactory.decodeResource(getResources(), R.drawable.image7);
        aid.addImage("image7", image7);
        Bitmap image8 = BitmapFactory.decodeResource(getResources(), R.drawable.image8);
        aid.addImage("image8", image8);
        Bitmap image9 = BitmapFactory.decodeResource(getResources(), R.drawable.image9);
        aid.addImage("image9", image9);
        Bitmap image10 = BitmapFactory.decodeResource(getResources(), R.drawable.image10);
        aid.addImage("image10", image10);
        Bitmap image11 = BitmapFactory.decodeResource(getResources(), R.drawable.image11);
        aid.addImage("image11", image11);
        Bitmap image12 = BitmapFactory.decodeResource(getResources(), R.drawable.image12);

        aid.addImage("image12", image12);
             */

            config.setAugmentedImageDatabase(aid);
            session.configure(config);
            runOnUiThread(() -> session.configure(config));
        }).start();
    }


    private void stopAndReleaseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();

            isVideoPlaying = false;
            if (anchorNode != null) {
                scene.removeChild(anchorNode);
                anchorNode.getAnchor().detach();
                anchorNode.setParent(null);
                anchorNode = null;
            }
        }
    }
    private void playVideo(Anchor anchor, float extentX, float extentZ)  throws IOException {
        mediaPlayer.start();

        anchorNode = new AnchorNode(anchor);
        anchorNode.setWorldScale(new Vector3(extentX, 1f, extentZ));
        scene.addChild(anchorNode);

        texture.getSurfaceTexture().setOnFrameAvailableListener(surfaceTexture -> {
            renderable.getMaterial().setExternalTexture("videoTexture", texture);
            anchorNode.setRenderable(renderable);
            texture.getSurfaceTexture().setOnFrameAvailableListener(null);
        });
    }
    @Override
    public void onPause() {
        super.onPause();
        configureSessionWithAugmentedImagesInBackground();
        mediaPlayer.pause();
        mediaPlayer.stop();
        mediaPlayer.reset();
        button.setVisibility(View.GONE);
        isVideoPlaying = false;
        if (anchorNode != null) {
            scene.removeChild(anchorNode);
            anchorNode.getAnchor().detach();
            anchorNode.setParent(null);
            anchorNode = null;
        }
    }

}

