package com.wzc.animation;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {
    private FrameLayout frameLayout;
    private SplashView splashView;
    private static Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        frameLayout = new FrameLayout(this);
        ContentView contentView = new ContentView(this);
        frameLayout.addView(contentView);
        splashView = new SplashView(this);
        frameLayout.addView(splashView);

        setContentView(frameLayout);
        //模拟数据加载（显示数据加载完成前的动画）
        startSplashDataLoad();
    }

    private void startSplashDataLoad() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                splashView.splashDisappear();
            }
        }, 5000);
    }
}
