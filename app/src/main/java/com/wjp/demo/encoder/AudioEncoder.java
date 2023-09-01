package com.wjp.demo.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;

import com.io.rtmp.RTMPMuxer;

import java.nio.ByteBuffer;

public class AudioEncoder extends AbsEncoder {

    private static final String TAG = "AudioEncoder";

    private static final int SAMPLE_RATE = 44100;
    private static final int BIT_RATE = 64000;

    private AudioRecord mAudioRecord;
    private byte[] buffer = new byte[1024];

    public AudioEncoder() {
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        int source = MediaRecorder.AudioSource.MIC;
        int bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(source, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize);
    }

    @Override
    public void start() {
        mMediaCodec.start();
        mAudioRecord.startRecording();
    }

    @Override
    public void stop() {
        mAudioRecord.stop();
        mMediaCodec.stop();
        mAudioRecord.release();
        mMediaCodec.release();
    }

    @Override
    public void queueInputBuffer() {
        int len = mAudioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
        if (len <= 0) {
            return;
        }
        drain(buffer, len);
    }

    @Override
    public void writeSampleData(RTMPMuxer rtmpMuxer) {
        int index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
        if (index >= 0) {
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            } else {

                ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
                mBufferInfo.presentationTimeUs = getPTSUs();
                mMediaMuxer.writeSampleData(track, buffer, mBufferInfo);

                if (rtmpMuxer != null && rtmpMuxer.isConnected()) {
                    byte[] outData = new byte[mBufferInfo.size];
                    buffer.get(outData);
                    rtmpMuxer.writeAudio(outData, 0, outData.length, System.currentTimeMillis());
                }
            }

            mMediaCodec.releaseOutputBuffer(index, false);
            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                Log.e(TAG, "BUFFER_FLAG_END_OF_STREAM");
                mEnd = true;
            }
        }
    }

    @Override
    public void endOfStream() {
        while (!drain(null, 0)) ;
    }

    private boolean drain(byte[] buffer, int size) {
        int index = mMediaCodec.dequeueInputBuffer(0);
        if (index >= 0) {
            if (buffer == null) {
                mMediaCodec.queueInputBuffer(index, 0, 0, getPTSUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                inputBuffer.put(buffer, 0, size);
                mMediaCodec.queueInputBuffer(index, 0, size, getPTSUs(), 0);
            }
            return true;
        }
        return false;
    }

}
