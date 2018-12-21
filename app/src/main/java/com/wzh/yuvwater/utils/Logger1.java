package com.wzh.yuvwater.utils;

import android.util.Log;

import java.util.Locale;

/**
 * Created by wzh on 17/3/15.
 * <p>
 * Logger.d(TAG_CANCEL, a+"xxx"+b);
 * <p>
 * Logger1.d(TAG_CANCEL, "%s xxx %s=", "a","b");
 * 当在release 环境中，不会进行计算，效率比Logger更高
 */
public class Logger1 {
    private static String TAG_START_WITH;
    private static boolean LOG_STACK_TRACE_ENABLE = false;//是否打印调用信息
    public static boolean LOG_CAT_ENABLE = true;//log 到logcat

    public static final String LOG_V = "V", LOG_D = "D", LOG_I = "I", LOG_W = "W", LOG_E = "E";

    static {
        TAG_START_WITH = "yuvwater_";
    }

    public static void setTagStartWith(String tagStartWith) {
        TAG_START_WITH = tagStartWith;
    }
    public static void v(String format, Object... args) {
        v("", format, args);
    }

    public static void v(String tag, String format, Object... args) {
        if (LOG_CAT_ENABLE) {
            String msg = buildMsg(LOG_STACK_TRACE_ENABLE, format, args);
            Log.v(TAG_START_WITH + tag, msg);
        }
        logToFile(tag, format, args, LOG_V);
    }
    public static void d(String format, Object... args) {
        d("", format, args);
    }

    public static void d(String tag, String format, Object... args) {
        if (LOG_CAT_ENABLE) {
            String msg = buildMsg(LOG_STACK_TRACE_ENABLE, format, args);
            Log.d(TAG_START_WITH + tag, msg);
        }
        logToFile(tag, format, args, LOG_D);
    }

    private static void logToFile(String tag, String format, Object[] args, String logLevel) {

    }

    public static void i(String format, Object... args) {
        i("", format, args);
    }

    public static void i(String tag, String format, Object... args) {
        i(LOG_STACK_TRACE_ENABLE, tag, format, args);
    }

    public static void i(boolean stackLog, String tag, String format, Object... args) {
        if (LOG_CAT_ENABLE) {
            String msg = buildMsg(stackLog, format, args);
            Log.i(TAG_START_WITH + tag, msg);
        }
        logToFile(tag, format, args, LOG_I);
    }

    public static void w(String format, Object... args) {
        w("", format, args);
    }

    public static void w(String tag, String format, Object... args) {
        if (LOG_CAT_ENABLE) {
            String msg = buildMsg(LOG_STACK_TRACE_ENABLE, format, args);
            Log.w(TAG_START_WITH + tag, msg);
        }
        logToFile(tag, format, args, LOG_W);
    }

    public static void e(String format, Object... args) {
        e("", format, args);
    }

    public static void e(String tag, String format, Object... args) {
        //error 默认输出错误调用信息
        e(true, tag, format, args);
    }

    public static void e(boolean stackLog, String tag, String format, Object... args) {
        if (LOG_CAT_ENABLE) {
            String msg = buildMsg(stackLog, format, args);
            Log.e(TAG_START_WITH + tag, msg);
        }
        logToFile(tag, format, args, LOG_E);
    }

    private static String buildMsg(boolean stackLog, String format, Object[] args) {
        String msg = args == null ? format : String.format(format, args);
        if (stackLog) {
            msg += "\n" + getAutoJumpLogInfos();
        }
        return msg;
    }

    private static String getAutoJumpLogInfos() {
        StackTraceElement[] caller = Thread.currentThread().getStackTrace();
        String info = "";
        if (caller != null) {
            for (StackTraceElement element : caller) {
                info += generateTag(element) + "\n";
            }
        }
        return info;
    }

    private static String generateTag(StackTraceElement caller) {
        String tag = "%s.%s(Line:%d)"; // 占位符
        String callerClazzName = caller.getClassName(); // 获取到类名
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(Locale.getDefault(), tag, callerClazzName, caller.getMethodName(), caller.getLineNumber()); // 替换
        return tag;
    }


}
