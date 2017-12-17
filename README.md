## Android使用Jni mp4v2库将h264裸流合成mp4文件

### 建议使用场景
#### 一般视频流有如下两种途径获取：
1. Android摄像头采集
2. 服务端传输过来的视频流

如果数据由本机摄像头直接采集，建议使用MediaMuxer类去实现mp4的合成。如果是服务端传输过来的视频流可以使用mp4v2的方法实现mp4的合成。我在项目里面也简单的利用MediaMuxer编写了一个Demo。可能写的不是很详细，功能也不是很完善。所以有什么问题还是多多希望指出，一起改进。

### mp4v2的So文件
1. 我demo里面只导入了armeabi 指令集的so包，需要其他类型的可以自己去官网下载进行打包。
2. 将mp4v2的头文件放在libs里面的include文件夹下面，所以再cmake文件里面需要增加`include_directories(libs/include)`命令。
### 使用c++将mp4v2的使用封装了一层
封装了如下几个方法：

	// open or creat a mp4 file.
	MP4FileHandle CreateMP4File(const char *fileName,int width,int height,int timeScale = 90000,int frameRate = 25);
	// wirte 264 metadata in mp4 file.
	bool Write264Metadata(MP4FileHandle hMp4File,LPMP4ENC_Metadata lpMetadata);
	// wirte 264 data, data can contain  multiple frame.
	int WriteH264Data(MP4FileHandle hMp4File,const unsigned char* pData,int size);
	// close mp4 file.
	void CloseMP4File(MP4FileHandle hMp4File);
	// convert H264 file to mp4 file.
	// no need to call CreateMP4File and CloseMP4File,it will create/close mp4 file automaticly.
	bool WriteH264File(const char* pFile264,const char* pFileMp4);

方法说明：

1. CreateMP4File：创建需要写入的mp4文件，可以设置视频的分辨率。默认的时间刻度是90000，帧率是25帧
2. WriteH264Data:写入h264数据，可以写入byte[]类型的数据流，也可是LPMP4ENC_Metadata(sps pps)类型数据。
3. CloseMP4File：转换完成后记得要释放内存，调用次方法
4. WriteH264File：直接将本地h264文件转换成mp4。实现方法是一样的，只是在C++代码里面实现了，将h264数据分割成一帧一帧，再写入至输入文件中。

### 一、mp4v2 方法的使用
同样，我在代码中封装了三个本地方法：

	public static native void init(String mp4FilePath, int widht, int height);

    public static native int writeH264Data(byte[] data, int size);

    public static native void close();
从方法名，可以看得出怎么使用，所以在这里就不多赘述了。项目里面提供了标准的h264测试文件mtv.h264。

### 二、MediaMuxer合成Mp4
- 官方文档介绍：http://www.loverobots.cn/android-api/reference/android/media/MediaMuxer.html
```
 MediaMuxer muxer = new MediaMuxer("temp.mp4", OutputFormat.MUXER_OUTPUT_MPEG_4);
 // More often, the MediaFormat will be retrieved from MediaCodec.getOutputFormat()
 // or MediaExtractor.getTrackFormat().
 MediaFormat audioFormat = new MediaFormat(...);
 MediaFormat videoFormat = new MediaFormat(...);
 int audioTrackIndex = muxer.addTrack(audioFormat);
 int videoTrackIndex = muxer.addTrack(videoFormat);
 ByteBuffer inputBuffer = ByteBuffer.allocate(bufferSize);
 boolean finished = false;
 BufferInfo bufferInfo = new BufferInfo();

 muxer.start();
 while(!finished) {
   // getInputBuffer() will fill the inputBuffer with one frame of encoded
   // sample from either MediaCodec or MediaExtractor, set isAudioSample to
   // true when the sample is audio data, set up all the fields of bufferInfo,
   // and return true if there are no more samples.
   finished = getInputBuffer(inputBuffer, isAudioSample, bufferInfo);
   if (!finished) {
     int currentTrackIndex = isAudioSample ? audioTrackIndex : videoTrackIndex;
     muxer.writeSampleData(currentTrackIndex, inputBuffer, bufferInfo);
   }
 };
 muxer.stop();
 muxer.release();
```
#### 使用中一些需要注意的地方

	1. MediaFormat 可以在初始化编码器的时候获取
```
mediaformat = MediaFormat.createVideoFormat("video/avc", VIDEO_WIDTH, VIDEO_HEIGHT);
```
	2. 写入数据时候的inputBuffer 和 bufferInfo 需要自己构造

### 三、注意
1. 不熟悉cmake编译的同学，可以去查查资料，反正我在项目里面使用so文件遇到过比较大的坑，因为一直不熟悉cmake的语法，链接库都不会。
2. 还有就是对视频流数据结构的理解，这玩意是一帧一帧组成的。不同类型的帧需要不同的处理。比如第一帧需要传入I帧，你得先去将视频流分割成一帧一帧的，再去判断帧的类型。不太熟悉的同学可以[传送门](http://blog.csdn.net/dittychen/article/details/55509718)一下。**切记传入数据以帧为单位**


### 四、Q&S
1. 录制的视频或快或慢。
在 MP4Encoder::CreateMP4File本地方法中有一个m_nFrameRate变量，控制帧率的，也就是每分钟多少帧，这里可以自己去控制，和录制视频的帧率一致就行了，这里默认的是25帧。
2. 录制的本地mp4视频预览画面是黑色或者是绿色的
造成的原因是录制视频流的时候第一帧不是关键帧（I帧）,所以在使用writeH264Data方法的时候，记得第一帧传入关键帧。
3. download下载的项目无法运行
。。。这个，你就自己去配置编译环境了，代码都在这了。
4. 关于音频写入的问题
未完待续。。。
5. 关于音视频同步的问题
未完待续。。。