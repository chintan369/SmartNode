package com.nivida.smartnode.views;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.nivida.smartnode.R;

/**
 * Created by Nivida new on 02-Dec-16.
 */

public class RoundView extends View {
    int strokeWidth = 2;
    private int radius = 150;
    private final int ANIMATION_DURATION = 300;
    private final float SCALE_FACTOR = 0.3f;
    private Paint mPaint,bgPaint;
    Context context;

    private int strokeColor=Color.WHITE;
    private int fillColor=Color.BLACK;

    public RoundView(Context context) {
        super(context);
        this.context=context;
        init();
    }
    public RoundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.RoundView);

        initAttributes(attrArray);

        attrArray.recycle();
        init();
    }
    public RoundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context=context;
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.RoundView, defStyle, 0);

        initAttributes(attrArray);

        attrArray.recycle();

        init();
    }

    private void initAttributes(TypedArray attrArray){
        strokeColor=attrArray.getColor(R.styleable.RoundView_strokeColor,Color.WHITE);
        fillColor=attrArray.getColor(R.styleable.RoundView_fillColor,Color.CYAN);
        strokeWidth=attrArray.getInteger(R.styleable.RoundView_strokeWidth,3);
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(strokeColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);

        bgPaint=new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(fillColor);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setStrokeWidth(strokeWidth);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2,
                radius, mPaint);
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2,
                radius, bgPaint);
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public ObjectAnimator getScalingAnimator() {
        PropertyValuesHolder imgViewScaleY = PropertyValuesHolder.ofFloat(View
                .SCALE_Y, SCALE_FACTOR);
        PropertyValuesHolder imgViewScaleX = PropertyValuesHolder.ofFloat(View
                .SCALE_X, SCALE_FACTOR);
        ObjectAnimator imgViewScaleAnimator = ObjectAnimator
                .ofPropertyValuesHolder(this, imgViewScaleX, imgViewScaleY);
        imgViewScaleAnimator.setRepeatCount(1);
        imgViewScaleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        imgViewScaleAnimator.setDuration(ANIMATION_DURATION);
        return imgViewScaleAnimator;
    }

    public void setStrokeColor(int color){
        this.strokeColor=color;
        mPaint.setColor(strokeColor);
        this.invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        bgPaint.setColor(color);
        this.invalidate();
    }

    public void setStrokeWidth(int width){
        strokeWidth=width;
        mPaint.setStrokeWidth(width);
        this.invalidate();
    }
}