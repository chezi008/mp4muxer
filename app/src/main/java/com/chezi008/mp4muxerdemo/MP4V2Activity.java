package com.chezi008.mp4muxerdemo;

import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.chezi008.mp4muxerdemo.decode.VideoDecoder;
import com.chezi008.mp4muxerdemo.file.FileConstant;
import com.chezi008.mp4muxerdemo.file.H264ReadRunable;
import com.chezi008.mp4muxerdemo.helper.MP4EncoderHelper;
import com.chezi008.mp4muxerdemo.utils.SPUtils;

import java.util.Arrays;

public class MP4V2Activity extends AppCompatActivity {
    String TAG = getClass().getSimpleName();

    public static final String VIDEO_KEY_SPS = "video_sps";
    public static final String VIDEO_KEY_PPS = "video_pps";

    private VideoDecoder mVideoDecode;
    private TextureView mTextureView;
    private boolean haveGetSpsInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4_v2);
        initVariable();
        initView();
    }

    private void initVariable() {
        MP4EncoderHelper.init(FileConstant.mp4FilePath, 1280, 720);
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

    private void initVideoCodec(SurfaceTexture surface) {
        mVideoDecode = new VideoDecoder(this);
        mVideoDecode.initCodec(new Surface(surface));
        mVideoDecode.start();
    }

    private void readLocalFile() {
        H264ReadRunable h264ReadRunable = new H264ReadRunable();
        h264ReadRunable.setH264ReadListener(new H264ReadRunable.H264ReadListener() {
            @Override
            public void onFrameData(byte[] datas) {
                mVideoDecode.decodeFrame(datas);
                if (haveGetSpsInfo) {
                    Log.d(TAG, "onFrameData: -->datas[4]:" + datas[4]);
                    MP4EncoderHelper.writeH264Data(datas, datas.length);
                    return;
                }
                //找sps和pps
                if ((datas[4] & 0x1f) == 7) {//sps
                    MP4EncoderHelper.writeH264Data(datas, datas.length);
                    SPUtils.saveObject(MP4V2Activity.this, VIDEO_KEY_SPS, datas);
                    Log.d(TAG, "onFrameData: ");
                } else if ((datas[4] & 0x1f) == 8) {//pps
                    MP4EncoderHelper.writeH264Data(datas, datas.length);
                    SPUtils.saveObject(MP4V2Activity.this, VIDEO_KEY_PPS, datas);
                }else if((datas[4] & 0x1f) == 5){
                    //第一帧为I帧
                    haveGetSpsInfo = true;
                    MP4EncoderHelper.writeH264Data(datas, datas.length);
                }
            }
            @Override
            public void onStopRead() {
                mVideoDecode.release();
                MP4EncoderHelper.close();
            }
        });
        Thread readFileThread = new Thread(h264ReadRunable);
        readFileThread.start();
    }
}
