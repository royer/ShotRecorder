package com.bangz.shotrecorder;

import com.actionbarsherlock.app.SherlockActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.app.Activity;
import android.content.Intent;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity implements 
View.OnClickListener
{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button	button = (Button)findViewById(R.id.startRecorderActivity);
		button.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}



	@Override
	public void onClick(View v) {
		
		switch(v.getId()) {
		case R.id.startRecorderActivity:
			gotoRecorderActivity();
			break;
		}
		
	}
	
	
	private void gotoRecorderActivity() {
		
		Intent intent = new Intent(this, RecordActivity.class);
		startActivity(intent);
	}
	

}
