package com.wzh.yuvwater.monitor;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * 封装需要传输的数据类型
 */
public class MuxerData {
    int trackIndex;
    ByteBuffer byteBuf;
    MediaCodec.BufferInfo bufferInfo;

    public MuxerData() {
    }

    public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        this.trackIndex = trackIndex;
        this.byteBuf = byteBuf;
        this.bufferInfo = bufferInfo;
    }
}