package com.chezi008.mp4muxerdemo.decode;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.chezi008.mp4muxerdemo.MuxerMp4Activity;
import com.chezi008.mp4muxerdemo.utils.SPUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 描述：
 * 作者：chezi008 on 2017/6/30 10:03
 * 邮箱：chezi008@163.com
 */

public class VideoDecoder {
    private String TAG = getClass().getSimpleName();

    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;

    private MediaCodec mCodec;
    private MediaFormat mediaformat;
    private int mFrameRate = 30;

    private Context mContext;
    private long timeoutUs = 10000;

    private ByteBuffer[] inputBuffers;
    private MediaCodec.BufferInfo bufferInfo;

    public VideoDecoder(Context context) {
        this.mContext = context;
    }

    public void initCodec(Surface surface) {
        try {
            //通过多媒体格式名创建一个可用的解码器
            mCodec = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //初始化编码器
        mediaformat = MediaFormat.createVideoFormat("video/avc", VIDEO_WIDTH, VIDEO_HEIGHT);

        //设置帧率
        mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mCodec.configure(mediaformat, surface, null, 0);

    }

    public MediaFormat getMediaformat() {
        return mediaformat;
    }

    public void release() {
        Log.d(TAG, "releasing encoder objects");
        if (mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    public void start() {
        if (mCodec != null) {
            mCodec.start();

            inputBuffers = mCodec.getInputBuffers();
            bufferInfo = new MediaCodec.BufferInfo();
        }
    }

    public void decodeFrame(byte[] frame) {
        int inIndex = mCodec.dequeueInputBuffer(System.nanoTime() / 1000);
        if (inIndex >= 0) {
            ByteBuffer byteBuffer = inputBuffers[inIndex];
            byteBuffer.clear();
            byteBuffer.put(frame);
            mCodec.queueInputBuffer(inIndex, 0, frame.length, 0, 0);
        }
        int outIndex = mCodec.dequeueOutputBuffer(bufferInfo, timeoutUs);
        if (outIndex >= 0) {
            boolean doRender = (bufferInfo.size != 0);
            //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
            mCodec.releaseOutputBuffer(outIndex, doRender);
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

        }
    }
}
