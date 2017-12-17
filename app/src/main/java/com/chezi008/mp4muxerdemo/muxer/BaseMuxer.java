package com.chezi008.mp4muxerdemo.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.chezi008.mp4muxerdemo.file.FileConstant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

/**
 * 描述：mp4合成器
 * 作者：chezi008 on 2017/7/3 16:11
 * 邮箱：chezi008@163.com
 */

public class BaseMuxer {
    public static final boolean VERBOSE = true;
    private String TAG = getClass().getSimpleName();

    private MediaMuxer mMuxer;
    private int mVideoTrackIndex, mAudioTrackIndex;

    public BaseMuxer() {
        try {
            mMuxer = new MediaMuxer(FileConstant.mp4FilePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
    }

    public synchronized void addVideoTrack(MediaFormat mediaFormat) {
        if (mVideoTrackIndex != -1)
            throw new RuntimeException("already add video tracks");
        mVideoTrackIndex = mMuxer.addTrack(mediaFormat);
    }

    public synchronized void addAudioTrack(MediaFormat mediaFormat) {
        if (mAudioTrackIndex != -1)
            throw new RuntimeException("already add audio tracks");
        mAudioTrackIndex = mMuxer.addTrack(mediaFormat);
    }

    public synchronized void startMuxer() {
        mMuxer.start();
    }

    private long systemTimeStamp;
    private boolean isInitTimeStamp;

    public synchronized void writeSampleData(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        if (mAudioTrackIndex == -1 || mVideoTrackIndex == -1) {
            Log.i(TAG, String.format("pumpStream [%s] but muxer is not start.ignore..", isVideo ? "video" : "audio"));
            return;
        }
        //初始化时间
        if (!isInitTimeStamp) {
            systemTimeStamp = System.currentTimeMillis();
            isInitTimeStamp = true;
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
        } else if (bufferInfo.size != 0) {


            if (isVideo && mVideoTrackIndex == -1) {
                throw new InvalidParameterException("muxer hasn't started");
            }

            // adjust the ByteBuffer values to match BufferInfo (not needed?)
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

            int currentTimeStamp = (int) (System.currentTimeMillis()-systemTimeStamp);
            bufferInfo.presentationTimeUs = currentTimeStamp*1000;

            if (VERBOSE)
                Log.d(TAG, String.format("sent %s [" + bufferInfo.size + "] with timestamp:[%d] to muxer", isVideo ? "video" : "audio", bufferInfo.presentationTimeUs ));
            mMuxer.writeSampleData(isVideo ? mVideoTrackIndex : mAudioTrackIndex, outputBuffer, bufferInfo);

        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM received");
        }
    }

    public synchronized void release() {
        if (mMuxer != null) {
            if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1) {
                if (VERBOSE)
                    Log.i(TAG, String.format("muxer is started. now it will be stoped."));
                try {
                    mMuxer.stop();
                    mMuxer.release();
                } catch (IllegalStateException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
