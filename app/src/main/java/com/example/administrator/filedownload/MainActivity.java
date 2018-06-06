package com.example.administrator.filedownload;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private TextView address;
    private EditText threadnum;
    private TextView downloadinfo;
    private Button bt_start;
    private Button bt_stop;
    private Button bt_cancel;
    private Button bt_restart;
    private Button bt_continue;
    private Context mContext;
    private ProgressBar progressBar;
    private long downloadtime;
    private int downloadedSize = 0;
    private int fileSize = 0;
    private int num ;
    DownloadFile downLoadFile;
    private SharedPreferences sp;
    private static final String SP_NAME = "download_file";
    private String loadUrl = "http://p1.pstatp.com/large/166200019850062839d3";
    private String filePath = Environment.getExternalStorageDirectory()+"/"+"一张动图";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        final TextView tvprogress = (TextView) findViewById(R.id.downloadinfo);
        downLoadFile = new DownloadFile(this, loadUrl, filePath, num);
        downLoadFile.setOnDownLoadListener(new DownloadFile.DownLoadListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void getProgress(int progress) {
                tvprogress.setText("当前进度 ：" + progress + " %");
                progressBar.getProgress();
            }

            @Override
            public void onComplete() {
                Toast.makeText(MainActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure() {
                Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * 初始化控件
     */
    @SuppressLint("SetTextI18n")
    private void init(){
        address = findViewById(R.id.address);
        address.setText("http://p1.pstatp.com/large/166200019850062839d3");
        downloadinfo = findViewById(R.id.downloadinfo);
        threadnum = findViewById(R.id.threadnum);
        bt_start = findViewById(R.id.bt1);
        bt_stop = findViewById(R.id.bt2);
        bt_cancel = findViewById(R.id.bt3);
        bt_restart = findViewById(R.id.bt4);
        bt_continue = findViewById(R.id.bt5);
        progressBar = findViewById(R.id.jindu);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                num = Integer.parseInt(threadnum.getText().toString().trim());
                downLoadFile.downLoad(num);
                downloadtime = SystemClock.currentThreadTimeMillis();
            }
        });


        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downLoadFile.onPause();
            }
        });


        bt_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downLoadFile.onStart();
            }
        });


        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downLoadFile.cancel();
                downloadinfo.setText("当前进度 ：0 %");
            }
        });

        bt_restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                num = Integer.parseInt(threadnum.getText().toString().trim());
                downLoadFile.downLoad(num);
            }
        });

    }

}
