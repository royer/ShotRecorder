package com.bangz.shotrecorder;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class RecordActivity extends SherlockFragmentActivity {
	
	public static final String TAG = "RecorderActivity";
	
	public static enum MODE { COMSTOCK, VIRGINIA, PAR_TIME }
	
	private MODE		mMode ;
	
	private String[]	strModeNames ;
	private String[]	strModeValNames ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_recorder);
		
		strModeNames = getResources().getStringArray(R.array.mode_names) ;
		strModeValNames = getResources().getStringArray(R.array.mode_value_names) ;
		
		mMode = MODE.COMSTOCK ;

		setDigitFont();
		
		initButtonsListener() ;
		
		TextView v = (TextView)findViewById(R.id.textTIME);
		v.setText("------");
		
		
		
		updateMode();
	}
	
	private void initButtonsListener() {
		
		View.OnClickListener modebuttonlsn = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				mMode = MODE.values()[(mMode.ordinal() + 1) % MODE.values().length] ;
				
				updateMode();
			}
		};
		
		Button btnmode = (Button)findViewById(R.id.btnMode) ;
		btnmode.setOnClickListener(modebuttonlsn) ;
	}
	
	private void setDigitFont() {
		
		final String FONT_ITALIC_PATH = "fonts/digital-7 (italic).ttf" ;
		final String FONT_NORMAL_PATH = "fonts/digital-7.ttf" ;
		
		Typeface tfItalic = Typeface.createFromAsset(getAssets(), FONT_ITALIC_PATH);
		Typeface tfNormal = Typeface.createFromAsset(getAssets(), FONT_NORMAL_PATH);
		
		TextView v = (TextView)findViewById(R.id.textTIME) ;
		
		v.setTypeface(tfItalic) ; // style bold | italic = 1| 2 = 3;
		
		v = (TextView)findViewById(R.id.textElapsedTime) ;
		v.setTypeface(tfItalic);
		
		v = (TextView)findViewById(R.id.textTimeLable) ;
		v.setTypeface(tfNormal);
		
		v = (TextView)findViewById(R.id.textmode) ;
		v.setTypeface(tfNormal);
		
		v = (TextView)findViewById(R.id.textModePrefix) ;
		v.setTypeface(tfNormal);
		
		v = (TextView)findViewById(R.id.textModeValue) ;
		v.setTypeface(tfNormal);
		
		v = (TextView)findViewById(R.id.textNumberLabel);
		v.setTypeface(tfItalic);
		
		v = (TextView)findViewById(R.id.textNumber) ;
		v.setTypeface(tfItalic);
		
		v = (TextView)findViewById(R.id.textSplitLabel);
		v.setTypeface(tfItalic);
		
		v = (TextView)findViewById(R.id.textSplit);
		v.setTypeface(tfItalic);
		v.setText("12.99");
	}

	private void updateMode() {
		
		TextView vModeName = (TextView)findViewById(R.id.textmode) ;
		TextView vModeValName = (TextView)findViewById(R.id.textModePrefix) ;
		TextView vModeVal = (TextView)findViewById(R.id.textModeValue);

		vModeName.setText(strModeNames[mMode.ordinal()]);
		
		vModeValName.setText(strModeValNames[mMode.ordinal()]);
		
		int visable = (mMode != MODE.COMSTOCK)?View.VISIBLE : View.INVISIBLE ;
		vModeValName.setVisibility(visable);
		vModeVal.setVisibility(visable);
	}
}
