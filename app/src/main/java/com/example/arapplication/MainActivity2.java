package com.example.arapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

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
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    private ExternalTexture texture;
    private MediaPlayer mediaPlayer;

    private MediaPlayer video1 ;
    private MediaPlayer video2  ;
    private ArFragment arFragment;
    private Scene scene;
    private ModelRenderable renderable;

    String currentImageName = "";

    private AnchorNode anchorNode;
    private boolean isVideoPlaying = false;
    private boolean isVideoPlaying1 = false;
    private boolean isVideoPlaying2 = false;
    private boolean isImageDetected = false;
    private boolean imageInFocus = false;

    private HashSet<String> processedImages = new HashSet<>();
    private HashMap<String, MediaPlayer> videoPlayers = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texture = new ExternalTexture();
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        scene = arFragment.getArSceneView().getScene();
        video1  = MediaPlayer.create(this,  R.raw.video);
        video2 = MediaPlayer.create(this,  R.raw.video2);
        scene.addOnUpdateListener(this::onUpdate);

        Session session;
        try {
            session = new Session(this);
        } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException |
                 UnavailableSdkTooOldException | UnavailableDeviceNotCompatibleException e) {
            throw new RuntimeException(e);
        }

        Config config = new Config(session);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        AugmentedImageDatabase aid = new AugmentedImageDatabase(session);
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        aid.addImage("image", image);
        Bitmap image2 = BitmapFactory.decodeResource(getResources(), R.drawable.image2);
        aid.addImage("image2", image2);
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
    }
    private String currentImageAnchor = "";
    private MediaPlayer currentVideoPlayer = null;

    private boolean foundImage = false;


    private void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if (frame == null) return;
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
        List<String> imagesNoLongerTracked = new ArrayList<>(processedImages);
        for (AugmentedImage image : updatedAugmentedImages) {
            if (image.getTrackingState() == TrackingState.TRACKING && isValidImage(image.getName())) {
                if (!processedImages.contains(image.getName())){

                    currentImageName = image.getName();
                    arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
                    isImageDetected = true;
                    mediaPlayer = MediaPlayer.create(this, (image.getName().equals("image")) ? R.raw.video : R.raw.video2);
                    mediaPlayer.setSurface(texture.getSurface());

                    playVideo(image.createAnchor(image.getCenterPose()), image.getExtentX(), image.getExtentZ());
                    processedImages.add(image.getName());

                    isVideoPlaying = true;
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            isVideoPlaying = false;
                            stopAndReleaseVideo();
                        }
                    });
                } else if (image.getName().equals(currentImageName)) {

                    processedImages.remove(image.getName());
                }
                imagesNoLongerTracked.remove(image.getName());
            }
        }
        for (String noLongerTrackedImage : imagesNoLongerTracked) {
            processedImages.remove(noLongerTrackedImage);
        }
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

    private boolean isValidImage(String imageName) {
        return imageName != null && (imageName.equals("image") || imageName.equals("image2"));
    }

    private void playVideo(Anchor anchor, float extentX, float extentZ) {
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


}