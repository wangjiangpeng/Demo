package com.wjp.demo.encoder;

import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.io.rtmp.RTMPMuxer;

import java.util.ArrayList;

public class MediaWrapper {

    private static final String TAG = "MediaWrapper";

    private ReadThread mReadThread;
    private String savePath;
    private String url;
    private int width;
    private int height;
    private boolean isConnected = false;
    private boolean isRunning = false;
    private Object mLock = new Object();

    private Surface mSurface;
    private ArrayList<AbsEncoder> mList = new ArrayList<>();

    private MediaWrapper(boolean hasAudio, boolean hasVideo, int width, int height, String path, String url) {
        this.width = width;
        this.height = height;
        this.savePath = path;
        this.url = url;
        if(hasAudio){
            mList.add(new AudioEncoder());
        }
        if(hasVideo){
            VideoEncoder encoder = new VideoEncoder(width, height);
            mSurface = encoder.getSurface();
            mList.add(encoder);
        }
    }

    public Surface getSurface(){
        return mSurface;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void startRecord() {
        if (mReadThread == null) {
            isRunning = true;
            isConnected = false;
            mReadThread = new ReadThread();
            mReadThread.start();
        }
    }

    public void stopRecord() {
        if (mReadThread != null) {
            mReadThread.recording = false;
            mReadThread = null;
        }
    }

    public void waitStop() {
        if (isRunning) {
            synchronized (mLock) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public class ReadThread extends Thread {

        private ArrayList<MuxerThread> mThreadList = new ArrayList<>();
        boolean recording = true;

        private void startEncoder(){
            for(AbsEncoder encoder : mList){
                encoder.start();
            }
        }

        private boolean addTrack(MediaMuxer mediaMuxer){
            boolean ret = true;
            for(AbsEncoder encoder : mList){
                ret &= encoder.addTrack(mediaMuxer);
            }
            return ret;
        }

        private void startThread(RTMPMuxer rtmpMuxer){
            for(AbsEncoder encoder : mList){
                MuxerThread thread = new MuxerThread(encoder, isConnected ? rtmpMuxer : null);
                thread.start();
                mThreadList.add(thread);
            }
        }

        private void queueInputBuffer(){
            for(AbsEncoder encoder : mList){
                encoder.queueInputBuffer();
            }
        }

        private void endOfStream(){
            for(AbsEncoder encoder : mList){
                encoder.endOfStream();
            }
        }

        private void joins(){
            for(MuxerThread thread : mThreadList){
                try {
                    thread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void releaseAll(MediaMuxer mediaMuxer){
            if(mediaMuxer != null){
                mediaMuxer.release();
            }
            for(AbsEncoder encoder : mList){
                encoder.stop();
            }
        }

        @Override
        public void run() {
            super.run();

            Log.e(TAG, "ReadThread start");
            try {
                startEncoder();
                MediaMuxer mediaMuxer = null;
                try {
                    mediaMuxer = new MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    if(!addTrack(mediaMuxer)){
                        Log.e(TAG, "addTrack err");
                        releaseAll(mediaMuxer);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    releaseAll(mediaMuxer);
                    return;
                }

                RTMPMuxer rtmpMuxer = new RTMPMuxer();
                if(url != null){
                    int ret = rtmpMuxer.open(url, width, height);
                    isConnected = (ret >= 0);
                    Log.e(TAG, "rtmpMuxer open ret:" + ret);
                }

                mediaMuxer.start();
                startThread(rtmpMuxer);

                while (recording) {
                    queueInputBuffer();
                    // video do not queue, because it is auto queue
                }
                endOfStream();
                joins();

                if (isConnected) {
                    rtmpMuxer.close();
                }

                mediaMuxer.stop();
                releaseAll(mediaMuxer);

            } catch (Exception e) {
                e.printStackTrace();
            }
            isRunning = false;
            synchronized (mLock) {
                mLock.notify();
            }
            Log.e(TAG, "ReadThread stop");
        }
    }

    public class MuxerThread extends Thread {

        AbsEncoder mEncoder;
        RTMPMuxer mRtmpMuxer;

        MuxerThread(AbsEncoder encoder, RTMPMuxer rtmpMuxer) {
            mEncoder = encoder;
            mRtmpMuxer = rtmpMuxer;
        }

        @Override
        public void run() {
            super.run();

            while (!mEncoder.isEnd()) {
                mEncoder.writeSampleData(mRtmpMuxer);
            }
        }
    }

}
