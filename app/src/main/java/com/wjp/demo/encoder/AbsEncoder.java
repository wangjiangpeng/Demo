package com.wjp.demo.encoder;

import android.media.MediaCodec;
import android.media.MediaMuxer;

import com.io.rtmp.RTMPMuxer;

public abstract class AbsEncoder {

    protected int track;
    protected MediaCodec mMediaCodec;
    protected MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    protected boolean mEnd = false;
    private long mPrevOutputPTSUs = 0;
    protected MediaMuxer mMediaMuxer;

    public abstract void start();
    public abstract void stop();
    public abstract void queueInputBuffer();
    public abstract void writeSampleData(RTMPMuxer rtmpMuxer);
    public abstract void endOfStream();

    public boolean addTrack(MediaMuxer mediaMuxer) {
        int i = 0;
        while(i++ < 10){
            int index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 100000);
            if(index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                mMediaMuxer = mediaMuxer;
                track =  mediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                return true;
            } else if(index >= 0){
                mMediaCodec.releaseOutputBuffer(index, false);
            }
        }
        return false;
    }

    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        if (result > mPrevOutputPTSUs)
            mPrevOutputPTSUs = result;
        return mPrevOutputPTSUs;
    }

    public boolean isEnd() {
        return mEnd;
    }

}
