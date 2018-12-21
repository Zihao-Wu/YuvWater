package com.wzh.yuvwater.monitor;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.wzh.yuvwater.utils.Logger1;

import java.io.IOException;

/**
 * 混合音视频线程
 */
class MuxerManager {

    public static final int TRACK_VIDEO = 1;
    public static boolean DEBUG = true;
    private final HandlerThread mHandlerThread;
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private volatile boolean isVideoTrackAdd;
    //混合音视频的线程开启状态

    private String TAG = "MediaMuxerRunnable";
    private String filePath;
    private static MuxerManager sInstance;

    private MuxerHandler mHandler;
    private static final int MSG_STOP_MUXER = 1;
    private static final int MSG_SET_NEW_FILE_PATH = 2;
    private static final int MSG_SET_FORMAT = 3;
    private static final int MSG_ADD_MUXER_DATA = 5;
    private static final int MSG_PAUSE_MUXER = 6;
    private static final int MSG_RELEASE_MANAGER = 4;


    class MuxerHandler extends android.os.Handler {

        public MuxerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MuxerManager.this.handleMessage(msg);
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SET_NEW_FILE_PATH:
                this.filePath = (String) msg.obj;
                readyStart();
                break;
            case MSG_SET_FORMAT:
                addTrackIndex(msg.arg1, (MediaFormat) msg.obj);
                break;
            case MSG_ADD_MUXER_DATA:
                try {
                    MuxerData data = (MuxerData) msg.obj;
//                    Logger1.d(TAG, "写入混合数据 " + data.bufferInfo.size);
                    mediaMuxer.writeSampleData(videoTrackIndex, data.byteBuf, data.bufferInfo);
                    MediaCodecManager.getInstance().releaseMuxerData(data);
                } catch (Exception e) {
                    Logger1.e(TAG, "写入混合数据失败!" + e.toString());
                }
                break;
            case MSG_STOP_MUXER:
                releaseMuxer();
                break;
            case MSG_RELEASE_MANAGER:
                releaseManager();
                break;
            case MSG_PAUSE_MUXER:
                break;
            default:
                break;
        }
    }

    private MuxerManager() {
        mHandlerThread = new HandlerThread("muxerThread");
        mHandlerThread.start();

        mHandler = new MuxerHandler(mHandlerThread.getLooper());
    }

    public static MuxerManager getInstance() {
        if (sInstance == null) {
            synchronized (MuxerManager.class) {
                if (sInstance == null)
                    sInstance = new MuxerManager();
            }
        }
        return sInstance;
    }

    /**
     * 重新开始新视频录制
     *
     * @param filePath
     */
    public void reStartMuxer(String filePath) {
        mHandler.obtainMessage(MSG_SET_NEW_FILE_PATH, filePath).sendToTarget();
    }

    /**
     * 暂停视频录制
     */
    public void pauseMuxer() {
        MediaCodecManager.getInstance().pauseMediaCodec();
    }

    public void resumeMuxer() {
        MediaCodecManager.getInstance().resumeMediaCodec();
    }
    /**
     * 结束当前视频录制
     */
    public void stopMuxer() {
        mHandler.obtainMessage(MSG_STOP_MUXER).sendToTarget();
    }

    /**
     * 发送添加track事件
     *
     * @param index
     * @param mediaFormat
     */
    public void sendAddTrack(int index, MediaFormat mediaFormat) {
        mHandler.obtainMessage(MSG_SET_FORMAT, index, 0, mediaFormat).sendToTarget();
    }

    /**
     * 添加混合数据
     *
     * @param data
     */
    public void sendWriteDataMsg(MuxerData data) {
        if (!isMuxerStart() || data.trackIndex != TRACK_VIDEO) {
            return;
        }
        mHandler.obtainMessage(MSG_ADD_MUXER_DATA, data).sendToTarget();
    }

    public void addVideoFrameData(byte[] data) {
        if(mediaMuxer!=null)
            MediaCodecManager.getInstance().addFrameData(data);
    }

    private boolean isVideoTrackAdd() {
        return isVideoTrackAdd;
    }

    /**
     * 设置更换文件
     * create at 2017/3/22 18:06
     */
    private void readyStart() {
        if (TextUtils.isEmpty(filePath))
            return;
        try {
            readyStart(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readyStart(String filePath) throws IOException {
        Logger1.d(TAG, "readyStart start path=%s",filePath);

        isVideoTrackAdd = false;

        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaCodecManager.getInstance().initCodecManager(CameraWrapper.SRC_VIDEO_WIDTH,
                CameraWrapper.SRC_VIDEO_HEIGHT,CameraWrapper.getInstance().isPort()?270:0);
        MediaCodecManager.getInstance().startMediaCodec();
        Logger1.d(TAG, "readyStart end ");
    }


    private void addTrackIndex(int index, MediaFormat mediaFormat) {
        /*轨迹改变之后,重启混合器*/
        if (index == TRACK_VIDEO && isVideoTrackAdd()) {
            return;
        }
        if (mediaMuxer != null && index == TRACK_VIDEO) {
            try {
                videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();

                isVideoTrackAdd = true;
                Logger1.d(TAG, "添加视轨 完成");
            } catch (Exception e) {
                Logger1.e(TAG, "addTrack 异常:" + e.toString());
            }
        }
    }

    private boolean isMuxerStart() {
        return isVideoTrackAdd;
    }

    /**
     * 关闭mediaMuxer
     * create at 2017/3/22 17:59
     */
    private void releaseMuxer() {
        Logger1.d(TAG, "releaseMuxer start " + mediaMuxer);
        if (mediaMuxer != null) {
            MediaCodecManager.getInstance().releaseManager();
            try {
                mediaMuxer.release();
            } catch (Exception e) {
                Logger1.e(TAG, "mediaMuxer.release() 异常:" + e.toString());
            }
            mediaMuxer = null;
            mHandler.removeCallbacksAndMessages(null);
        }
//        Logger1.d(TAG, "releaseMuxer end " + mediaMuxer);

    }

    public void releaseManager() {
        Logger1.i(TAG, "releaseManager: start");
        releaseMuxer();
        MediaCodecManager.getInstance().releaseManager();

        sInstance = null;
        mHandlerThread.quit();
        mHandler=null;
        Logger1.i(TAG, "releaseManager: end");

    }


}
