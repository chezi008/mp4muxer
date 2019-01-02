package com.chezi008.mp4muxerdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chezi008.mp4muxerdemo.hw.HWRecorderActivity;

/**
 * 描述：
 * 作者：chezi008 on 2017/6/29 16:32
 * 邮箱：chezi008@qq.com
 */
public class MainActivity extends AppCompatActivity {
    String TAG = getClass().getSimpleName();
    private static final int MY_PERMISSION_REQUEST_CODE = 10000;
    private String[] requestPermissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        getPermission();
    }

    private void getPermission() {
        /**
         * 第 1 步: 检查是否有相应的权限
         */
        boolean isAllGranted = checkPermissionAllGranted(
                requestPermissions
        );
        // 如果这3个权限全都拥有, 则直接执行备份代码
        if (isAllGranted) {
            return;
        }
        // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
        ActivityCompat.requestPermissions(
                this,
                requestPermissions,
                MY_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                Log.d(TAG, "checkPermissionAllGranted: -------->permission权限没有获取成功："+permission);
                return false;
            }
        }
        return true;
    }

    private void initView() {
        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        //android muxer shengcheng mp4
        Button btn_muxer = (Button) findViewById(R.id.btn_muxer);
        btn_muxer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MuxerMp4Activity.class);
                startActivity(intent);
            }
        });

        Button btn_mp4v2 = (Button) findViewById(R.id.btn_mp4v2);
        btn_mp4v2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MP4V2Activity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnHw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HWRecorderActivity.class);
                startActivity(intent);
            }
        });
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
