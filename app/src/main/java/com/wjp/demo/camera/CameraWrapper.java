package com.wjp.demo.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CameraWrapper {

    public interface OnTakePictureListener{
        void onTakePicture(String path);
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession(cameraDevice);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            if(mCaptureSession != null){
                try {
                    mCaptureSession.stopRepeating();
                    mCaptureSession.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cameraDevice.close();
            cameraErr = true;
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image mImage = reader.acquireNextImage();
            if (takePicture) {
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                ByteArrayOutputStream baos = null;
                try {
                    if (mLensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Matrix m = new Matrix();
                        m.postScale(-1, 1);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                        baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        bytes = baos.toByteArray();
                    }
                    savePicture(bytes);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (baos != null) {
                        try {
                            baos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                takePicture = false;
            }
            mImage.close();
        }
    };

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private boolean cameraErr = false;
    private String mCameraId;
    private ArrayList<Surface> mSurfaces = new ArrayList<>();
    private boolean takePicture = false;
    private ImageReader mImageReader;
    private int mLensFacing;
    private String mPhotoPath;

    private OnTakePictureListener mOnTakePictureListener;

    public CameraWrapper(String id, ArrayList<Surface> ss, int width, int height){
        mCameraId = id;
        mSurfaces.addAll(ss);
    }

    @SuppressLint("MissingPermission")
    public void openCamera(Context context){
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(String.valueOf(mCameraId));
            mLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

            cameraManager.openCamera(mCameraId, mStateCallback, new Handler(Looper.getMainLooper()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeCamera(){
        try {
            if(mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mCameraDevice.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takePicture(String path) {
        mPhotoPath = path;
        takePicture = true;
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            mCaptureSession.capture(captureBuilder.build(), null, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnTakePictureListener(OnTakePictureListener listener){
        mOnTakePictureListener = listener;
    }

    protected void savePicture(byte[] data) {
        try {
            File file = new File(mPhotoPath);
            FileOutputStream output = new FileOutputStream(file);
            output.write(data);
            output.close();

            if(mOnTakePictureListener != null){
                mOnTakePictureListener.onTakePicture(mPhotoPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession(CameraDevice cameraDevice) {
        try {
            mImageReader = ImageReader.newInstance(640, 480, ImageFormat.YV12, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
            mSurfaces.add(mImageReader.getSurface());

            CaptureRequest.Builder mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            for(Surface s : mSurfaces){
                mPreviewRequestBuilder.addTarget(s);
            }

            cameraDevice.createCaptureSession(mSurfaces,
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (null == cameraDevice) {
                                return;
                            }
                            try {
                                mCaptureSession = cameraCaptureSession;
                                cameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }

                    }, null
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
