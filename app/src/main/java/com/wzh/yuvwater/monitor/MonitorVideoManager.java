package com.wzh.yuvwater.monitor;

import com.wzh.yuvwater.utils.Utils;

public class MonitorVideoManager {
    private static MonitorVideoManager sInstance;

    private String TAG = "MonitorVideoManager";

    private MonitorVideoManager() {

    }

    public static MonitorVideoManager getInstance() {
        if (sInstance == null) {
            synchronized (MonitorVideoManager.class) {
                if (sInstance == null)
                    sInstance = new MonitorVideoManager();
            }
        }
        return sInstance;
    }

    private int mWidth, mHeight;

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public void onCameraData(byte[] nv21Data) {
        MuxerManager.getInstance().addVideoFrameData(nv21Data);
    }

    public void start() {
        String newVideoPath = Utils.getVideoFilePath();
        MuxerManager.getInstance().reStartMuxer(newVideoPath);
    }


    public void stop() {
        MuxerManager.getInstance().stopMuxer();
    }

}
