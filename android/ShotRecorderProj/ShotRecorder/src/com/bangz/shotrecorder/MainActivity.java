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

import java.util.Arrays;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.content.CursorLoader ;
import android.support.v4.app.LoaderManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.mobeta.android.dslv.DragSortListView;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//import com.google.api.services.drive.DriveScopes;
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class MainActivity extends SherlockFragmentActivity implements
View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>,
AdapterView.OnItemClickListener,
DragSortListView.RemoveListener {

    private static final String TAG = "MainActivity";

    private SimpleCursorAdapter mAdapter ;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = getIntent() ;

        if (intent.getData() == null) {
            intent.setData(ShotRecord.ShotRecords.CONTENT_URI);
        }

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
                v.setText(String.format("%.02f",spendtime/1000.0));
            }
        };




        TextView emptyView = (TextView)findViewById(R.id.empty) ;

        DragSortListView listView = (DragSortListView)findViewById(R.id.listRecords);

        listView.setEmptyView(emptyView);

        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        listView.setRemoveListener(this);

        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(1, null, this);
        
        // temp for google drive code
        
        // check google play service is install
//        int r = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) ;
//        if (r != ConnectionResult.SUCCESS) {
//        	GooglePlayServicesUtil.getErrorDialog(r, this, REQUEST_ACCOUNT_PICKER).show();
//        }
//        else {
//	        credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
//	        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
//        }

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


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Uri uri = ContentUris.withAppendedId(ShotRecord.ShotRecords.CONTENT_URI,id);
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
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
        return new CursorLoader(this, getIntent().getData(),PROJECTION,select, null, null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }


    @Override
    public void remove(int which) {

        DragSortListView listView = (DragSortListView)findViewById(R.id.listRecords);

        long id = listView.getItemIdAtPosition(which) ;

        Uri uri = ContentUris.withAppendedId(getIntent().getData(),id);
        getContentResolver().delete(uri,null,null);

    }
}
