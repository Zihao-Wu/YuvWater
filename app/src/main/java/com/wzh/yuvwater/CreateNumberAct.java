package com.wzh.yuvwater;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.wzh.yuvwater.jni.YuvOsdUtils;
import com.wzh.yuvwater.utils.Logger1;
import com.wzh.yuvwater.utils.ThreadManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CreateNumberAct extends AppCompatActivity {

    private Paint p;
    private String TAG = "CreateNumberAct";

    public String[] chars = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-", ":", " "};

    public Map<String, byte[]> map = new HashMap<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_create_num);
        initView();
    }

    public void initView() {
        LinearLayout llRoot = (LinearLayout) findViewById(R.id.ll_root);
        if (p == null) {
            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setTextSize(30);
        }
        int w = 17, h = 25;

        findViewById(R.id.bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ThreadManager.getInstance().execute(() -> {

                    for (int i = 0; i < chars.length; i++) {
                        String content = chars[i];

                        Rect r = new Rect();
                        p.getTextBounds(content, 0, content.length(), r);

//                        p.setTypeface(tf);

                        Bitmap srcBit = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
                        Canvas canvas = new Canvas(srcBit);
                        canvas.drawColor(Color.BLACK);

                        if (TextUtils.equals("1", content)) {
                            canvas.drawText(content, 1, (r.height() + h) / 2, p);
                        } else if (TextUtils.equals("-", content)) {
                            canvas.drawText(content, (w - r.width()) / 2, h - r.height(), p);
                        } else {
                            canvas.drawText(content, (w - r.width()) / 2, (r.height() + h) / 2, p);
                        }

                        runOnUiThread(() -> {
                            ImageView iv = (ImageView) LayoutInflater.from(CreateNumberAct.this).inflate(R.layout.item_image_view, llRoot, false);
                            iv.setImageBitmap(srcBit);
                            llRoot.addView(iv);
                        });

                        byte[] nv12 = YuvOsdUtils.bitmapToGrayNV(srcBit, w, h);

                        Logger1.i(TAG, "onClick: length=%s w*h=%s w=%s h=%s", nv12.length, w * h, w, h);
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < h; j++) {
                            String line = "";
                            for (int k = 0; k < w; k++) {
                                //黑色为0，其它为1
                                if (nv12[j * w + k] == 16) {
                                    nv12[j * w + k] = 0;
                                } else {//其它为白色
                                    nv12[j * w + k] = 1;
                                }
                                line += nv12[j * w + k];
                            }
                            sb.append(line + "\n");
                        }
                        Logger1.i(TAG, "onClick: \n%s", sb);


                       /* List<Byte> bytes=new ArrayList<>(100);
                        byte blackNum=-1;
                        for (int j = 0; j < nv12.length; j++) {
                            if(nv12[j]==16){
                                if(blackNum==-1){
                                    blackNum=0;
                                    bytes.add(nv12[j]);
                                }
                                blackNum++;
                                if(blackNum==Byte.MAX_VALUE){
                                    bytes.add(blackNum);
                                    blackNum=-1;
                                }
                            }else{
                                if(blackNum!=-1){
                                    bytes.add(blackNum);
                                    blackNum=-1;
                                }
                                bytes.add(nv12[j]);
                            }
                        }
                        if(blackNum!=-1){
                            bytes.add(blackNum);
                        }
                        Logger1.i(TAG, "onClick: c=%s newLen=%d %s",content,bytes.size(), bytes);
*/
                        Logger1.i(TAG, "onClick: c=%s len=%d %s", content, nv12.length, Arrays.toString(nv12));

                        map.put(content, nv12);

//                        Logger1.i(TAG, "initBase: len=%s==%s char=%s %s", w * h * 3 / 2, nv12.length, content, Arrays.toString(nv12));

                    }

                    Bitmap srcBit = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
                    Canvas canvas = new Canvas(srcBit);
                    canvas.drawColor(Color.WHITE);

                    byte[] nv12 = YuvOsdUtils.bitmapToGrayNV(srcBit, w, h);

                    Logger1.i(TAG, "onClick: length=%s whites=%s", nv12.length, Arrays.toString(nv12));
                });

            }
        });

        findViewById(R.id.bt_save).setOnClickListener((View v) -> {

        });

    }
}
