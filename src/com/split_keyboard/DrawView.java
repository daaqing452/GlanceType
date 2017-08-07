package com.split_keyboard;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

class DrawView extends View {
	Bitmap bitmap = null;
	Canvas canvas = null;
	
	public DrawView(Context context) {
		super(context);
		bitmap = Bitmap.createBitmap(2560, 1600, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		drawKeyCentriods();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (bitmap != null) canvas.drawBitmap(bitmap, 0, 0, new Paint());
	}

	/** eyes-free **/
	/* final int Y0 = 1250;
	final int[] LX = new int[] {133, 175, 235};
	final int[] RX = new int[] {1453, 1495, 1555};
	final int Y = 130;
	final int X = 104;
	final int BIAS = 194; */
	
	/** eyes-focus **/
	/*final int Y0 = 1140;
	final int[] LX = new int[] {133, 175, 235};
	final int[] RX = new int[] {1453, 1495, 1555};
	final int Y = 130;
	final int X = 104;
	final int BIAS = 194;*/
	
	/** addition **/
	final int Y0 = 1067;
	final int[] LX = new int[] {93, 93, 93, 93, 93};
	final int[] RX = new int[] {1530, 1530, 1530, 1530, 1530};
	final int Y = 115;
	final int X = 104;
	final int BIAS = 194;
	
	void drawKeyCentriods() {
		Paint p = new Paint();
		p.setColor(Color.RED);
		p.setStrokeWidth(2.0f);
		for (int y = 0; y < LX.length; y++) {
			for (int x = 0; x < 5; x++) {
				canvas.drawCircle(LX[y] + x * X, Y0 + y * Y - BIAS, 2, p);
			}
			for (int x = 4; x < 10; x++) {
				canvas.drawCircle(RX[y] + x * X, Y0 + y * Y - BIAS, 2, p);
			}
		}
		int[] location = new int[2];
		this.getLocationOnScreen(location);
		canvas.drawText(location[0] + " " + location[1], 10, 80, p);
	}
}