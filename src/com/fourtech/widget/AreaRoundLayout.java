package com.fourtech.widget;

import android.content.Context;
import android.util.AttributeSet;

public class AreaRoundLayout extends ScrollRoundLayout {

	protected double mAngleScale;
	protected double mUnreachableAngle;
	protected double mCoefficient = 0.4;

	public AreaRoundLayout(Context context) {
		this(context, null);
	}

	public AreaRoundLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AreaRoundLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mUnreachableAngle = 60;
		mAngleScale = (180 - mUnreachableAngle) / 180;
	}

	@Override
	protected double getArcAngle(double c, double l) {
		final double a = super.getArcAngle(c, l);
		if (a <= 180) {
			double ac = a * (180 - a) / 180.0 * mCoefficient;
			return (a - ac) * mAngleScale;
		} else {
			double ac = (a - 180) * (360 - a) / 180.0 * mCoefficient;
			return 180 + mUnreachableAngle + (a + ac - 180) * mAngleScale;
		}
	}

	@Override
	protected float computeDeltaX(float x, float lastX, float remainder) {
		return 2 * super.computeDeltaX(x, lastX, remainder);
		/* return (1.0f + (float) mCoefficient + (float) mAngleScale) * super.computeDeltaX(x, lastX, remainder) */
	}

}