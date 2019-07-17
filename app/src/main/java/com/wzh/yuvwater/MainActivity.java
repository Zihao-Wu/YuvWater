package com.wzh.yuvwater;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.wzh.yuvwater.monitor.CameraWrapper;
import com.wzh.yuvwater.utils.Logger1;
import com.wzh.yuvwater.utils.Utils;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView surfaceView;
    private SwitchCompat compat;
    private View rlRoot;
    private String TAG = "MainActivity";
    private Button mBtStart;

    private int mRecordTime;
    private Handler mHandler = new Handler();
    private Button mBtPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        surfaceView = findViewById(R.id.surface);
        rlRoot = findViewById(R.id.rl_root);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera(compat.isChecked());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mBtStart = findViewById(R.id.bt_start);
        mBtPause=findViewById(R.id.bt_pause);
        mBtStart.setOnClickListener(this);
        findViewById(R.id.bt_stop).setOnClickListener(this);
        mBtPause.setOnClickListener(this);
        compat = findViewById(R.id.switch_compat);

        compat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                initCamera(isChecked);
            }
        });


    }

    private void initCamera(boolean isChecked) {
        CameraWrapper.getInstance().setScreenPort(isChecked);
        changeSurfaceView(isChecked);
        CameraWrapper.getInstance()
                .setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .doOpenCamera(surfaceView);
        CameraWrapper.getInstance().pausePreview();
        CameraWrapper.getInstance().startPreview();
    }

    private void changeSurfaceView(boolean isPort) {
        int screenW = rlRoot.getMeasuredWidth();
        int screenH = rlRoot.getMeasuredHeight();
        Logger1.d("TAG", "changeSurfaceView" + screenW + "*" + screenH);
        if (isPort) {
            float scaleW = (float) screenW / CameraWrapper.SRC_VIDEO_HEIGHT;
            float scaleH = (float) screenH / CameraWrapper.SRC_VIDEO_WIDTH;
            float scale = Math.min(scaleH, scaleW);
            surfaceView.getLayoutParams().width = (int) (CameraWrapper.SRC_VIDEO_HEIGHT * scale);
            surfaceView.getLayoutParams().height = (int) (CameraWrapper.SRC_VIDEO_WIDTH * scale);
            surfaceView.requestLayout();
        } else {
            float scaleW = (float) screenW / CameraWrapper.SRC_VIDEO_WIDTH;
            float scaleH = (float) screenH / CameraWrapper.SRC_VIDEO_HEIGHT;
            float scale = Math.min(scaleH, scaleW);
            surfaceView.getLayoutParams().width = (int) (CameraWrapper.SRC_VIDEO_WIDTH * scale);
            surfaceView.getLayoutParams().height = (int) (CameraWrapper.SRC_VIDEO_HEIGHT * scale);
            surfaceView.requestLayout();
        }
    }

    boolean misRecord;
    boolean isPause;

    String newVideoPath;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start:
                if (misRecord) {
                    return;
                }
                misRecord = true;

                newVideoPath= Utils.getVideoFilePath();
                CameraWrapper.getInstance().startRecording(newVideoPath);
                mBtStart.setEnabled(false);
                mRecordTime = 0;
                mBtStart.setText("00:00");
                isPause=false;
                mBtPause.setText("暂停录制");

                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!isPause){
                            mRecordTime++;
                            mBtStart.setText(getStrRecordTime(mRecordTime));
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                }, 1000);

                break;
            case R.id.bt_pause:
                if(!isPause){
                    CameraWrapper.getInstance().pauseRecording();
                    isPause=true;
                    mBtPause.setText("继续录制");
                }else{
                    isPause=false;
                    CameraWrapper.getInstance().resumeRecording();
                    mBtPause.setText("暂停录制");
                }
                break;
            case R.id.bt_stop:
                Toast.makeText(this,"视频文件保存至："+newVideoPath,Toast.LENGTH_SHORT).show();
                CameraWrapper.getInstance().stopRecording();
                mHandler.removeCallbacksAndMessages(null);
                misRecord=false;
                mBtStart.setEnabled(true);
                mBtStart.setText("开始录制");
                break;

            default:

                break;
        }
    }

    private String getStrRecordTime(int mRecordTime) {
        int minute = mRecordTime / 60;
        int second = mRecordTime % 60;
        return String.format(Locale.CHINA, "%02d:%02d", minute, second);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("生成数字YUV数据").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(MainActivity.this, CreateNumberAct.class));
                return true;
            }
        });
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }
}
