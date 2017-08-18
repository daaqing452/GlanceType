package com.split_keyboard;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

public class TouchEvent {
	
	final int DRAG_TIME = 300;
	final int DWELL_DIST = 50;
	final int SWIPE_DIST = 90;
	//final int RUB_DIST_1 = 20;
	//final int RUB_DIST_2 = 5;
	
	public final static int EVENT_NULL = 0;
	public final static int EVENT_CLICK = 1;
	public final static int EVENT_DRAG = 2;
	public final static int EVENT_SWIPE_LEFT = 3;
	public final static int EVENT_SWIPE_RIGHT = 4;
	public final static int EVENT_SWIPE_UP = 5;
	public final static int EVENT_SWIPE_DOWN = 6;
	public final static int EVENT_RUB = 7;
	public final static int EVENT_DWELL = 8;
	
	Activity activity;
	int downX, downY;
	int x, y;
	Timer timer;
	boolean hand;
	boolean drag = false;
	
	//double maxX = -1e20, minX = 1e20, maxY = -1e20, minY = 1e20;
	double maxDist = 0;
	int maxX = 0, maxY = 0;
	
	public TouchEvent(Activity activity, int downX, int downY) {
		this.activity = activity;
		this.x = this.downX = downX;
		this.y = this.downY = downY;
		hand = (x < 1280);
		timer = new Timer();
		timer.schedule(new TouchEventTimerTask(this), DRAG_TIME);
	}
	
	public boolean anyMove(int x, int y) {
		if (this.x == x && this.y == y) return false;
		this.x = x;
		this.y = y;
		double nowDist = Point.dist(x, y, downX, downY);
		if (nowDist > maxDist) {
			maxDist = nowDist;
			maxX = x;
			maxY = y;
		}
		return drag;
	}
	
	public int up(int x, int y) {
		timer.cancel();
		this.x = x;
		this.y = y;
		//Log.d("rub", maxDist + " " + Point.dist(x, y, maxX, maxY));
		if (drag) return (maxDist < DWELL_DIST) ? EVENT_DWELL : EVENT_DRAG;
		//if (maxDist > RUB_DIST_1 && Point.dist(x, y, maxX, maxY) > RUB_DIST_2) return EVENT_RUB;
		if (ifDwell()) return EVENT_CLICK;
		if (Math.abs(x - downX) > Math.abs(y - downY)) {
			if (x < downX - SWIPE_DIST) return EVENT_SWIPE_LEFT;
			if (x > downX + SWIPE_DIST) return EVENT_SWIPE_RIGHT;
		} else {
			if (y < downY - SWIPE_DIST) return EVENT_SWIPE_UP;
			if (y > downY + SWIPE_DIST) return EVENT_SWIPE_DOWN;
		}
		return EVENT_NULL;
	}
	
	public boolean ifDwell() {
		return Point.dist(x,  y, downX, downY) < DWELL_DIST;
	}
}



class TouchEventTimerTask extends TimerTask {
	
	TouchEvent father;
	
	public TouchEventTimerTask(TouchEvent father) {
		this.father = father;
	}
	
	public void run() {
		//if (!father.ifDwell()) return;
		father.drag = true;
		Vibrator vibrator =  (Vibrator)father.activity.getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = {0, 20};
		vibrator.vibrate(pattern, -1);
	}
}



class Point {
	
	int x, y, hand;
	Date date;
	boolean uppercase;
	
	public Point() {
		x = y = 0;
		uppercase = false;
	}
	
	public Point(int x, int y, boolean uppercase) {
		this.x = x;
		this.y = y;
		this.uppercase = uppercase;
		this.hand = (x < 1280) ? 1 : 0;
		this.date = new Date();
	}
	
	public Point(int x, int y) {
		this(x, y, false);
	}
	
	static double dist(int x, int y, int xx, int yy) {
		return Math.sqrt(Math.pow(x - xx, 2) + Math.pow(y - yy, 2));
	}
}