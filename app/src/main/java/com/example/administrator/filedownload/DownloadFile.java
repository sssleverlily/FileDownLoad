package com.example.administrator.filedownload;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFile {
    private static final String SP_NAME = "download_file";
    private static final String CURR_LENGTH = "curr_length";
    private static final String DOWNLOAD_INIT = "1";
    private static final String DOWNLOAD_ING = "2";
    private static final String DOWNLOAD_PAUSE = "3";
    private final int SUCCESS = 0x00000101;
    private final int FAILURE = 0x00000102;
    EditText threadnum ;

    private Context mContext;
    private int threadCount;
    private String loadUrl;
    private String filePath;

    private int fileLength;
    private int currLength;
    private int runningThreadCount;
    private Thread[] mThreads;
    private String stateDownload = DOWNLOAD_INIT;
    private SharedPreferences sp;
    private DownLoadListener mDownLoadListener;

    public void setOnDownLoadListener(DownLoadListener mDownLoadListener) {
        this.mDownLoadListener = mDownLoadListener;
    }

    interface DownLoadListener {
        void getProgress(int progress);

        void onComplete();

        void onFailure();
    }


    public DownloadFile(Context mContext, String loadUrl, String filePath, int threadCount) {
        this(mContext, loadUrl, filePath, threadCount, null);
    }

    public DownloadFile(Context mContext, String loadUrl, String filePath, int threadCount, DownLoadListener mDownLoadListener) {
        this.mContext = mContext;
        this.loadUrl = loadUrl;
        this.filePath = filePath;
        this.threadCount = threadCount;
        runningThreadCount = 0;
        this.mDownLoadListener = mDownLoadListener;
    }


    /**
     * 开始
     */
    public void downLoad(final int threadCount) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mThreads == null)
                        mThreads = new Thread[threadCount];
                    URL url = new URL(loadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        fileLength = conn.getContentLength();
                        RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
                        raf.setLength(fileLength);
                        raf.close();
                        //计算各个线程下载的数据段
                        int blockLength = fileLength / threadCount;
                        if(String.valueOf(threadCount)!= null) {
                            sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor edit = sp.edit();
                            edit.putString("num", String.valueOf(threadCount));
                            System.out.println(String.valueOf(threadCount));
                            edit.apply();
                        }else {
                            String numstring  = sp.getString("num","null");
                            final int threadnum = Integer.getInteger(numstring);
                            restart(threadnum);
                        }
                        currLength = sp.getInt(CURR_LENGTH, 0);
                        for (int i = 0; i < threadCount; i++) {
                            //开始，获取上次取消下载的进度，默认返回第i个线程开始下载的位置（i*blockLength）
                            int startPosition = sp.getInt(SP_NAME + (i + 1), i * blockLength);
                            //结束，-1是为了防止上一个线程和下一个线程重复下载衔接处数据
                            int endPosition = (i + 1) * blockLength - 1;
                            if ((i + 1) == threadCount)
                                endPosition = endPosition * 2;

                            mThreads[i] = new DownThread(i + 1, startPosition, endPosition);
                            mThreads[i].start();
                        }
                    } else {
                        handler.sendEmptyMessage(FAILURE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(FAILURE);
                }
            }
        }).start();
    }

    /**
     * 取消
     */
    public void cancel() {
        if (mThreads != null) {
            if (stateDownload.equals(DOWNLOAD_PAUSE))
                onStart();
            for (Thread dt : mThreads) {
                ((DownThread) dt).cancel();
            }
        }
    }

    /**
     * 暂停
     */
    public void onPause() {
        if (mThreads != null)
            stateDownload = DOWNLOAD_PAUSE;
    }

    /**
     * 继续
     */
    public void onStart() {
        if (mThreads != null)
            synchronized (DOWNLOAD_PAUSE) {
                stateDownload = DOWNLOAD_ING;
                DOWNLOAD_PAUSE.notifyAll();
            }
    }

    public void onDestroy() {
        if (mThreads != null)
            mThreads = null;
    }

    /**
      恢复
     */
    public void restart(final int threadnum) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mThreads == null)
                        mThreads = new Thread[threadnum];

                    URL url = new URL(loadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        fileLength = conn.getContentLength();
                        RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
                        raf.setLength(fileLength);
                        raf.close();
                        int blockLength = fileLength / threadnum;
                        sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                        currLength = sp.getInt(CURR_LENGTH, 0);
                        for (int i = 0; i < threadnum; i++) {
                            int startPosition = sp.getInt(SP_NAME + (i + 1), i * blockLength);
                            int endPosition = (i + 1) * blockLength - 1;
                            if ((i + 1) == threadnum)
                                endPosition = endPosition * 2;

                            mThreads[i] = new DownThread(i + 1, startPosition, endPosition);
                            mThreads[i].start();
                        }
                    } else {
                        handler.sendEmptyMessage(FAILURE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(FAILURE);
                }
            }
        }).start();
    }

    private class DownThread extends Thread {

        private boolean isGoOn = true;
        private int threadId;
        private int startPosition;
        private int endPosition;
        private int currPosition;

        private DownThread(int threadId, int startPosition, int endPosition) {
            this.threadId = threadId;
            this.startPosition = startPosition;
            currPosition = startPosition;
            this.endPosition = endPosition;
            runningThreadCount++;
        }

        @Override
        public void run() {
            SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            try {
                URL url = new URL(loadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startPosition + "-" + endPosition);
                conn.setConnectTimeout(5000);
                if (conn.getResponseCode() == 206) {
                    InputStream is = conn.getInputStream();
                    RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
                    raf.seek(startPosition);
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = is.read(buffer)) != -1) {
                        if (!isGoOn)
                            break;
                        if (mDownLoadListener != null) {
                            currLength += len;
                            int progress = (int) ((float) currLength / (float) fileLength * 100);
                            handler.sendEmptyMessage(progress);
                        }

                        raf.write(buffer, 0, len);
                        currPosition += len;
                        synchronized (DOWNLOAD_PAUSE) {
                            if (stateDownload.equals(DOWNLOAD_PAUSE)) {
                                DOWNLOAD_PAUSE.wait();
                            }
                        }
                    }
                    is.close();
                    raf.close();
                    runningThreadCount--;
                    if (!isGoOn) {
                        if (currPosition < endPosition) {
                            sp.edit().putInt(SP_NAME + threadId, currPosition).apply();
                            sp.edit().putInt(CURR_LENGTH, currLength).apply();
                        }
                        return;
                    }
                    if (runningThreadCount == 0) {
                        sp.edit().clear().apply();
                        handler.sendEmptyMessage(SUCCESS);
                        handler.sendEmptyMessage(100);
                        mThreads = null;
                    }
                } else {
                    sp.edit().clear().apply();
                    handler.sendEmptyMessage(FAILURE);
                }
            } catch (Exception e) {
                sp.edit().clear().apply();
                e.printStackTrace();
                handler.sendEmptyMessage(FAILURE);
            }
        }

        public void cancel() {
            isGoOn = false;
        }
    }



    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (mDownLoadListener != null) {
                if (msg.what == SUCCESS) {
                    mDownLoadListener.onComplete();
                } else if (msg.what == FAILURE) {

                    mDownLoadListener.onFailure();
                } else {
                    mDownLoadListener.getProgress(msg.what);
                }
            }
        }
    };
}
