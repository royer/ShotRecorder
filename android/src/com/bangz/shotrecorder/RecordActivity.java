package com.bangz.shotrecorder;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class RecordActivity extends SherlockActivity {
	
	public static final String TAG = "RecorderActivity";
	public static final String FONT_PATH = "fonts/digital-7 (italic).ttf" ;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_recorder);
		
		TextView v = (TextView)findViewById(R.id.textView1);
		
		Typeface tf = Typeface.createFromAsset(getAssets(), FONT_PATH) ;
		
		v.setTypeface(tf);
		v.setText("127.45");
	}

}
