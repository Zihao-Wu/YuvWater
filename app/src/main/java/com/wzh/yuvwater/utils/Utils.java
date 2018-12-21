package com.wzh.yuvwater.utils;

import android.content.Context;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Utils  extends FileUtil{

    public static final String VIDEO_FILE_START_WITH = "part";
    private static String TAG = "MonitorUtils";
    private static final int MIN_SPACE = 500;//最小剩余可用空间500M

    /**
     * 同一天中， 多次重启打开应用，生成分段，最好的情况是每天只有一个分段
     */
    public static String getVideoFilePath() {
        String ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        String rootFolder = "/sdcard/yuvVideo" + File.separator + ymd;
        FileUtil.mkdirs(rootFolder);
        File folder = new File(rootFolder);

        String newVideoPath = null;
        if (folder.exists()) {
            String[] files = folder.list((File dir, String name) -> name.endsWith(".mp4")
                    && name.startsWith(VIDEO_FILE_START_WITH)
                    && new File(dir, name).length() > 100 * 1024 //100 k
            );
            Logger1.i(TAG, "Folder: %s files=%s", rootFolder, Arrays.toString(files));
            if (files != null && files.length > 0) {
                newVideoPath = rootFolder + "/" + VIDEO_FILE_START_WITH + (files.length + 1) + ".mp4";

                StatFs stat = new StatFs(rootFolder);

                long availableBlocks = stat.getBlockSizeLong() * stat.getAvailableBlocksLong() / 1024 / 1024;//剩余x M;
                if (availableBlocks < MIN_SPACE) {//删除上一个视频
                    File delFile = new File(rootFolder + "/" + VIDEO_FILE_START_WITH + files.length + ".mp4");
                    if (delFile.exists())
                        FileUtil.delFile(delFile.getAbsolutePath());
                }
                Logger1.i(TAG, "getVideoFilePath: available size=%s MB", availableBlocks);
            }
        }
        if (TextUtils.isEmpty(newVideoPath)) {
            newVideoPath = rootFolder + "/" + VIDEO_FILE_START_WITH + "1.mp4";
        }
        return newVideoPath;
    }
    public static String read() {
        return "bb";
    }

        public static int getScreenW(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenH(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static void close(Closeable is) {
        if (is != null)
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
