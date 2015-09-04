package com.fourtech.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;

public class MainActivity extends Activity implements OnClickListener {

	private RadioButton mSelectedView;
	private RoundLayout rl, srl, arl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		rl = (RoundLayout) findViewById(R.id.rl);
		int count = rl.getChildCount();
		for (int i = 0; i < count; i++) {
			rl.getChildAt(i).setOnClickListener(this);
		}

		srl = (RoundLayout) findViewById(R.id.srl);
		count = srl.getChildCount();
		for (int i = 0; i < count; i++) {
			srl.getChildAt(i).setOnClickListener(this);
		}

		arl = (RoundLayout) findViewById(R.id.arl);
		count = arl.getChildCount();
		for (int i = 0; i < count; i++) {
			arl.getChildAt(i).setOnClickListener(this);
		}

		View st = findViewById(R.id.btn_static);
		st.setOnClickListener(this);
		findViewById(R.id.btn_scroll).setOnClickListener(this);
		findViewById(R.id.btn_area).setOnClickListener(this);

		mSelectedView = (RadioButton) st;
		mSelectedView.setChecked(true);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_static) {
			selecteView(v);
			rl.setVisibility(View.VISIBLE);
			srl.setVisibility(View.GONE);
			arl.setVisibility(View.GONE);
		} else if (v.getId() == R.id.btn_scroll) {
			selecteView(v);
			rl.setVisibility(View.GONE);
			srl.setVisibility(View.VISIBLE);
			arl.setVisibility(View.GONE);
		} else if (v.getId() == R.id.btn_area) {
			selecteView(v);
			rl.setVisibility(View.GONE);
			srl.setVisibility(View.GONE);
			arl.setVisibility(View.VISIBLE);
		} else {
			if (rl.getVisibility() == View.VISIBLE) {
				rl.snapToChild(v);
			} else if (srl.getVisibility() == View.VISIBLE) {
				srl.snapToChild(v);
			} else if (arl.getVisibility() == View.VISIBLE) {
				arl.snapToChild(v);
			}
		}

	}

	private void selecteView(View v) {
		if (mSelectedView != v) {
			mSelectedView.setChecked(false);
		}

		mSelectedView = (RadioButton) v;
		mSelectedView.setChecked(true);
	}

}
