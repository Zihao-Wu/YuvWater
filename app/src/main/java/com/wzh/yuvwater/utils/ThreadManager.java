package com.wzh.yuvwater.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wzh on 2017/12/28.
 * 线程池管理类，不要直接new Thread()..
 */
public class ThreadManager {

    private static ThreadManager sInstance;
    private ExecutorService mExecutor;

    private ThreadManager() {
        int numberCore = Runtime.getRuntime().availableProcessors();

        int keepAliveTime = 20;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        mExecutor = new ThreadPoolExecutor(numberCore*2, numberCore * 3,
                keepAliveTime, TimeUnit.SECONDS, queue);
    }

    public static ThreadManager getInstance() {
        if (sInstance == null) {
            synchronized (ThreadManager.class) {
                if (sInstance == null) {
                    sInstance = new ThreadManager();
                }
            }
        }

        return sInstance;
    }


    public void execute(Runnable runnable) {
        mExecutor.execute(runnable);
    }

    public ExecutorService getExecutor() {
        return mExecutor;
    }

    public void release() {
        if(mExecutor!=null)
            mExecutor.shutdown();
        mExecutor=null;
        sInstance=null;
    }
}
