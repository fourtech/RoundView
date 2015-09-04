package com.fourtech.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class RoundLayout extends FrameLayout {

	protected int mR; // radius
	protected int mAngle; // the angle of viewport
	protected int mViewPointZ; // z distance
	protected double mC; // circumference
	protected double mRadian; // radian of the angle
	protected double mScale; // to cooperate viewport
	protected float mOffsetX, mOffsetY;
	protected float mCellWidth; // width of each child view

	public RoundLayout(Context context) {
		this(context, null);
	}

	public RoundLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RoundLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundLayout, defStyle, 0);

		mR = a.getDimensionPixelSize(R.styleable.RoundLayout_radius, -1);
		mViewPointZ = a.getDimensionPixelSize(R.styleable.RoundLayout_viewPointZ, -1);
		mAngle = a.getInt(R.styleable.RoundLayout_viewAngle, 12);

		a.recycle();
	}

	private void initRoundLayout() {
		mC = 2 * Math.PI * mR;
		mRadian = toRadian(mAngle);
		if (mViewPointZ <= 0) mViewPointZ = 2 * mR;
		mScale = 1 - ((double) mViewPointZ / (mViewPointZ + 2 * mR));
		mOffsetX = (getMeasuredWidth() / 2f - mR);
		mOffsetY = (float) (mR * Math.sin(mRadian));
	}

	/**
	 * snap to a child
	 * @param child the child to snap
	 */
	public void snapToChild(View c) {
		if (c == null || c.getVisibility() == GONE || c.getParent() != this) {
			return;
		}

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (c == child) {
				mScrollX %= mC;
				float childLeft = i * mCellWidth;
				float targetScrollX = childLeft - mR + child.getMeasuredWidth()/2f;
				float deltaX1 = targetScrollX - mScrollX;
				float deltaX2 = (float) mC - targetScrollX + mScrollX;
				if (Math.abs(deltaX1) < Math.abs(deltaX2)) {
					doScrollByForSnapToChild((int) deltaX1, 0);
				} else {
					doScrollByForSnapToChild((int) deltaX2, 0);
				}
				break;
			}
		}
	}

	// Override this to enable animation
	protected void doScrollByForSnapToChild(int x, int y) {
		scrollBy(x, y);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (mR <= 0) mR = getMeasuredWidth() / 2;
		initRoundLayout();

		final int count = getChildCount();
		if (count <= 0) return;

		final double cellWidth = mC / count;
		final double H = getMeasuredHeight();
		mCellWidth = (float) cellWidth;

		// layout children in linear
		int childLeft, childTop;
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final int width = child.getMeasuredWidth();
				final int height = child.getMeasuredHeight();

				childLeft = (int) (i * cellWidth + (cellWidth - width) / 2);
				childTop = (int) ((H - height) / 2);
				child.layout(childLeft, childTop, childLeft + width, childTop + height);
			}
		}

		// snap to center child
		snapToChild(getChildAt(count / 2));
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		processRoundLayout(); // changed display

		final int cc = getChildCount();
		if (cc > 0) {
			final long drawingTime = getDrawingTime();

			canvas.save();
			// FIXME: A child contain a smaller z should draw first
			for (int i = 0; i < cc; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() == VISIBLE) {
					drawChild(canvas, child, drawingTime);
				}
			}
			canvas.restore();
		}
	}

	/** changed children display in round */
	protected void processRoundLayout() {
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v != null) {
				double[] coors = getViewCoordinates(v, mC, mR);
				double x = coors[0], y = coors[1], z = coors[2];

				// scale for perspective
				float scale = 1 - (float) ((z / (2 * mR)) * mScale);
				x = mR + (mR - x - v.getMeasuredWidth()/2) * scale;
				y = y * scale + y / 2;

				// change view display for perspective
				v.setTranslationX(mScrollX - v.getLeft() + (float) x + mOffsetX);
				v.setTranslationY((float) y + mOffsetY);
				v.setScaleX(scale);
				v.setScaleY(scale);
				v.setAlpha(scale);
			}
		}
	}

	/** get radian by angle **/
	protected double toRadian(double angle) {
		return angle * Math.PI / 180;
	}

	/**
	 * get angle of arc
	 * @param c circumference
	 * @param l length of arc
	 * @return angle of arc
	 */
	protected double getArcAngle(double c, double l) {
		return ((Math.abs(l) / c) * 360) % 360;
	}

	protected double[] getViewCoordinates(View view, double c, double r) {
		double[] coordinates = { 0, 0, 0, 0 };
		double l = view.getLeft() - mScrollX - r; // length of arc
		double a = getArcAngle(c, l); // angle
		double v = (l >= 0) ? 1 : -1; // vector direction

		if (a == 0 || a >= 360) {
			coordinates[0] = r;
			coordinates[1] = 0;
		} else if (a < 90) {
			double d = toRadian(a);
			coordinates[0] = r + v * r * Math.sin(d);
			coordinates[1] = r - r * Math.cos(d);
		} else if (a == 90) {
			coordinates[0] = r + r;
			coordinates[1] = r;
		} else if (a < 180) {
			double d = toRadian(a - 90);
			coordinates[0] = r + v * r * Math.cos(d);
			coordinates[1] = r + r * Math.sin(d);
		} else if (a == 180) {
			coordinates[0] = r;
			coordinates[1] = r + r;
		} else if (a < 270) {
			double d = toRadian(270 - a);
			coordinates[0] = r - v * r * Math.cos(d);
			coordinates[1] = r + r * Math.sin(d);
		} else if (a == 270) {
			coordinates[0] = 0;
			coordinates[1] = r;
		} else if (a < 360) {
			double d = toRadian(a - 270);
			coordinates[0] = r - v * r * Math.cos(d);
			coordinates[1] = r - r * Math.sin(d);
		}

		double y = -coordinates[1];
		coordinates[1] = y * Math.sin(mRadian);
		coordinates[2] = Math.abs(y) * Math.cos(mRadian);

		return coordinates;
	}

}