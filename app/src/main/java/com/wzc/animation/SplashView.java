package com.wzc.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by Administrator on 2017/2/23.
 */

public class SplashView extends View {
    private int mSplashBgColor = Color.WHITE;//整体的背景颜色
    private float mRotationRadius = 120;//旋转动画中大圆圈的半径
    private float mCircleRadius = 18;//旋转动画中每个小圆圈的半径
    private long mRotationDuration = 2000;//大圆圈旋转一次动画的时间
    private int[] mCircleColors;//小圆圈的颜色
    private long mSplashDuration = 1000;//聚合、扩散动画的持续时间

    private float mHoleRadius = 0F;//扩散动画空心圆的空心部分的初始半径
    private float mCurrentRotationAngle = 0F;//旋转动画中大圆圈的当前旋转角度
    private float mCurrentRotationRadius = mRotationRadius;//扩散动画空心圆的半径

    private Paint mPaint = new Paint();//绘制圆的画笔
    private Paint mPaintBackground = new Paint();//绘制背景的画笔

    private float mCenterX;//屏幕中心点的x坐标
    private float mCenterY;//屏幕中心点的y坐标
    private float mDiagonalDist;//屏幕对角线长度的一半

    public SplashView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaint.setAntiAlias(true);//设置画笔抗锯齿，默认实心

        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setStyle(Paint.Style.STROKE);//用来绘制空心圆
        mPaintBackground.setColor(mSplashBgColor);

        mCircleColors = getContext().getResources().getIntArray(R.array.splash_circle_colors);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2f;
        mCenterY = h / 2f;

        mDiagonalDist = (float) Math.sqrt((w * w + h * h) / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mState == null) {
            mState = new RotationState();
        }
        mState.drawState(canvas);
        super.onDraw(canvas);
    }

    public void splashDisappear() {
        if (mState != null && mState instanceof RotationState) {
            RotationState rotationState = (RotationState) mState;
            rotationState.cancel();
            //让旋转动画停止，并执行聚合动画
            post(new Runnable() {
                @Override
                public void run() {
                    mState = new MergingState();
                }
            });
        }
    }

    private SplashState mState;//状态表示当前执行的是三个动画中的哪一个动画

    private abstract class SplashState {
        public abstract void drawState(Canvas canvas);
    }

    private ValueAnimator mAnimator;

    /**
     * 旋转动画
     * 在一个旋转的大圆圈上，分列小圆圈，小圆圈随着大圆圈旋转而旋转
     */
    private class RotationState extends SplashState {
        public RotationState() {
            //值域旋转一周，0-2π
            mAnimator = ValueAnimator.ofFloat(0F, (float) Math.PI * 2);
            mAnimator.setInterpolator(new LinearInterpolator());//线性差值器，使动画看起来更平滑
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    //角度变化
                    mCurrentRotationAngle = (float) animation.getAnimatedValue();
                    //请求绘制->onDraw()
                    postInvalidate();
                }
            });
            mAnimator.setDuration(mRotationDuration);
            mAnimator.setRepeatCount(ValueAnimator.INFINITE);//循环动画
            mAnimator.start();
        }

        public void cancel() {
            mAnimator.cancel();
//            mAnimator.end();
        }

        @Override
        public void drawState(Canvas canvas) {
            //绘制动画
            drawBackground(canvas);
            drawCircle(canvas);
        }
    }

    /**
     * 聚合动画
     * 小圆圈随着大圆圈半径的缩小而聚合
     */
    private class MergingState extends SplashState {
        public MergingState() {
            mAnimator = new ValueAnimator().ofFloat(0, mRotationRadius);
            mAnimator.setInterpolator(new OvershootInterpolator(5F));//抖动
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotationRadius = (float) animation.getAnimatedValue();
                    postInvalidate();
                }
            });
            //监听动画结束，执行扩散动画
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mState = new ExpandState();
                }
            });
            mAnimator.setDuration(mSplashDuration);
            mAnimator.reverse();//动画反转，让动画先抖动后缩小
        }

        @Override
        public void drawState(Canvas canvas) {
            drawBackground(canvas);
            drawCircle(canvas);
        }
    }

    /**
     * 扩散动画
     * 画一个渐渐扩大的显示加载内容的空心圆
     */
    private class ExpandState extends SplashState {
        public ExpandState() {
            //空心圆中空心的半径为变化值
            mAnimator = ValueAnimator.ofFloat(0, mDiagonalDist);
//            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mHoleRadius = (float) animation.getAnimatedValue();
                    postInvalidate();
                }
            });
            mAnimator.setDuration(mSplashDuration);
            mAnimator.start();
        }

        @Override
        public void drawState(Canvas canvas) {
            drawBackground(canvas);
        }
    }

    public void drawBackground(Canvas canvas) {
        //空心圆的空心部分的半径大于0时表示在执行扩散动画
        if (mHoleRadius > 0F) {
            //计算画笔的宽度
            float strokeWidth = mDiagonalDist - mHoleRadius;
            mPaintBackground.setStrokeWidth(strokeWidth);
            //整个空心圆的半径=空心部分的半径加上填色部分的半径
            float radius = mHoleRadius + strokeWidth / 2;
            canvas.drawCircle(mCenterX, mCenterY, radius, mPaintBackground);
        } else {
            //默认绘制背景
            canvas.drawColor(mSplashBgColor);
        }
    }

    /**
     * 绘制小圆圈
     *
     * @param canvas
     */
    public void drawCircle(Canvas canvas) {
        for (int i = 0; i < mCircleColors.length; i++) {
            //计算每个小圆圈当前的角度
            float rotationAngle = (float) (2 * Math.PI / mCircleColors.length);
            double angle = i * rotationAngle + mCurrentRotationAngle;
            //x=r*cos(a),y=r*sin(a)计算不同角度时小圆圈在大圆圈上的坐标
            //因为Android绘制时原点是左上角，所以加上屏幕一半
            float cx = (float) (mCurrentRotationRadius * Math.cos(angle) + mCenterX);
            float cy = (float) (mCurrentRotationRadius * Math.sin(angle) + mCenterY);
            mPaint.setColor(mCircleColors[i]);
            canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
        }
    }
}
