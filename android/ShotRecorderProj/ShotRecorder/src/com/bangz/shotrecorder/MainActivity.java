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

//import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.accounts.AccountManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.mobeta.android.dslv.DragSortListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.services.drive.DriveScopes;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class MainActivity extends SherlockFragmentActivity implements
View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>,
AdapterView.OnItemClickListener,
DragSortListView.RemoveListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ACCOUNT_PICKER = 1;
    private static final int REQUEST_AUTHORIZATION = 2;
    private static final int REQUEST_INSTALL_GOOGLEPLAYSERVICE = 3;

    private static final int REQUEST_FOR_BACKUP_GOOGLE_DRIVE = 1;
    private static final int REQUEST_FOR_RESTORE_GOOGLE_DRIVE = 2;
    private int backup_or_restore ;

    private SimpleCursorAdapter mAdapter ;

    private Uri dbfileUri ;
    private Drive googleDriveService ;
    private GoogleAccountCredential credential;

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
        

    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getSupportMenuInflater() ;
        menuInflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_backup:
                try {
                    backup();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.action_restore:
                try {
                    restore();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
	public void onClick(View v) {
		
		switch(v.getId()) {
		case R.id.startRecorderActivity:
			gotoRecorderActivity();
			break;
		}
		
	}

    @Override
    protected void onResume() {
        super.onResume();
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

    private boolean backup() throws IOException {

        dbfileUri = Uri.fromFile(getDatabasePath(ShotRecordProvider.DATABASE_NAME));

        backup_or_restore = REQUEST_FOR_BACKUP_GOOGLE_DRIVE ;

        // Check Google Play serevice is installed
        int r = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (r != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(r)) {
                GooglePlayServicesUtil.getErrorDialog(r, this, REQUEST_INSTALL_GOOGLEPLAYSERVICE).show();
            } else {
                Toast.makeText(this,R.string.unsupport_google_drive, Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            // Author Google account
            String[] scopes = { DriveScopes.DRIVE, DriveScopes.DRIVE_APPDATA} ;
            credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(scopes)) ;
            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        }

        return true ;
    }

    private boolean restore() throws IOException {
        //String topath = "/data/data/com.bangz.shotrecorder/databases/"+"shotrecord.db" ;
        //String topath = getFilesDir() + "/databases/shotrecord.db" ;
        dbfileUri = Uri.fromFile(getDatabasePath(ShotRecordProvider.DATABASE_NAME));

        backup_or_restore = REQUEST_FOR_RESTORE_GOOGLE_DRIVE ;

        if (googleDriveService == null) {
            int r = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) ;
            if (r != ConnectionResult.SUCCESS) {
                if (GooglePlayServicesUtil.isUserRecoverableError(r)) {
                    GooglePlayServicesUtil.getErrorDialog(r, this, REQUEST_INSTALL_GOOGLEPLAYSERVICE).show();
                } else {
                    Toast.makeText(this, R.string.unsupport_google_drive, Toast.LENGTH_LONG).show();
                    return false ;
                }
            } else {
                String[] scopes = { DriveScopes.DRIVE_APPDATA };
                credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(scopes)) ;
                startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        } else {
            restoreFileFromDrive();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_INSTALL_GOOGLEPLAYSERVICE:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.must_install_google_play, Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        googleDriveService = getDriveService(credential) ;
                        if (backup_or_restore == REQUEST_FOR_BACKUP_GOOGLE_DRIVE)
                            saveFileToDrive();
                        else if (backup_or_restore == REQUEST_FOR_RESTORE_GOOGLE_DRIVE) {
                            restoreFileFromDrive();
                        }
                    }
                }
                break;
            case REQUEST_AUTHORIZATION :
                if (resultCode == RESULT_OK) {
                    if (backup_or_restore == REQUEST_FOR_BACKUP_GOOGLE_DRIVE)
                        saveFileToDrive();
                    else if (backup_or_restore == REQUEST_FOR_RESTORE_GOOGLE_DRIVE)
                        restoreFileFromDrive();
                } else {
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }
                break;
        }
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build() ;
    }

    private void saveFileToDrive() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    java.io.File fileContent = new java.io.File(dbfileUri.getPath());
                    FileContent mediaContent = new FileContent(null, fileContent) ;

                    // File's metadata.
                    File body = new File();
                    body.setTitle(fileContent.getName());
                    body.setDescription("Shot Recorder Database file");
                    body.setMimeType("");
                    body.setParents(Arrays.asList(new ParentReference().setId("appdata")));

                    File file = googleDriveService.files().insert(body, mediaContent).execute();
                    if (file != null) {
                        ShowToast("Backup successful.");
                    }
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        t.start();
    }

    private void restoreFileFromDrive() {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dbfile = getFileFromDrive();
                    if (dbfile != null) {
                        java.io.File tofile = getDatabasePath(ShotRecordProvider.DATABASE_NAME) ;
                        downloadFileFromDrive(dbfile, new FileOutputStream(tofile));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshData();
                            }
                        });
                    }
                }catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }) ;

        t.start();
    }

    private void refreshData() {
        LoaderManager lm = getSupportLoaderManager() ;
        lm.restartLoader(1, null, this) ;
    }

    private com.google.api.services.drive.model.File
    getFileFromDrive() {

        List<File> result = new ArrayList<File>();
        Drive.Files.List request ;
        try {
            request = googleDriveService.files().list();
        } catch (IOException e) {
            e.printStackTrace();
            return  null;
        }

        request.setQ("'appdata' in parents");

        File dbFile = null ;

        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getItems());

                for(File f : result) {
                    if (f.getTitle().equals(ShotRecordProvider.DATABASE_NAME)) {
                        // we find it
                        dbFile = f ;
                        break;
                    }
                }
                result.clear();

                if (dbFile != null) {
                    request.setPageToken(null) ;
                    break;
                }
                request.setPageToken(files.getNextPageToken()) ;
            } catch (IOException e) {
                e.printStackTrace();
                request.setPageToken(null) ;
            }

        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0) ;


        return dbFile ;
    }

    private void downloadFileFromDrive(
            com.google.api.services.drive.model.File fromFile, FileOutputStream toFile) throws IOException {
        if (fromFile.getDownloadUrl() != null && fromFile.getDownloadUrl().length() > 0 ) {
            InputStream inputstream = null ;
            try {
                HttpResponse resp =
                        googleDriveService.getRequestFactory().buildGetRequest(
                                new GenericUrl(fromFile.getDownloadUrl())).execute();
                inputstream = resp.getContent() ;

                int read = 0;
                byte[] bytes = new byte[1024];
                while( (read = inputstream.read(bytes)) != -1) {
                    toFile.write(bytes,0,read) ;
                }
            } finally {
                if (inputstream != null) {
                    inputstream.close();
                }
            }
        }
    }

    /*
    private static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {

        FileChannel fromChannel = null ;
        FileChannel toChannel = null ;

        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }
    */

    private void ShowToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
