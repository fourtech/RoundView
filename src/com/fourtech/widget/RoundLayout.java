package com.fourtech.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
	private static final int TAG_COORS = 0x444 << 24;

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
				adjustScroll();
				float targetScrollX = child.getLeft() + child.getMeasuredWidth()/2f - mR;

				double[] deltaX = {
						targetScrollX - mScrollX,
						targetScrollX - mC - mScrollX,
						targetScrollX + mC - mScrollX
				};

				double temp = 0;
				for (int j = deltaX.length - 1; j > 0; --j) {
					for (int k = 0; k < j; ++k) {
						if (Math.abs(deltaX[k+1]) < Math.abs(deltaX[k])) {
							temp = deltaX[k];
							deltaX[k] = deltaX[k+1];
							deltaX[k+1] = temp;
						}
					}
				}

				doScrollByForSnapToChild((int) deltaX[0], 0);
				break;
			}
		}
	}

	protected void adjustScroll() {
		if (mScrollX >= 0) {
			mScrollX %= mC;
		} else {
			double sx = (-mScrollX) % mC;
			mScrollX = (int) (mC - sx);
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

				childLeft = (int) (i * cellWidth + (cellWidth - width) / 2.0);
				childTop = (int) ((H - height) / 2.0);
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
			List<View> vs = new ArrayList<View>();
			for (int i = 0, c = cc; i < c; i++) {
				final View v = getChildAt(i);
				if (v.getVisibility() != GONE) {
					vs.add(v);
				}
			}

			Collections.sort(vs, mComparator);

			// Draw smaller z coordinate view first
			for (int i = 0; i < vs.size(); i++) {
				drawChild(canvas, vs.get(i), drawingTime);
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
				v.setTag(TAG_COORS, coors);

				// scale for perspective
				int width = v.getMeasuredWidth();
				float scale = (float) (1 - ((z / (2 * mR)) * mScale));
				x = mR - (mR - x) * scale;
				y = y * scale + y / 2.0;

				// change view display for perspective
				v.setTranslationX(mScrollX - v.getLeft() + mOffsetX + (float) x - width/2f);
				v.setTranslationY((float) y + mOffsetY);
				v.setScaleX(scale);
				v.setScaleY(scale);
				v.setAlpha(scale);
			}
		}
	}

	private Comparator<View> mComparator = new Comparator<View>() {
		@Override
		public int compare(View v0, View v1) {
			final double z0 = ((double[]) v0.getTag(TAG_COORS))[2];
			final double z1 = ((double[]) v1.getTag(TAG_COORS))[2];
			if (z1 > z0) return 1;
			if (z1 < z0) return -1;
			return 0;
		}
	};

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
		double l = mScrollX + r - view.getLeft() - (view.getMeasuredWidth()/2f); // length of arc
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