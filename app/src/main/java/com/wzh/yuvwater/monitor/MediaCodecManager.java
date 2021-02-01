package com.wzh.yuvwater.monitor;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.util.Pools;

import com.wzh.yuvwater.jni.YuvOsdUtils;
import com.wzh.yuvwater.utils.Logger1;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

@TargetApi(Build.VERSION_CODES.M)
class MediaCodecManager {
    public static boolean DEBUG = false;

    private static final String TAG = "MediaCodecManager";
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    //
    private ArrayBlockingQueue<byte[]> frameBytes;
    private MediaCodec mMediaCodec;
    private int mColorFormat;
    private long mStartTime = 0;
    private MediaFormat mediaFormat;
    private volatile boolean isStart = false;
    private volatile boolean isPause = false;
    private boolean isAddOSD = true;
    private int dstWidth, dstHeight;

    private static MediaCodecManager sInstance;
    private HandlerThread mHandlerThread;
    private SimpleDateFormat mFormat;

    private MuxerManager mMuxerManager;
    private boolean isInitCodec;

    private boolean isFlush = false;

    private long lastPauseTime = -1;//上次暂停时间
    private boolean isHasKeyFrame;

    private Handler mHandler;

    private int off_y = 50, off_x = 100;
    private int rotation;
    private Pools.SimplePool<MuxerData> mPools;

    private MediaCodecManager() {
        mHandlerThread = new HandlerThread("codecThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public static MediaCodecManager getInstance() {
        if (sInstance == null) {
            synchronized (MediaCodecManager.class) {
                if (sInstance == null)
                    sInstance = new MediaCodecManager();
            }
        }
        return sInstance;
    }

    /**
     * 初始化编码器
     *
     * @param dstWidth
     * @param dstHeight
     */
    public void initCodecManager(int dstWidth, int dstHeight, int rotation) {
        isFlush = false;
        if (!isInitCodec) {
            Logger1.i(TAG, "initCodecManager: width=%s*%s rotation=%s", dstWidth, dstHeight, rotation);
            mHandler.post(() -> {
                mMuxerManager = MuxerManager.getInstance();

                frameBytes = new ArrayBlockingQueue<>(100);
                mPools=new Pools.SynchronizedPool<>(50);//缓存对象池

                MediaCodecManager.this.dstWidth = dstWidth;
                MediaCodecManager.this.dstHeight = dstHeight;
                MediaCodecManager.this.rotation = rotation;

                prepare();
//                String pattern = "yyyy年MM月dd日 HH:mm:ss";//日期格式 年月日
                String pattern = "yyyy-MM-dd HH:mm:ss";//日期格式
                mFormat = new SimpleDateFormat(pattern, Locale.CHINA);

                YuvOsdUtils.initOsd(off_x, off_y, pattern.length(), dstWidth, dstHeight, rotation);

                isInitCodec = true;
            });
        }
    }

    /**
     * 开始编码器
     */
    public void startMediaCodec() {
        if (isStart) {
            Logger1.i(TAG, "startMediaCodec: was started");
            return;
        }
        mHandler.post(() -> {
            start();
        });
    }
    long pauseTime;
    /**
     * 暂停录制
     */
    public void pauseMediaCodec() {
        if (!isStart || isPause) {
            Logger1.i(TAG, "MediaCodec: isn't started");
            return;
        }
        isPause=true;
        pauseTime=System.nanoTime();
        frameBytes.clear();
    }
    private long mTime;

    /**
     * 继续录制
     */
    public void resumeMediaCodec() {
        if (!isStart || !isPause) {
            Logger1.i(TAG, "MediaCodec: was started");
            return;
        }
        isPause=false;
        mTime+=System.nanoTime()-pauseTime;
    }

    public void releaseManager() {
        mHandler.post(()->{
            if(mMediaCodec!=null){
                frameBytes.clear();
                mHandlerThread.quit();
                YuvOsdUtils.releaseOsd();
                stopMediaCodec();
                sInstance=null;
            }
        });


    }

    public void addFrameData(byte[] data) {
        if (isStart && !isPause) {
            boolean isOffer = frameBytes.offer(data);
//            Logger1.i(TAG, "addFrameData: isOffer=%s", isOffer);
            if (!isOffer) {
                frameBytes.poll();
                frameBytes.offer(data);
            }
        }
    }

    /**
     * 准备一些需要的参数
     * <p>
     * YV12: YYYYYYYY VV UU    =>YUV420P
     * NV12: YYYYYYYY UVUV     =>YUV420SP
     * NV21: YYYYYYYY VUVU     =>YUV420SP
     * create at 2017/3/22 18:13
     */
    private void prepare() {

        mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;//nv12 最终数据需要nv12

        int videoW = rotation == 90 || rotation == 270 ? dstHeight : dstWidth;
        int videoH = rotation == 90 || rotation == 270 ? dstWidth : dstHeight;
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,//注意这里旋转后有一个大坑，就是要交换mHeight，和mWidth的位置。否则录制出来的视频时花屏的。
                videoW, videoH);
        int frameRate = 15; // 15fps
        int compressRatio = 256;
        int bitRate = dstWidth * dstHeight * 3 * 8 * frameRate / compressRatio;

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        Logger1.d(TAG, "prepare format: " + mediaFormat);
    }

    byte[] outData;

    private void start() {
        if (!isInitCodec)
            throw new RuntimeException("initCodec is false,please call initCodecManager() before");
        if (isStart) {
            Logger1.i(TAG, "startMediaCodec: was started");
            return;
        }
        try {
            Logger1.i(TAG, "startMediaCodec: starting");

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(mediaFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);

            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

                    byte[] data = null;
                    try {
                        data = frameBytes.take();
                        if (outData == null) {
                            outData = new byte[data.length];
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }

//                    Logger1.i(TAG, "onInputBufferAvailable: %s index=%d frameLen=%s", Thread.currentThread(), index, data == null ? "null" : "" + data.length);

                    ByteBuffer inputBuffer = codec.getInputBuffer(index);
                    if (inputBuffer == null)
                        return;
                    if (isAddOSD) {
//                        long start= SystemClock.uptimeMillis();
                        String date = mFormat.format(new Date());
                        YuvOsdUtils.addOsd(data, outData, date);
//                        long time=SystemClock.uptimeMillis()-start;
//                        Logger1.d(TAG,"time="+time+" ms");
                    } else {
                        YuvOsdUtils.NV21ToNV12(data, dstWidth, dstHeight);
                    }

                    inputBuffer.clear();
                    inputBuffer.put(outData);

                    long currentTimeUs = (System.nanoTime()-mTime) / 1000;//通过控制时间轴，达到暂停录制，继续录制的效果

                    codec.queueInputBuffer(index, 0, outData.length, currentTimeUs, 0);
//                    Logger1.i(TAG, "onInputBufferAvailable: currentTimeUs=%s ", currentTimeUs);

                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {

                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
//                    Logger1.i(TAG, "onOutputBufferAvailable: %s flag=%s timeus=%s", outputBuffer, info.flags, info.presentationTimeUs);

                    if (!isHasKeyFrame && info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                        isHasKeyFrame = true;
                    }
                    if (outputBuffer == null)
                        return;
                    else if (info.presentationTimeUs < lastPauseTime || !isHasKeyFrame) {//上一视频的数据，或者无关键帧，丢弃
                        //视频第一帧一定要是关键帧
                        Logger1.i(TAG, "onOutputBufferAvailable: discard %s time=%s", isHasKeyFrame, info.presentationTimeUs);
                    } else if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Logger1.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    } else {
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);

                        MuxerData muxerData=mPools.acquire();
                        if(muxerData==null){
                            Logger1.d(TAG,"muxerData==null");
                            muxerData=new MuxerData();
                        }
                        muxerData.trackIndex=MuxerManager.TRACK_VIDEO;
                        muxerData.bufferInfo=info;
                        muxerData.byteBuf=outputBuffer;
                        mMuxerManager.sendWriteDataMsg(muxerData);
                    }

                    codec.releaseOutputBuffer(index, false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
//                    Logger1.i(TAG, "添加视轨  " + newFormat.toString());
                    mMuxerManager.sendAddTrack(MuxerManager.TRACK_VIDEO, newFormat);
                }
            }, mHandler);
            mMediaCodec.start();
        } catch (Exception e) {
            return;
        }
        isStart = true;
    }

    /**
     * 返回到接收数据状态
     */
    public synchronized void flushMediaCodec() {
        Logger1.i(TAG, "flushMediaCodec");
        frameBytes.clear();
        isFlush = true;
        lastPauseTime = (System.nanoTime()) / 1000;//记录

        isHasKeyFrame = false;

    }

    private void stopMediaCodec() {
        if (isStart && mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        isStart = false;
        isPause=true;
        Logger1.i(TAG, "stopMediaCodec video");
    }

    private int getIndex(char c) {
        if (c >= '0' && c <= '9')
            return c - '0';
        else if (c == '-')
            return 10;
        else if (c == ' ')
            return 11;
        else if (c == ':')
            return 12;
        return 11;
    }


    public void releaseMuxerData(MuxerData data) {
        if(mPools!=null && data!=null){
            data.byteBuf=null;
            data.bufferInfo=null;
            mPools.release(data);
        }
    }
}
