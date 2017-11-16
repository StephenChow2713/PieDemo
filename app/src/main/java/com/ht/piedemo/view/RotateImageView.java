package com.ht.piedemo.view;

/**
 * Created by 张天宝 on 2017/11/14.
 */

import android.graphics.Matrix;
import android.graphics.Rect;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;


import com.ht.piedemo.R;

/**
 * 可以旋转的ImageView
 * @author pie
 */
public class RotateImageView extends android.support.v7.widget.AppCompatImageView {

    private double mAngle;

    public RotateImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadAttributes(context, attrs);
    }

    public RotateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttributes(context, attrs);
    }

    private void loadAttributes(Context context, AttributeSet attrs) {
        TypedArray arr = context.obtainStyledAttributes(attrs,
                R.styleable.RotateImageView);
        mAngle = arr.getFloat(R.styleable.RotateImageView_angle, 0.0f);
        arr.recycle();
    }

    public double getAngle() {
        return mAngle;
    }

    public void setAngle(double angle) {
        mAngle = angle;
        postInvalidate();
    }

    public Bitmap rotate(RotateType type, int degrees) {
        Matrix matrix = new Matrix();
        degrees = degrees % 360;
        if (type.equals(RotateType.LEFT)) {
            matrix.setRotate(degrees * -1);
        } else {
            matrix.setRotate(degrees);
        }
        Bitmap original = ((BitmapDrawable) getDrawable()).getBitmap();
        Bitmap bitmap = Bitmap.createBitmap(original, 0, 0,
                original.getWidth(), original.getHeight(), matrix, true);
        setImageBitmap(bitmap);
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null)
            return;
        Rect bounds = drawable.getBounds();
        int w = bounds.right - bounds.left;
        int h = bounds.bottom - bounds.top;

        if (w == 0 || h == 0)
            return; // nothing to draw
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getPaddingRight();
        int bottom = getPaddingBottom();
        int width = getWidth() - left - right;
        int height = getHeight() - top - bottom;
        int saveCount = canvas.getSaveCount();

        // Scale down the image first if required.
        if ((getScaleType() == ImageView.ScaleType.FIT_CENTER)
                && ((width < w) || (height < h))) {
            float ratio = Math.min((float) width / w, (float) height / h);
            canvas.scale(ratio, ratio, width / 2.0f, height / 2.0f);
        }
        canvas.translate(left + width / 2, top + height / 2);
        canvas.rotate((float) mAngle);
        canvas.translate(-w / 2, -h / 2);
        drawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public enum RotateType {
        DEFAULT, LEFT, RIGHT;
    }

}
