package com.split_keyboard;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class TouchEvent {
	
	final int DRAG_TIME = 350;
	final int DWELL_DIST = 70;
	final int SWIPE_DIST = 90;
	
	public final static int EVENT_NULL = 0;
	public final static int EVENT_CLICK = 1;
	public final static int EVENT_DRAG = 2;
	public final static int EVENT_SWIPE_LEFT = 3;
	public final static int EVENT_SWIPE_RIGHT = 4;
	public final static int EVENT_SWIPE_UP = 5;
	public final static int EVENT_SWIPE_DOWN = 6;
	
	int downX, downY;
	int x, y;
	Timer timer;
	boolean hand;
	boolean drag = false;
	
	public TouchEvent(int downX, int downY) {
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
		return drag;
	}
	
	public int up(int x, int y) {
		this.x = x;
		this.y = y;
		if (drag) return EVENT_DRAG;
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
		return Math.sqrt(Math.pow(x - downX, 2) + Math.pow(y - downY, 2)) < DWELL_DIST;
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
	}
}



class Point {
	
	int x, y, hand;
	Date date;
	
	public Point() {
		x = y = 0;
	}
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
		this.hand = (x < 1280) ? 1 : 0;
		this.date = new Date();
	}
}