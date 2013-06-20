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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


/**
 * Created by royer on 07/06/13.
 */
public class ShotRecordProvider extends ContentProvider {
    private static final String TAG = "ShotRecordProvider" ;

    private static final String DATABASE_NAME = "shotrecord.db";
    private static final int DATABASE_VERSION = 1 ;

    private DatabaseHelper mOpenHelper ;

    private static final String[] ALL_COLUMN_PROJECTION = new String[] {
            ShotRecord.ShotRecords._ID,
            ShotRecord.ShotRecords.COLUMN_NAME_DATE,
            ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,
            ShotRecord.ShotRecords.COLUMN_NAME_SHOTS,
            ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME,
            ShotRecord.ShotRecords.COLUMN_NAME_SPLITS,
    };
    private static final int INDEX_ID = 0;
    private static final int INDEX_DATE = 1;
    private static final int INDEX_DESCRIPTION = 2;
    private static final int INDEX_SHOTS = 3;
    private static final int INDEX_SPENDTIME = 4;
    private static final int INDEX_SPLITS = 5;


    private static final UriMatcher sUriMatcher ;

    /** The incoming URI matches the Table of all Records URI pattern */
    private static final int MATCH_RECORDS = 1;

    /** The incoming URI matches the one shot record URI pattern */
    private static final int MATCH_ONE_RECORD = 2;


    // Creates a new projection map instance. The map returns a column name
    // given a string. The two are usually equal.
    private static HashMap<String, String> sProjectionMap ;

    static {

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ShotRecord.AUTHORITY,ShotRecord.ShotRecords.TABLE_NAME,MATCH_RECORDS);
        sUriMatcher.addURI(ShotRecord.AUTHORITY,ShotRecord.ShotRecords.TABLE_NAME+"/#", MATCH_ONE_RECORD);

        sProjectionMap = new HashMap<String, String>();
        sProjectionMap.put(ShotRecord.ShotRecords._ID, ShotRecord.ShotRecords._ID);
        sProjectionMap.put(ShotRecord.ShotRecords.COLUMN_NAME_DATE,ShotRecord.ShotRecords.COLUMN_NAME_DATE);
        sProjectionMap.put(ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION);
        sProjectionMap.put(ShotRecord.ShotRecords.COLUMN_NAME_SHOTS,ShotRecord.ShotRecords.COLUMN_NAME_SHOTS);
        sProjectionMap.put(ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME,ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME);
        sProjectionMap.put(ShotRecord.ShotRecords.COLUMN_NAME_SPLITS,ShotRecord.ShotRecords.COLUMN_NAME_SPLITS);

    }


    @Override
    public boolean onCreate() {

        mOpenHelper = new DatabaseHelper(getContext());

        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(ShotRecord.ShotRecords.TABLE_NAME);

        // Choose the projection and adjust the "where" clause based on URI pattern matching.
        switch(sUriMatcher.match(uri)) {
            case MATCH_RECORDS:
                qb.setProjectionMap(sProjectionMap);
                break;
            case MATCH_ONE_RECORD:
                qb.setProjectionMap(sProjectionMap);
                qb.appendWhere(ShotRecord.ShotRecords._ID + "=" +
                    uri.getPathSegments().get(ShotRecord.ShotRecords.SHOTRECORD_ID_PATH_POSITION));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase() ;

        Cursor cursor = qb.query(db,projection,selection, selectionArgs,null,null,sortOrder);

        // Tells the cursor what URI to watch, so it knows when its source data changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {

        switch(sUriMatcher.match(uri)) {
            case MATCH_RECORDS:
                return ShotRecord.ShotRecords.CONTENT_TYPE ;
            case MATCH_ONE_RECORD:
                return ShotRecord.ShotRecords.CONTENT_ITEM_TYPE ;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        // Validates the incoming URI. Only the full provider URI is allowed for inserts.
        if (sUriMatcher.match(uri) != MATCH_RECORDS) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        //Get current system time
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        if (!values.containsKey(ShotRecord.ShotRecords.COLUMN_NAME_DATE)) {
            values.put(ShotRecord.ShotRecords.COLUMN_NAME_DATE,df.format(date));
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowid = db.insert(ShotRecord.ShotRecords.TABLE_NAME,ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,values);

        if (rowid > 0) {
            Uri recorduri = ContentUris.withAppendedId(uri, rowid);
            getContext().getContentResolver().notifyChange(recorduri, null);

            return recorduri ;
        }
        throw new IllegalArgumentException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int count ;

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String where ;

        switch (sUriMatcher.match(uri)) {
            case MATCH_RECORDS:
                count = db.delete(ShotRecord.ShotRecords.TABLE_NAME, selection, selectionArgs);
                break;
            case MATCH_ONE_RECORD:
                where = ShotRecord.ShotRecords._ID + " = " +
                        uri.getPathSegments().get(ShotRecord.ShotRecords.SHOTRECORD_ID_PATH_POSITION);
                if (selection != null) {
                    where = where + " AND " + selection ;
                }
                count = db.delete(ShotRecord.ShotRecords.TABLE_NAME, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int count ;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String where ;

        switch (sUriMatcher.match(uri)) {
            case MATCH_RECORDS:
                count = db.update(ShotRecord.ShotRecords.TABLE_NAME,values, selection, selectionArgs);
                break;
            case MATCH_ONE_RECORD:
                where = ShotRecord.ShotRecords._ID + " = " +
                        uri.getPathSegments().get(ShotRecord.ShotRecords.SHOTRECORD_ID_PATH_POSITION);
                if (selection != null) {
                    where = where + " AND " +selection ;
                }
                count = db.update(ShotRecord.ShotRecords.TABLE_NAME, values,where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }


        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    public DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper ;
    }
    public static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String TAG = DatabaseHelper.class.getName();


        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, // no cursor factory
                    DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE "
                    + ShotRecord.ShotRecords.TABLE_NAME + " ("
                    + ShotRecord.ShotRecords._ID + " INTEGER PRIMARY KEY, "
                    + ShotRecord.ShotRecords.COLUMN_NAME_DATE + " TEXT, "
                    + ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION + " TEXT, "
                    + ShotRecord.ShotRecords.COLUMN_NAME_SHOTS + " INTEGER, "
                    + ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME + " INTEGER, "
                    + ShotRecord.ShotRecords.COLUMN_NAME_SPLITS + " TEXT"
                    + " );");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // template for add column for upgrade
//            try {
//                db.query(ShotRecord.ShotRecords.TABLE_NAME,
//                        new String[] {ShotRecord.ShotRecords.COLUMN_NAME_SPLITS},
//                        null, null, null, null,null);
//            } catch (SQLException e) {
//                // column does not exist, add it!
//                Log.e(TAG, "Database upgrade - add column splits", e) ;
//                try {
//                    db.execSQL("ALTER TABLE " + ShotRecord.ShotRecords.TABLE_NAME +
//                        " ADD COLUMN " + ShotRecord.ShotRecords.COLUMN_NAME_SPLITS + " TEXT DEFAULT NULL;");
//                } catch (SQLException ex) {
//                    Log.e(TAG,"",ex);
//                }
//            }
        }
    }
}
