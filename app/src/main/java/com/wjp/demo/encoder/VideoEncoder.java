package com.wjp.demo.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.io.rtmp.RTMPMuxer;

import java.nio.ByteBuffer;

public class VideoEncoder extends AbsEncoder {

    private static final String TAG = "VideoEncoder";

    private final static int FRAME_RATE = 20; // fps
    private final static int IFRAME_INTERVAL = 2;//关键帧间隔2s
    private final static int BYTE_RATE = 256000;

    private Surface mSurface;
    private int mWidth, mHeight;
    private long timeStamp;
    private boolean mHaveKeyFrame = false;

    public VideoEncoder(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;

        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
//        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel4);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BYTE_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, 4);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mMediaCodec.createInputSurface();

    }

    @SuppressLint("MissingPermission")
    @Override
    public void start() {
        mMediaCodec.start();
    }

    @Override
    public void stop() {
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public void queueInputBuffer() {
    }

    @Override
    public void writeSampleData(RTMPMuxer rtmpMuxer) {
        // reset key frame
        if (System.currentTimeMillis() - timeStamp >= 2_000) {
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            mMediaCodec.setParameters(params);
            timeStamp = System.currentTimeMillis();
        }
        int index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, -1);
        if (index >= 0) {
            mHaveKeyFrame |= ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0);
            if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                if (mHaveKeyFrame) {
                    ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    mMediaMuxer.writeSampleData(track, buffer, mBufferInfo);

                    if (rtmpMuxer != null && rtmpMuxer.isConnected()) {
                        byte[] outData = new byte[mBufferInfo.size];
                        buffer.get(outData);
                        rtmpMuxer.writeVideo(outData, 0, outData.length, System.currentTimeMillis());
                    }
                }
            } else {
                Log.e(TAG, "BUFFER_FLAG_END_OF_STREAM");
                mEnd = true;
            }
            mMediaCodec.releaseOutputBuffer(index, false);
        }
    }

    @Override
    public void endOfStream() {
        mMediaCodec.signalEndOfInputStream();
    }

}
