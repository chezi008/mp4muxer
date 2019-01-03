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

public class H264Decoder {
    private String TAG = getClass().getSimpleName();

    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;

    private MediaCodec mCodec;
    private MediaFormat mediaformat;
    private int mFrameRate = 30;
    private Boolean UseSPSandPPS = true;

//    private MediaMuxer mMuxer;
//    private int mTrackIndex;

    private ByteBuffer mCSD0;
    private ByteBuffer mCSD1;
    private Context mContext;
    private long timeoutUs = 10000;

    private ByteBuffer[] inputBuffers;

    public H264Decoder(Context context) {
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
        //获取h264中的pps及sps数据
        if (UseSPSandPPS) {
            byte[] header_default_sps = {0, 0, 0, 1, 103, 100, 0, 31, -84, -76, 2, -128, 45, -56};
            byte[] header_default_pps = {0, 0, 0, 1, 104, -18, 60, 97, 15, -1, -16, -121, -1, -8, 67, -1, -4, 33, -1, -2, 16, -1, -1, 8, 127, -1, -64};

            SPUtils spUtils = new SPUtils();
            //读取之前保存的sps 和pps
            byte[] header_sps = (byte[]) spUtils.readObject(mContext, MuxerMp4Activity.VIDEO_KEY_SPS);
            byte[] header_pps = (byte[]) spUtils.readObject(mContext, MuxerMp4Activity.VIDEO_KEY_PPS);

            if (header_sps != null) {
                mCSD0 = ByteBuffer.wrap(header_sps);
                mCSD1 = ByteBuffer.wrap(header_pps);
            } else {
                mCSD0 = ByteBuffer.wrap(header_default_sps);
                mCSD1 = ByteBuffer.wrap(header_default_pps);
                mCSD0.clear();
                mCSD1.clear();
            }
            //设置sps和pps 如果设置不正确会导致合成的mp4视频作为文件预览的时候，预览图片是黑色的
            //视频进度条拖拽画面会出现绿色，以及块状现象
            mediaformat.setByteBuffer("csd-0", mCSD0);
            mediaformat.setByteBuffer("csd-1", mCSD1);

        }
//        mediaformat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        mediaformat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1920 * 1080);
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

        }
    }
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    public void decodeFrame(byte[] frame) {
        long pts = System.nanoTime() / 1000;
        int inIndex = mCodec.dequeueInputBuffer(-1);
        if (inIndex >= 0) {
            ByteBuffer byteBuffer = inputBuffers[inIndex];
            byteBuffer.clear();
            byteBuffer.put(frame);
            mCodec.queueInputBuffer(inIndex, 0, frame.length, pts, 0);
        }
        int outIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        if (outIndex >= 0) {
            boolean doRender = (bufferInfo.size != 0);
            //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
            mCodec.releaseOutputBuffer(outIndex, doRender);
//            Log.d(TAG, "video: pts:"+bufferInfo.presentationTimeUs+",rPts:"+pts);
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        }
    }

    public interface H264DecoderListener{
        void ondecode(byte[] out,MediaCodec.BufferInfo info);
    }
}
