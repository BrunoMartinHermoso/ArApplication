package com.example.arapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MainActivity3 extends AppCompatActivity {

    private ExternalTexture texture;
    private MediaPlayer mediaPlayer;

    private ArFragment arFragment;
    private Scene scene;
    private ModelRenderable renderable;

    String currentImageName = "";

    private AnchorNode anchorNode;
    private boolean isVideoPlaying = false;



    private ViewRenderable textRenderable; // Variable para el renderizado del texto

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
        List<String> imagesNoLongerTracked = new ArrayList<>(processedImages);

        for (AugmentedImage image : updatedAugmentedImages) {
            if (image.getTrackingState() == TrackingState.TRACKING && isValidImage(image.getName())) {
                if (!processedImages.contains(image.getName())){
                    currentImageName = image.getName();
                    arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);


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
                            ViewRenderable.builder()
                                    .setView(MainActivity3.this, R.layout.text_view_layout) // Layout que contiene el texto "Reproducir de nuevo"
                                    .build()
                                    .thenAccept(renderable -> {
                                        textRenderable = renderable;
                                        addTextToScene();
                                    }); // Mostrar texto "Reproducir de nuevo" al finalizar el video

                        }
                    });
                }
            }
        }
    }

    // Método para mostrar el texto "Reproducir de nuevo"
    private void showReplayText() {

    }

    // Método para agregar el texto a la escena
    private void addTextToScene() {
        Anchor anchor = anchorNode.getAnchor(); // Obtener el ancla actual

        if (anchor != null && textRenderable != null) {
            AnchorNode textAnchorNode = new AnchorNode(anchor);
            textAnchorNode.setParent(arFragment.getArSceneView().getScene());

            Node textNode = new Node();
            textNode.setRenderable(textRenderable);
            textNode.setParent(textAnchorNode);

            // Posición del texto (ajusta según sea necesario)
            textNode.setLocalPosition(new Vector3(0.0f, 0.1f, -0.5f));

            scene.addChild(textNode);
            ;
            // Manejo del evento de toque para reproducir el video nuevamente
            textNode.setOnTapListener((hitTestResult, motionEvent) -> {
                replayVideo();
            });
        }
    }

    // Método para reproducir el video nuevamente al tocar el texto
    private void replayVideo() {

        mediaPlayer.start();
        // Quitar el texto al reproducir el video nuevamente (puedes ajustar según tu estructura de anclas)

    }

    private void stopAndReleaseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();


            isVideoPlaying = false;


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