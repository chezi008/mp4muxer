//
// Created by chezi on 2017/9/13.
//
#include <jni.h>
#include <mp4v2/mp4v2.h>
#include "MP4Encoder.h"
#include "android/log.h"

#define  LOG_TAG    "MP4EncoderHelper.cpp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__);

MP4Encoder mp4Encoder;
MP4FileHandle mp4FileHandle;

extern "C"
JNIEXPORT void JNICALL Java_com_chezi008_mp4muxerdemo_helper_MP4EncoderHelper_init
        (JNIEnv *env, jclass jclass, jstring path, jint width, jint height) {
    const char *local_title = env->GetStringUTFChars(path, NULL);
    int m_width = width;
    int m_height = height;
    mp4FileHandle = mp4Encoder.CreateMP4File(local_title, m_width, m_height);
    LOGI("MP4Encoder----->初始化成功");
}

extern "C"
JNIEXPORT jint JNICALL Java_com_chezi008_mp4muxerdemo_helper_MP4EncoderHelper_writeH264Data
        (JNIEnv *env, jclass clz, jbyteArray data, jint size) {
    jbyte *jb_data = env->GetByteArrayElements(data, JNI_FALSE);
    unsigned char *h264_data = (unsigned char *) jb_data;
    int result = mp4Encoder.WriteH264Data(mp4FileHandle, h264_data, size);
    LOGI("MP4Encoder----->添加数据 result:%d", result);
    return result;
}
/**
 * 释放
 */
extern "C"
JNIEXPORT void JNICALL Java_com_chezi008_mp4muxerdemo_helper_MP4EncoderHelper_close
        (JNIEnv *env, jclass clz) {
    mp4Encoder.CloseMP4File(mp4FileHandle);
    LOGI("MP4Encoder----->close");
}