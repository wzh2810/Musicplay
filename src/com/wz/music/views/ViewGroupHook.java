package com.wz.music.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class ViewGroupHook extends FrameLayout {

	public ViewGroupHook(Context paramContext) {
		super(paramContext);
	}

	public ViewGroupHook(Context paramContext, AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
	}

	public ViewGroupHook(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
	}

	// 为了保证能够响应触摸事件 返回true
	public boolean onTouchEvent(MotionEvent paramMotionEvent) {
		//		return super.onTouchEvent(paramMotionEvent);
		//action_down
		//action_move
		switch (paramMotionEvent.getAction()) {
		case MotionEvent.ACTION_DOWN:
			System.out.println("---viewGroudHook---onTouchEvent---ACTION_DOWN");
			break;
		case MotionEvent.ACTION_MOVE:
			System.out.println("---viewGroudHook---onTouchEvent---ACTION_MOVE");
			break;
		case MotionEvent.ACTION_UP:
			System.out.println("---viewGroudHook---onTouchEvent---ACTION_UP");

			break;
		case MotionEvent.ACTION_CANCEL:
			System.out.println("---viewGroudHook---onTouchEvent---ACTION_CANCEL");
			
			break;

		default:
			break;
		}
		return true;//action_down
	}
}
