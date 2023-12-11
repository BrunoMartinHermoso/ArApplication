package com.example.arapplication;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ArVideoFragment extends ArFragment {

    private static final String TAG = "";
    private MediaPlayer mediaPlayer;
    private ExternalTexture externalTexture;
    private ModelRenderable videoRenderable;
    private AnchorNode videoAnchorNode;
    private AugmentedImage activeAugmentedImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayer = new MediaPlayer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);



        initializeSession();
        createArScene();

        return view;
    }

    private void createArScene() {
        // Your existing code for creating ExternalTexture, ModelRenderable, and AnchorNode

        videoAnchorNode = new AnchorNode();
        videoAnchorNode.setParent(getArSceneView().getScene());
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        Collection<AugmentedImage> updatedAugmentedImages = getArSceneView().getArFrame().getUpdatedTrackables(AugmentedImage.class);

        List<AugmentedImage> nonFullTrackingImages = updatedAugmentedImages.stream()
                .filter(image -> image.getTrackingState() != TrackingState.TRACKING)
                .collect(Collectors.toList());

        if (activeAugmentedImage != null && mediaPlayer.isPlaying() && nonFullTrackingImages.contains(activeAugmentedImage)) {
            pauseArVideo();
        }

        List<AugmentedImage> fullTrackingImages = updatedAugmentedImages.stream()
                .filter(image -> image.getTrackingState() == TrackingState.TRACKING)
                .collect(Collectors.toList());

        if (fullTrackingImages.isEmpty()) return;

        if (activeAugmentedImage != null && fullTrackingImages.contains(activeAugmentedImage)) {
            if (!mediaPlayer.isPlaying()) {
                resumeArVideo();
            }
            return;
        }

        AugmentedImage augmentedImage = fullTrackingImages.get(0);
        try {
            playbackArVideo(augmentedImage);
        } catch (IOException e) {
            Log.e(TAG, "Could not play video [" + augmentedImage.getName() + "]", e);
        }
    }

    private void pauseArVideo() {
        videoAnchorNode.setRenderable(null);
        mediaPlayer.pause();
    }

    private void resumeArVideo() {
        mediaPlayer.start();
        videoAnchorNode.setRenderable(videoRenderable);
    }

    private void dismissArVideo() {
        if (videoAnchorNode.getAnchor() != null) {
            videoAnchorNode.getAnchor().detach();
        }
        videoAnchorNode.setRenderable(null);
        activeAugmentedImage = null;
        mediaPlayer.reset();
    }

    private void playbackArVideo(AugmentedImage augmentedImage) throws IOException {
        // Your code for setting up MediaPlayer and ExternalTexture

        mediaPlayer.setDataSource(requireContext().getAssets().openFd(augmentedImage.getName()));
        mediaPlayer.setLooping(true);
        mediaPlayer.prepare();
        mediaPlayer.start();

        // Your code for creating AnchorNode and rendering the video
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissArVideo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }

    // Your constants and TAG declaration

}