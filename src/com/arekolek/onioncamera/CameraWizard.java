
package com.arekolek.onioncamera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.util.List;

public class CameraWizard {

    private int cameraId;
    private Camera camera;
    private int orientation;

    public CameraWizard(int cameraId) {
        this.cameraId = cameraId;
        this.camera = Camera.open(cameraId);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setOrientation(Display display) {
        CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            orientation = (info.orientation + degrees) % 360;
            orientation = (360 - orientation) % 360; // compensate the mirror
        } else { // back-facing
            orientation = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(orientation);
    }

    public static CameraWizard getBackFacingCamera() {
        return new CameraWizard(getBackFacingCameraId());
    }

    public static int getBackFacingCameraId() {
        CameraInfo info = new Camera.CameraInfo();
        for (int id = 0; id < Camera.getNumberOfCameras(); ++id) {
            Camera.getCameraInfo(id, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                return id;
            }
        }
        throw new IllegalStateException("No back facing camera");
    }

    private static Size getBestPreviewSize(Parameters parameters, int width, int height) {
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        float ratio = width / (float) height;
        Size bestSize = null;
        float closestMatch = Float.MAX_VALUE;
        for (Size size : sizes) {
            float match = Math.abs(size.width / (float) size.height - ratio);
            if (match < closestMatch) {
                closestMatch = match;
                bestSize = size;
            }
        }
        return bestSize;
    }

    public Size setPreviewSize(int width, int height) {
        Parameters parameters = camera.getParameters();
        Size size;
        if (orientation == 0 || orientation == 180) {
            size = getBestPreviewSize(parameters, width, height);
        } else {
            size = getBestPreviewSize(parameters, height, width);
        }
        parameters.setPreviewSize(size.width, size.height);
        camera.setParameters(parameters);
        return size;
    }

    public void setAutofocus() {
        Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        camera.setParameters(parameters);
    }

    public void setFps() {
        Parameters parameters = camera.getParameters();
        int[] best = {
                Integer.MIN_VALUE, Integer.MIN_VALUE
        };
        for (int[] range : parameters.getSupportedPreviewFpsRange()) {
            if (range[0] > best[0]) {
                best = range;
            }
        }
        Log.d("OnionCamera", "request fps: " + best[0] + " - " + best[1]);
        parameters.setPreviewFpsRange(best[0], best[1]);
        camera.setParameters(parameters);
    }

}
