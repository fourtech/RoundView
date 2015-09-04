package com.fourtech.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class ScrollRoundLayout extends RoundLayout {

	protected static final int MIN_LENGTH_FOR_FLING = 6;
	protected static final int FLING_THRESHOLD_VELOCITY = 500;

	protected final int TOUCH_STATE_REST = 0;
	protected final int TOUCH_STATE_SCROLLING_X = 1;
	protected int mTouchState = TOUCH_STATE_REST;

	protected int mTouchSlop;
	protected float mDownMotionX;
	protected float mLastMotionX;
	protected float mLastMotionXRemainder;
	protected int mMaximumFlingVelocity;
	protected int mMinimumFlingVelocity;

	protected Scroller mScroller;
	protected int mMixScrollDuration = 60;
	protected int mMaxScrollDuration = 600;
	protected float mAcceleration = 0.004f;

	protected static final int INVALID_POINTER = -1;
	protected int mActivePointerId = INVALID_POINTER;
	protected VelocityTracker mVelocityTracker;

	protected static class ScrollInterpolator implements Interpolator {
		public ScrollInterpolator() {
		}

		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1;
		}
	}

	public ScrollRoundLayout(Context context) {
		this(context, null);
	}

	public ScrollRoundLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollRoundLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();
		mMaximumFlingVelocity = config.getScaledMaximumFlingVelocity();
		mMinimumFlingVelocity = config.getScaledMinimumFlingVelocity();

		mScroller = new Scroller(context, new ScrollInterpolator());
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		// Skip touch handling if there are no child to swipe
		if (getChildCount() <= 0) return super.onInterceptTouchEvent(event);
		acquireVelocityTrackerAndAddMovement(event); // Acquire Velocity

		final int action = event.getAction();
		if ((action == MotionEvent.ACTION_MOVE && mTouchState != TOUCH_STATE_REST))
			return true;

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE: {
			if (mActivePointerId != INVALID_POINTER) {
				determineDraggingStart(event);
				break;
			}
			// If here, means a down motion has missed
			// In this case, do not break
		}

		case MotionEvent.ACTION_DOWN: {
			mDownMotionX = mLastMotionX = event.getX();
			mLastMotionXRemainder = 0;
			mActivePointerId = event.getPointerId(0);

			// look up last scrlling state
			final int xDist = Math.abs(mScroller.getFinalX() - mScroller.getCurrX());
			final boolean finishedScrolling = (mScroller.isFinished() || xDist < mTouchSlop);

			// stop scroll anyway
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			if (finishedScrolling) {
				mTouchState = TOUCH_STATE_REST;
			} else {
				mTouchState = TOUCH_STATE_SCROLLING_X;
			}
			break;
		}

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
			mActivePointerId = INVALID_POINTER;
			releaseVelocityTracker();
			break;

		case MotionEvent.ACTION_POINTER_UP:
			releaseVelocityTracker();
			break;
		}

		return (mTouchState != TOUCH_STATE_REST);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		// Skip touch handling if there are no child to swipe
		if (getChildCount() <= 0) return super.onTouchEvent(event);
		acquireVelocityTrackerAndAddMovement(event); // Acquire Velocity

		final int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mDownMotionX = mLastMotionX = event.getX();
			mLastMotionXRemainder = 0;
			mActivePointerId = event.getPointerId(0);

			// stop scroll anyway
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			break;

		case MotionEvent.ACTION_MOVE:
			if (mTouchState == TOUCH_STATE_SCROLLING_X) {
				// Scroll to follow the motion event
				final float x = event.getX();
				final float deltaX = x - (mLastMotionX + mLastMotionXRemainder);

				// move when deltaY >= 1db, or move next time
				if (Math.abs(deltaX) >= 1.0f) {
					scrollBy((int) deltaX, 0);

					mLastMotionX = x;
					mLastMotionXRemainder = deltaX - (int) deltaX;
				}
			} else if (mTouchState == TOUCH_STATE_REST) {
				determineDraggingStart(event);
			}
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mTouchState == TOUCH_STATE_SCROLLING_X) {
				final float x = event.getX();
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
				float velocityX = velocityTracker.getXVelocity();
				final float totalDeltaX = (x - mDownMotionX);

				boolean isFling = Math.abs(totalDeltaX)>MIN_LENGTH_FOR_FLING && Math.abs(velocityX)>mMinimumFlingVelocity;

				// start fling
				if (isFling) {
					startScroll(velocityX);
				}
			}
			mTouchState = TOUCH_STATE_REST;
			mActivePointerId = INVALID_POINTER;
			releaseVelocityTracker();
			break;

		case MotionEvent.ACTION_POINTER_UP:
			releaseVelocityTracker();
			break;
		}

		return true/*(mTouchState == TOUCH_STATE_REST)*/;
	}

	protected void startScroll(float velocityX) {
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}
		// velocityX /= 1000; // dip per millisecond
		float distance = velocityX * velocityX / (2000000 * mAcceleration); // 2 * 1000 * 1000 * mAcceleration
		int duration = (int) Math.abs(2000 * distance / velocityX); // 2 * distance / (velocityX / 1000)
		duration = Math.min(Math.max(duration, mMixScrollDuration), mMaxScrollDuration); // ((velocityX / 1000) / 2)
		int deltaX = (int) (velocityX > 0 ? distance : -distance);
		mScroller.startScroll(mScrollX, 0, deltaX, 0, duration);
		invalidate();
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			invalidate();
		}
	}

	@Override
	protected void doScrollByForSnapToChild(int x, int y) {
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}

		mScroller.startScroll(mScrollX, 0, x, 0, mMaxScrollDuration);
		invalidate();
	}

	/** determine dragging if set to start state */
	protected void determineDraggingStart(MotionEvent event) {
		final int pointerIndex = event.findPointerIndex(mActivePointerId);
		if (pointerIndex == -1) {
			return;
		}

		final float x = event.getX(pointerIndex);
		final float xDiff = Math.abs(x - mLastMotionX);

		if (xDiff > mTouchSlop) {
			mTouchState = TOUCH_STATE_SCROLLING_X;
		}
	}

	private void acquireVelocityTrackerAndAddMovement(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
	}

	private void releaseVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

}