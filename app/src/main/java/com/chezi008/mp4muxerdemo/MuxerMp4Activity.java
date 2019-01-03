package com.chezi008.mp4muxerdemo;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.chezi008.mp4muxer.Mp4Muxer;
import com.chezi008.mp4muxerdemo.decode.H264Decoder;
import com.chezi008.mp4muxerdemo.encode.AACEncoder;
import com.chezi008.mp4muxerdemo.file.FileConstant;
import com.chezi008.mp4muxerdemo.file.H264ReadRunable;
import com.chezi008.mp4muxerdemo.utils.SPUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * 描述：muxer合成Mp4界面
 * 作者：chezi008 on 2017/6/29 16:33
 * 邮箱：chezi008@qq.com
 */
public class MuxerMp4Activity extends AppCompatActivity {
    String TAG = getClass().getSimpleName();
    public static final String VIDEO_KEY_SPS = "video_sps";
    public static final String VIDEO_KEY_PPS = "video_pps";

    private H264Decoder mVideoDecode;
    private AACEncoder mAACEncoder;
    private Mp4Muxer mMp4Muxer;
    private TextureView mTextureView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muxer_mp4);
        initView();

    }

    private void initVideoCodec(SurfaceTexture surface) {
        mVideoDecode = new H264Decoder(this);
        mVideoDecode.initCodec(new Surface(surface));
        mVideoDecode.start();
    }

    private long startPts;
    private void initView() {
        Button btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initVideoCodec(mTextureView.getSurfaceTexture());
                initAudioCoder();
                readLocalFile();
            }
        });
        findViewById(R.id.btnRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initMediaMuxer();
            }
        });
        findViewById(R.id.btnStopRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMp4Muxer.stop();
                startRecord = false;
            }
        });
        mTextureView = findViewById(R.id.tv_paly_view);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable: ");
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

    private MediaFormat audioFormat;
    private void initAudioCoder() {
        mAACEncoder = new AACEncoder();
        try {
            mAACEncoder.setSavePath(FileConstant.aacFilePath);
            mAACEncoder.setAudioEnncoderListener(new AACEncoder.AudioEnncoderListener() {
                @Override
                public void getAudioBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                    if (startRecord&&startPts!=0){
                        bufferInfo.presentationTimeUs = System.nanoTime()/1000-startPts;
                        mMp4Muxer.writeAudioData(byteBuffer, bufferInfo);
                    }
                }

                @Override
                public void getOutputFormat(MediaFormat format) {
                    audioFormat = format;
                }
            });
            mAACEncoder.prepare();
            mAACEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void initMediaMuxer() {
        mMp4Muxer = new Mp4Muxer(FileConstant.mp4FilePath);
        mMp4Muxer.addVideoTrack(mVideoDecode.getMediaformat());
        if (audioFormat!=null){
            mMp4Muxer.addAudioTrack(audioFormat);
        }
        mMp4Muxer.start();
        startRecord = true;
    }

    private boolean haveGetSpsInfo;
    private boolean startRecord;
    private void readLocalFile() {
        H264ReadRunable h264ReadRunable = new H264ReadRunable();
        h264ReadRunable.setH264ReadListener(new H264ReadRunable.H264ReadListener() {
            @Override
            public void onFrameData(byte[] datas) {
                mVideoDecode.decodeFrame(datas);
                if (!startRecord){
                    return;
                }
                if (true) {
                    Log.d(TAG, "onFrameData: -->datas[4]:" + datas[4]);
                    addMuxerVideoData(datas);
                    return;
                }
                //找sps和pps
                if ((datas[4] & 0x1f) == 7) {//sps
                    addMuxerVideoData(datas);
                    SPUtils.saveObject(MuxerMp4Activity.this, VIDEO_KEY_SPS, datas);
                    Log.d(TAG, "onFrameData: ");
                } else if ((datas[4] & 0x1f) == 8) {//pps
                    addMuxerVideoData(datas);
                    SPUtils.saveObject(MuxerMp4Activity.this, VIDEO_KEY_PPS, datas);
                }else if((datas[4] & 0x1f) == 5){
                    //第一帧为I帧
                    haveGetSpsInfo = true;
                    addMuxerVideoData(datas);
                }

            }

            @Override
            public void onStopRead() {
                mVideoDecode.release();
                mAACEncoder.stop();
            }
        });
        Thread readFileThread = new Thread(h264ReadRunable);
        readFileThread.start();
    }


    private void addMuxerVideoData(byte[] datas) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        if (mMp4Muxer == null) return;
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
        long pts = System.nanoTime()/1000;
        if (startPts == 0) {
            startPts = pts;
        }
        bufferInfo.presentationTimeUs = System.nanoTime()/1000-startPts;
        mMp4Muxer.writeVideoData(buffer, bufferInfo);
    }

}
