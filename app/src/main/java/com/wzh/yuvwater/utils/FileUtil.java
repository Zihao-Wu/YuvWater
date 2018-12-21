package com.wzh.yuvwater.utils;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by JHY on 2018/3/15.
 * <p>
 * 文件处理工具类
 */
public class FileUtil {

    private static String TAG="FileUtil";

    /**
     * 删除文件或文件夹所有文件，包括文件夹
     * @return
     */
    public static boolean delFile(String filePath) {
        File fileDirectory = new File(filePath);
        if (fileDirectory.isDirectory()) {
            String[] files = fileDirectory.list();
            boolean isSuccess = true;
            if (files != null) {
                for (String file : files) {
                    boolean del = delFile(filePath + File.separator + file);
                    if (!del) {
                        isSuccess = false;
                    }
                }
            }
            isSuccess = fileDirectory.delete() && isSuccess;
            return isSuccess;
        } else {
            return fileDirectory.delete();
        }
    }

    /**
     * 删除文件夹下所有文件，不删除自己
     *
     * @param filePath
     * @return
     */
    public static boolean clearFileDirectory(String filePath) {
        File fileDirectory = new File(filePath);
        if (fileDirectory.isDirectory()) {
            String[] files = fileDirectory.list();
            boolean isSuccess = true;
            if (files != null) {
                for (String file : files) {
                    boolean del = delFile(filePath + File.separator + file);
                    if (!del) {
                        isSuccess = false;
                    }
                }
            }
            return isSuccess;
        } else {
            return fileDirectory.delete();
        }
    }

    public static String getFileSize(File file) {
        if (file == null)
            return "0B";
        long len = file.length();
        if (len > 1024) {
            long kb = len / 1024;
            if (kb > 1024) {
                long mb = kb / 1024;
                if (mb > 1024)
                    return mb / 1024 + "GB";
                return mb + "MB";
            }
            return kb + "KB";
        } else
            return len + "B";
    }

    public static String readTextFile(File textFile) {
        if (textFile == null || !textFile.exists())
            return null;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(textFile));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(reader);
        }
    }

    /**
     * 读文本类文件内容
     *
     * @param textFilePath
     * @return
     */
    public static String readTextFile(String textFilePath) {
        if (TextUtils.isEmpty(textFilePath))
            return null;
        File file = new File(textFilePath);
        return readTextFile(file);
    }

    /**
     * 写入内容到文件
     *
     * @param text
     * @param file
     * @return
     */
    public static boolean writeTextToFile(String text, File file) {
        if (file == null)
            return false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(text.getBytes(Charset.forName("utf-8")));
            fos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(fos);
        }

    }

    private static void close(Closeable fos) {
        if(fos!=null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean mkdirs(String path) {
        File file = new File(path);
        if (!file.exists())
            return file.mkdirs();
        return true;
    }
    public static String read() {
        return "aa";
    }
}