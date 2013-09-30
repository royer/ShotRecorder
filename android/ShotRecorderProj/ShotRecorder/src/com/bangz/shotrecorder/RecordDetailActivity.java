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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;

/**
 * Created by royer on 18/06/13.
 */
public class RecordDetailActivity extends SherlockFragmentActivity
implements SplitListFragment.OnSplitItemSelectedListerner,
    LoaderManager.LoaderCallbacks<Cursor>,
    ImageView.OnClickListener,
    GetDescriptDialogFragment.DescriptDialogListener {

    private static final String TAG = "RecordDetailActivity" ;

    private Uri mUri ;


    private ShareActionProvider mShareActionProvider ;


    private Cursor mCursor ;
    private boolean mbModified = false;

    SplitManager    mManager = new SplitManager() ;
    SplitArrayAdapter mSplitAdapter ;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.record_detail);

        final Intent intent = getIntent() ;

        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {

            mUri = intent.getData();

        } else {
            Log.e(TAG, "Unknown Action, exiting") ;
            finish();
            return ;
        }

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);


        ImageView imageView = (ImageView)findViewById(R.id.imgEditDescript);
        imageView.setOnClickListener(this);

        FragmentManager fm = getSupportFragmentManager();
        SplitListFragment splitfragment = (SplitListFragment)fm.findFragmentById(R.id.splitlist);
        mSplitAdapter = new SplitArrayAdapter(this, mManager.getSplits());
        splitfragment.setListAdapter(mSplitAdapter);

        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(1,null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.detailactivity, menu);

        MenuItem shareitem = menu.findItem(R.id.menu_item_share);

        mShareActionProvider = (ShareActionProvider) shareitem.getActionProvider();
        //mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);


        return true ;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.menu_item_share).setEnabled(mManager.getNumbers() > 0) ;

        setShareIntent();

        return true ;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void setShareIntent() {

        if (mShareActionProvider != null) {

            final TextView viewNumber = (TextView)findViewById(R.id.txtNumber);
            String strnumber = viewNumber.getText().toString();

            final TextView viewTime = (TextView)findViewById(R.id.txtTime) ;
            String strTime = viewTime.getText().toString();

            final TextView viewDate = (TextView)findViewById(R.id.txtDate);
            String strDate = viewDate.getText().toString();

            final TextView viewdescription = (TextView)findViewById(R.id.txtDescription);
            String strDescription = viewdescription.getText().toString();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String strcontent = String.format(
                    getResources().getString(R.string.share_content),
                    strDescription,
                    strnumber,strTime, strDate);
            intent.putExtra(Intent.EXTRA_TEXT, strcontent) ;
            mShareActionProvider.setShareIntent(intent);
        }

    }

    @Override
    public void onSplitItemSelected(int position) {

    }

    @Override
    public void onSplitItemRemoved(int position) {

        mbModified = true ;
        mManager.remove(position);

        int number = mManager.getNumbers() ;
        long spendtime = mManager.getTotalElapsedTime() ;

        TextView view = (TextView)findViewById(R.id.txtNumber);
        view.setText(String.format("%d", number)) ;

        view = (TextView)findViewById(R.id.txtTime);
        view.setText(String.format("%.02f",spendtime/1000.0)) ;

        mSplitAdapter.notifyDataSetChanged();

        setShareIntent();

    }

    private static final String[] PROJECTION = new String[] {
            ShotRecord.ShotRecords._ID,
            ShotRecord.ShotRecords.COLUMN_NAME_DATE,
            ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,
            ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME,
            ShotRecord.ShotRecords.COLUMN_NAME_SHOTS,
            ShotRecord.ShotRecords.COLUMN_NAME_SPLITS,
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, mUri,PROJECTION, "",null, null );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor ;

        updateView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null ;
    }

    private void updateView() {

        if ( mCursor == null ) return ;

        mCursor.moveToFirst();

        int idxDate = mCursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_DATE);
        int idxDescript = mCursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION);
        int idxShots = mCursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SHOTS);
        int idxTime = mCursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME);
        int idxSplits = mCursor.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SPLITS);

        String strDate = mCursor.getString(idxDate);
        final TextView viewDate = (TextView)findViewById(R.id.txtDate);
        viewDate.setText(strDate);

        final String strdescript = mCursor.getString(idxDescript);
        final TextView viewDescript = (TextView)findViewById(R.id.txtDescription);
        viewDescript.setText(strdescript);

        long shots = mCursor.getLong(idxShots);
        final TextView viewShots = (TextView)findViewById(R.id.txtNumber);
        viewShots.setText(String.format("%d",shots));

        long spendtime = mCursor.getLong(idxTime);
        final TextView viewSpendTime = (TextView)findViewById(R.id.txtTime);
        viewSpendTime.setText(String.format("%.02f",spendtime/1000.0));

        String strsplits = mCursor.getString(idxSplits);
        mManager.rebuildFromJSONString(strsplits);
        mSplitAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.imgEditDescript) {

            TextView textView = (TextView)findViewById(R.id.txtDescription);
            String strdescript = textView.getText().toString() ;

            GetDescriptDialogFragment d = GetDescriptDialogFragment.newInstance(strdescript);
            d.show(getSupportFragmentManager(),"getDescriptDialogFragment");

        }
    }

    @Override
    public void onGetDescription(GetDescriptDialogFragment dialog, String descript) {


        TextView v = (TextView)findViewById(R.id.txtDescription);
        String sold = v.getText().toString();

        if (sold.equals(descript) == false) {
            v.setText(descript);

            mbModified = true ;

            setShareIntent();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mbModified) {

            ContentValues values = new ContentValues();

            TextView v = (TextView)findViewById(R.id.txtDescription);
            String strDescript = v.getText().toString();
            values.put(ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION, strDescript) ;

            values.put(ShotRecord.ShotRecords.COLUMN_NAME_SHOTS, mManager.getNumbers());
            values.put(ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME, mManager.getTotalElapsedTime());

            values.put(ShotRecord.ShotRecords.COLUMN_NAME_SPLITS,mManager.toJSONString()) ;


            getContentResolver().update(mUri, values, null, null);
            mbModified = false ;
        }
    }
}