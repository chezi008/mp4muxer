package com.chezi008.mp4muxerdemo;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.chezi008.mp4muxerdemo.decode.VideoDecoder;
import com.chezi008.mp4muxerdemo.encode.AudioEncoder;
import com.chezi008.mp4muxerdemo.file.FileConstant;
import com.chezi008.mp4muxerdemo.file.H264ReadRunable;
import com.chezi008.mp4muxerdemo.muxer.BaseMuxer;
import com.chezi008.mp4muxerdemo.utils.SPUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 描述：muxer合成Mp4界面
 * 作者：chezi008 on 2017/6/29 16:33
 * 邮箱：chezi008@qq.com
 */
public class MuxerMp4Activity extends AppCompatActivity {
    String TAG = getClass().getSimpleName();
    public static final String VIDEO_KEY_SPS = "video_sps";
    public static final String VIDEO_KEY_PPS = "video_pps";

    private VideoDecoder mVideoDecode;
    private AudioEncoder mAudioEncoder;
    private BaseMuxer mBaseMuxer;
    private TextureView mTextureView;

    private int videoTimeStamp;
    private int audioTimeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muxer_mp4);
        initView();

    }

    private void initVideoCodec(SurfaceTexture surface) {
        mVideoDecode = new VideoDecoder(this);
        mVideoDecode.initCodec(new Surface(surface));
        mVideoDecode.start();
    }

    private void initView() {
        Button btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readLocalFile();
            }
        });
        mTextureView = (TextureView) findViewById(R.id.tv_paly_view);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable: ");
                initVideoCodec(surface);
                initAudioCoder();
                initMediaMuxer();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed: ");
                mVideoDecode.release();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private long audioTimeUs;

    private void initAudioCoder() {
        mAudioEncoder = new AudioEncoder();
        try {
            mAudioEncoder.setSavePath(FileConstant.aacFilePath);
            mAudioEncoder.setAudioEnncoderListener(new AudioEncoder.AudioEnncoderListener() {
                @Override
                public void getAudioData(byte[] temp) {

                }

                @Override
                public void getAudioBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
//                    if (audioTimeUs == 0) {
//                        audioTimeUs = MuxerMp4Activity.this.bufferInfo.presentationTimeUs;
//                    } else {
//                        audioTimeUs += 1000 * 1000 / 30;
//                    }
                    if (videoTimeStamp == 0) {
                        return;
                    }
//                    audioTimeUs = audioTimeStamp++ * (1024 * 1000 * 1000 / 16000);
                    audioTimeStamp++;
//                    bufferInfo.presentationTimeUs = audioTimeUs;
//                    bufferInfo.offset += 7;
//                    bufferInfo.size -= 7;
                    mBaseMuxer.writeSampleData(byteBuffer, bufferInfo, false);
                }
            });
            mAudioEncoder.prepare();
            mAudioEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void initMediaMuxer() {
        mBaseMuxer = new BaseMuxer();
        mBaseMuxer.addVideoTrack(mVideoDecode.getMediaformat());
        mBaseMuxer.addAudioTrack(mAudioEncoder.getMediaFormat());
        mBaseMuxer.startMuxer();
    }

    private boolean haveGetSpsInfo;

    private void readLocalFile() {
        H264ReadRunable h264ReadRunable = new H264ReadRunable();
        h264ReadRunable.setH264ReadListener(new H264ReadRunable.H264ReadListener() {
            @Override
            public void onFrameData(byte[] datas) {
                mVideoDecode.decodeFrame(datas);
                addMuxerVideoData(datas);
                if (haveGetSpsInfo) {
                    return;
                }
                //找sps和pps
                Log.d(TAG, "onFrameData: " + datas);
                if ((datas[4] & 0x1f) == 7) {//sps
                    SPUtils.saveObject(MuxerMp4Activity.this, VIDEO_KEY_SPS, datas);
                    Log.d(TAG, "onFrameData: ");
                } else if ((datas[4] & 0x1f) == 8) {//pps
                    SPUtils.saveObject(MuxerMp4Activity.this, VIDEO_KEY_PPS, datas);
                    haveGetSpsInfo = true;
                }


            }

            @Override
            public void onStopRead() {
                mVideoDecode.release();
                mAudioEncoder.stop();
                mBaseMuxer.release();
            }
        });
        Thread readFileThread = new Thread(h264ReadRunable);
        readFileThread.start();
    }

    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private long timeUs = System.nanoTime() / 1000;

    private void addMuxerVideoData(byte[] datas) {

        if (mBaseMuxer == null) return;
        bufferInfo.offset = 0;
        bufferInfo.size = datas.length;
        if ((datas[4] & 0x1f) == 5) {
            Log.d(TAG, "onDecodeVideoFrame: -->I帧");
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
        } else if ((datas[4] & 0x1f) == 7 || (datas[4] & 0x1f) == 8) {
            Log.d(TAG, "addMuxerVideoData: -->sps or pps");
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        } else {
            bufferInfo.flags = 0;
        }
        ByteBuffer buffer = ByteBuffer.wrap(datas, bufferInfo.offset, bufferInfo.size);
        videoTimeStamp++;
//        bufferInfo.presentationTimeUs = videoTimeStamp++ * (1000 * 1000 / 30);
        mBaseMuxer.writeSampleData(buffer, bufferInfo, true);
    }
}
