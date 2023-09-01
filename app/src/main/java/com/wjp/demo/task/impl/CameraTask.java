package com.wjp.demo.task.impl;

import android.util.Log;
import android.view.Surface;
import android.view.TextureView;


import com.wjp.demo.AApplication;
import com.wjp.demo.camera.CameraWrapper;
import com.wjp.demo.encoder.MediaWrapper;
import com.wjp.demo.task.ATask;

import java.util.ArrayList;

public class CameraTask extends ATask<Long> {

    private static final String TAG = "CameraRecordTask";

    private int width = 640;
    private int height = 480;

    private String camId;
    private TextureView view;
    private CameraWrapper mCameraWrapper;
    private MediaWrapper mMediaWrapper;

    private Object mLock = new Object();

    @Override
    protected Object doInBackground(Object... objs) {
        Log.e(TAG, "CameraRecordTask");
        camId = (String)objs[0];
        view = (TextureView)objs[1];

        ArrayList list = new ArrayList<Surface>();
        list.add(new Surface(view.getSurfaceTexture()));

        mMediaWrapper = new MediaWrapper(false, true, width, height, "/mnt/sdcard/test.mp4", null);
        list.add(mMediaWrapper.getSurface());

        mCameraWrapper = new CameraWrapper(camId, list, width, height);
        mCameraWrapper.openCamera(AApplication.getInstance());
        mMediaWrapper.startRecord();

        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mCameraWrapper.closeCamera();
        mMediaWrapper.stopRecord();
        mMediaWrapper.waitStop();
        return null;
    }

    public void stopRecord() {
        synchronized (mLock) {
            mLock.notify();
        }
    }
}
