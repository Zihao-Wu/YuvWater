package com.wzh.yuvwater.jni;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.wzh.yuvwater.utils.Utils;

import java.io.ByteArrayOutputStream;

public class YuvOsdUtils {

    static {
        System.loadLibrary("YuvOsdUtils");
    }

    /**
     * 初始化时间水印
     *
     * @param osdOffX     水印在视频左上的x偏移
     * @param osdOffY     水印在视频左上的y 偏移
     * @param patternLen  水印格式长度
     * @param frameWidth  相机宽
     * @param frameHeight 相机高
     * @param rotation    旋转角度,0,90,180,270
     */
    public static native void initOsd(int osdOffX, int osdOffY
            , int patternLen, int frameWidth, int frameHeight, int rotation);

    /**
     * 释放内存
     */
    public static native void releaseOsd();

    public static native void addOsd(byte[] yuvInData, byte[] outYvuData, String date);


    /**
     * nv12 与nv21区别
     * NV12: YYYYYYYY UVUV     =>YUV420SP
     * NV21: YYYYYYYY VUVU     =>YUV420SP
     * rgb 转 nv21
     *
     * @param argb
     * @param width
     * @param height
     * @return
     */
    public static native byte[] argbIntToNV21Byte(int[] argb, int width, int height);

    /**
     * rgb 转nv12
     *
     * @param argb
     * @param width
     * @param height
     * @return
     */
    public static native byte[] argbIntToNV12Byte(int[] argb, int width, int height);

    /**
     * rgb 转灰度 nv
     * 也就是yuv 中只有 yyyy 没有uv 数据
     *
     * @param argb
     * @param width
     * @param height
     * @return
     */
    public static native byte[] argbIntToGrayNVByte(int[] argb, int width, int height);

    /**
     * nv21 转 nv 12
     *
     * @param nv21Src  源数据
     * @param nv12Dest 目标数组
     * @param width    数组长度 len=width*height*3/2
     * @param height
     */
    public static native void nv21ToNv12(byte[] nv21Src, byte[] nv12Dest, int width, int height);

    /**
     * @param bitmap cannot be used after call this function
     * @param width  the width of bitmap
     * @param height the height of bitmap
     * @return return the NV21 byte array, length = width * height * 3 / 2
     */
    public static byte[] bitmapToNV21(Bitmap bitmap, int width, int height) {
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] nv21 = argbIntToNV21Byte(argb, width, height);
        return nv21;
    }

    /**
     * @param bitmap cannot be used after call this function
     * @param width  the width of bitmap
     * @param height the height of bitmap
     * @return return the NV12 byte array, length = width * height * 3 / 2
     */
    public static byte[] bitmapToNV12(Bitmap bitmap, int width, int height) {
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] nv12 = argbIntToNV12Byte(argb, width, height);
        return nv12;
    }

    /**
     * @param bitmap cannot be used after call this function
     * @param width  the width of bitmap
     * @param height the height of bitmap
     * @return return the NV12 byte array, length = width * height
     */
    public static byte[] bitmapToGrayNV(Bitmap bitmap, int width, int height) {
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] nv12 = argbIntToGrayNVByte(argb, width, height);
        return nv12;
    }


    /**
     * java 版，速度比c 慢
     *
     * @param nv21
     * @param width
     * @param height
     */
    public static void NV21ToNV12(byte[] nv21, int width, int height) {
        if (nv21 == null) return;
        int framesize = width * height;
        int j = 0;
        int end = framesize + framesize / 2;
        byte temp = 0;
        for (j = framesize; j < end; j += 2)//u v
        {
            temp = nv21[j];
            nv21[j] = nv21[j + 1];
            nv21[j + 1] = temp;
        }
    }

    public static byte[] cameraFrameToArray(byte[] yuv, int w, int h) {
        return cameraFrameToArray(yuv, w, h, 100);
    }

    public static byte[] cameraFrameToArray(byte[] yuv, int w, int h, int quality) {
        YuvImage img = new YuvImage(yuv, ImageFormat.NV21,
                w, h, null);
        Rect rect = new Rect(0, 0, w, h);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        img.compressToJpeg(rect, quality, os);

        byte[] tmp = os.toByteArray();//裁剪后的人脸图
        Utils.close(os);
        return tmp;
    }

}
