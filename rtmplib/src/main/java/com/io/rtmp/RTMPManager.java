package com.io.rtmp;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class RTMPManager {

    private static final String TAG = "RTMPManager";
    private static final String MIME_TYPE = "video/avc";
    private final static int BITRETE = 2000000;
    private final static int FRAME_RATE = 20; // fps
    private final static int IFRAME_INTERVAL = 2;//关键帧间隔2s

    private static RTMPManager sRTMPManager;

    public static RTMPManager getInstance() {
        if (sRTMPManager == null) {
            sRTMPManager = new RTMPManager();
        }
        return sRTMPManager;
    }

    private final LinkedBlockingQueue<byte[]> mQueue = new LinkedBlockingQueue<>(4);//写队列

    MediaCodec mMediaCodec;
    Surface mSurface;
   // RTMPMuxer mRTMPMuxer;
    String mUrl;
    int mWidth;
    int mHeight;

    RTMPThread mRTMPThread;
    MediaCodecThread mMediaCodecThread;

    boolean upload = false;

    private RTMPManager() {
       // mRTMPMuxer = new RTMPMuxer();
    }

    public void setUrl(String url) {
        this.mUrl = url;

    }

    public void setSize(int w, int h) {
        mWidth = w;
        mHeight = h;

        if(mMediaCodec == null){
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BITRETE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            try {
                mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mMediaCodec.createInputSurface();
        }
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void start() {
        if (mRTMPThread == null) {
            mRTMPThread = new RTMPThread();
            mRTMPThread.start();
        }

        if (mMediaCodecThread == null) {
            mMediaCodecThread = new MediaCodecThread();
            mMediaCodecThread.start();
        }
    }

    public void stop() {
        if (mRTMPThread != null) {
            mRTMPThread.running = false;
            mRTMPThread = null;
        }

        if (mMediaCodecThread != null) {
            mMediaCodecThread.running = false;
            mMediaCodecThread = null;
        }
    }

    public boolean isUpload(){
        return upload;
    }

    public void reset(){
        upload = false;
    }

    public class MediaCodecThread extends Thread {
        boolean running = true;

        @Override
        public void run() {
            super.run();
            try {
                long timeStamp = 0;
                mMediaCodec.start();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                while (running) {
                    if (timeStamp != 0) {
                        if (System.currentTimeMillis() - timeStamp >= 2000) {
                            Bundle params = new Bundle();
                            //立即刷新 让下一帧是关键帧
                            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                            mMediaCodec.setParameters(params);
                            timeStamp = System.currentTimeMillis();
                        }
                    } else {
                        timeStamp = System.currentTimeMillis();
                    }

                    int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10);
                    if (index >= 0) {
                        ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
                        byte[] outData = new byte[bufferInfo.size];
                        buffer.get(outData);
                        mQueue.put(outData);
                        mMediaCodec.releaseOutputBuffer(index, false);
                    }
                }
                mMediaCodec.stop();
            } catch (Exception e) {

            }
        }
    }

    public class RTMPThread extends Thread {

        boolean running = true;

        @Override
        public void run() {
            super.run();


            int res = 0;
            int retry = 3;
            RTMPMuxer muxer = new RTMPMuxer();
            while(retry-- > 0){
                res = muxer.open(mUrl, mWidth, mHeight);
                if(res < 0){
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    break;
                }
            }

            if(res < 0){
                RTMPManager.this.stop();
                return;
            }
            upload = true;
            Log.e(TAG, "open:" + res);
            while (running && muxer.isConnected()) {
                byte[] data = mQueue.poll();
                if(data != null){
                    muxer.writeVideo(data, 0, data.length, System.currentTimeMillis());
                }
            }
            muxer.close();
            Log.e(TAG, "close");
        }
    }


}
