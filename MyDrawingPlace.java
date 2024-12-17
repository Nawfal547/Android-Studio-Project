package com.example.photoapp_final;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyDrawingPlace extends View {
    Path path = new Path();

    public MyDrawingPlace(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override //Sets all the stuff
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);
        canvas.drawPath(path, p);
        invalidate();
    }

    @Override // On touch things are drawn
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            path.moveTo(x, y);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            path.lineTo(x, y);
        }
        return true;
    }

    public Bitmap getBitmap() {
        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(5f);
        c.drawPath(path, p);
        return bmp; // Creates the bitmap used
    }

    public void clearDrawing() { // Clears the drawing
        path.reset();
    }
}
