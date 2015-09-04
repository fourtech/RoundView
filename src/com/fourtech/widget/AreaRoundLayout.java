package com.fourtech.widget;

import android.content.Context;
import android.util.AttributeSet;

public class AreaRoundLayout extends ScrollRoundLayout {

	protected double mAngleScale;
	protected double mUnreachableAngle;

	public AreaRoundLayout(Context context) {
		this(context, null);
	}

	public AreaRoundLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AreaRoundLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mUnreachableAngle = 45;
		mAngleScale = (180 - mUnreachableAngle) / 180;
	}

	@Override
	protected double getArcAngle(double c, double l) {
		final double a = super.getArcAngle(c, l);
		if (a <= 180) {
			return a * mAngleScale;
		} else {
			return 180 + mUnreachableAngle + (a - 180) * mAngleScale;
		}
	}
}