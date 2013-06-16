/**
 * Copyright (C) 2013 Bangz
 * 
 * @author Royer Wang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 */

package com.bangz.shotrecorder;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.content.CursorLoader ;
import android.support.v4.app.LoaderManager;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.mobeta.android.dslv.DragSortListView;

public class MainActivity extends SherlockFragmentActivity implements
View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MainActivity";

    private SimpleCursorAdapter mAdapter ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button	button = (Button)findViewById(R.id.startRecorderActivity);
		button.setOnClickListener(this);

        String[] columns = new String[] {ShotRecord.ShotRecords.COLUMN_NAME_DATE,
            ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,
            ShotRecord.ShotRecords.COLUMN_NAME_SHOTS,
            ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME};
        int [] listitemids = new int[] {
          R.id.txtDate, R.id.txtDescription, R.id.txtNumber, R.id.txtTime,
        };

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.recorder_list_entry,
                null,
                columns,
                listitemids,0) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                int idxID = cursor.getColumnIndex(ShotRecord.ShotRecords._ID);
                int idxDate = cursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_DATE);
                int idxDescript = cursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION);
                int idxSpendTime = cursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME);
                int idxShots = cursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SHOTS);

                String date = cursor.getString(idxDate);
                date = date.substring(0,date.lastIndexOf(':'));
                String descript = cursor.getString(idxDescript);
                long shots = cursor.getLong(idxShots);
                long spendtime = cursor.getLong(idxSpendTime);

                TextView v = (TextView)view.findViewById(R.id.txtDate);
                v.setText(date) ;

                v = (TextView)view.findViewById(R.id.txtDescription);
                v.setText(descript);

                v = (TextView)view.findViewById(R.id.txtNumber);
                v.setText(String.format("%d",shots));

                v = (TextView)view.findViewById(R.id.txtTime);
                v.setText(String.format("%.02f\"",spendtime/1000.0));
            }
        };

        DragSortListView listView = (DragSortListView)findViewById(R.id.listRecords);
        listView.setAdapter(mAdapter);

        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(1, null, this);

    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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


    private static final String[] PROJECTION = new String[] {
            ShotRecord.ShotRecords._ID,
            ShotRecord.ShotRecords.COLUMN_NAME_DATE,
            ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,
            ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME,
            ShotRecord.ShotRecords.COLUMN_NAME_SHOTS,
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String select = new String();
        return new CursorLoader(this, ShotRecord.ShotRecords.CONTENT_URI,PROJECTION,select, null, null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }



}
