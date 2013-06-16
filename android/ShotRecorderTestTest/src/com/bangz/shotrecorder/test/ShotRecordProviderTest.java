package com.bangz.shotrecorder.test;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import org.json.JSONArray;

import com.bangz.shotrecorder.ShotRecord;
import com.bangz.shotrecorder.ShotRecordProvider;
import com.bangz.shotrecorder.SplitManager;

import java.util.ArrayList;

/**
 * Created by royer on 11/06/13.
 */
public class ShotRecordProviderTest extends ProviderTestCase2<ShotRecordProvider> {

    private static final Uri INVALID_URI =
            Uri.withAppendedPath(ShotRecord.ShotRecords.CONTENT_URI,"invalid");

    private MockContentResolver mMockResolver ;
    private SQLiteDatabase mDB ;

    private static final OneRecord[] SHOTRECORDS = {
            new OneRecord("2013-05-12 10:32:28","Shot Record 01"),
            new OneRecord("2013-05-12 10:55:12","Shot Record 02"),
            new OneRecord("2013-05-23 12:21:33","Shot Record 03"),
            new OneRecord("2013-06-02 14:22:33","Shot Record 04"),
            new OneRecord("2013-06-03 10:22:43","Shot Record 05"),
            new OneRecord("2013-06-03 13:41:22","Shot Record 06")
    };

    private static final long[][] originsplits = {
            {653,933,1344,1756,2385,2899,3291,3999,4762,5877,6298,7820,8473,9211,11097},
            {542,1872,2346,7833,9208},
            {1299,2382,4377,7833,9834,11256,12866,13835,17592},
            {956,1587,2388,4529,6397,7764},
            {432,1830,2549,2899,3560,5666,6234,7830,8933,9378,10374,12883},
            {3456,6540,8922,9702,12834,13209}
    } ;

    private static final SplitManager[] msplitmanagers = new SplitManager[originsplits.length];

    static {

        for(int i = 0; i < msplitmanagers.length; i++) {
            msplitmanagers[i] = new SplitManager();
            for (int j = 0; j < originsplits[i].length; j++) {
                msplitmanagers[i].append(originsplits[i][j]);
            }

            SHOTRECORDS[i].setSplits(msplitmanagers[i]);
        }
    }


    public ShotRecordProviderTest() {
        super(ShotRecordProvider.class, ShotRecord.AUTHORITY);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mMockResolver = getMockContentResolver();

        mDB = getProvider().getOpenHelperForTest().getWritableDatabase();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * sets up test data.
     * the test data is in a SQL database. It is created in Setup() without any data,
     * and populated in insertData if necessary.
     */
    private void insertData() {

        for (int i = 0; i < SHOTRECORDS.length; i++) {
            mDB.insertOrThrow(
                    ShotRecord.ShotRecords.TABLE_NAME,
                    ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,
                    SHOTRECORDS[i].getContentValues());
        }

    }

    public void testInsert() {
        // create a new shot record
        OneRecord oneRecord = new OneRecord("2013-06-10 14:29:34","Shot Record 30",5,7644,"[765,1348,2399,4645,7644]");

        Uri newUri = mMockResolver.insert(ShotRecord.ShotRecords.CONTENT_URI, oneRecord.getContentValues());

        long rowid = ContentUris.parseId(newUri);

        Cursor c = mMockResolver.query(ShotRecord.ShotRecords.CONTENT_URI,null,null,null,null);

        assertEquals(1, c.getCount());

        assertTrue(c.moveToFirst()) ;

        int dateindex = c.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_DATE);
        int descriptionindex = c.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION);
        int shotsindex = c.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SHOTS);
        int spendtimeindex = c.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME);
        int splitsindex = c.getColumnIndex(ShotRecord.ShotRecords.COLUMN_NAME_SPLITS);

        assertEquals(oneRecord.DATE, c.getString(dateindex));
        assertEquals(oneRecord.DESCRIPTION,c.getString(descriptionindex));
        assertEquals(oneRecord.SHOTS, c.getInt(shotsindex));
        assertEquals(oneRecord.SPENDTIME, c.getLong(spendtimeindex));
        assertEquals(oneRecord.SPLITS,c.getString(splitsindex));

        ContentValues values = oneRecord.getContentValues();
        values.put(ShotRecord.ShotRecords._ID, (long)rowid);

        try {
            Uri rowUri = mMockResolver.insert(ShotRecord.ShotRecords.CONTENT_URI, values);
            fail("Expected insert failure for existing record but insert succeeded.");
        }catch (Exception e) {

        }
    }

    public void testQuery() {
    	
    	final String[] TEST_PROJECTION = {
    			ShotRecord.ShotRecords.COLUMN_NAME_DATE,
    			ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,
    			ShotRecord.ShotRecords.COLUMN_NAME_SPLITS
    	};
    	
    	final String DATE_SELECTION = ShotRecord.ShotRecords.COLUMN_NAME_DATE + " = " + "?";
    	final String SELECTION_COLUMNS = 
    			DATE_SELECTION + " OR " + DATE_SELECTION + " OR " + DATE_SELECTION ;
    	final String[] SELECTION_ARGS = { "2013-05-12 10:32:28","2013-05-12 10:55:12","2013-05-23 12:21:33" };
    	
    	Cursor c = mMockResolver.query(ShotRecord.ShotRecords.CONTENT_URI, 
    			TEST_PROJECTION, SELECTION_COLUMNS, SELECTION_ARGS, null);
    	assertEquals(0, c.getCount());
    	
    	insertData();
    	c = mMockResolver.query(ShotRecord.ShotRecords.CONTENT_URI, null, null, null, null);
    	assertEquals(SHOTRECORDS.length, c.getCount());
    	
    			
    }
    private static class OneRecord {
        String DATE ;
        String DESCRIPTION ;
        int    SHOTS;
        long   SPENDTIME ;
        String SPLITS ;

        public OneRecord(String sdate, String sDescription, int shots, int spendtime, String sSplits) {
            DATE = sdate;
            DESCRIPTION = sDescription;
            SHOTS = shots;
            SPENDTIME = spendtime;
            SPLITS = sSplits;
        }

        public OneRecord(String sdate,String sDescription) {
            DATE = sdate;
            DESCRIPTION = sDescription ;
        }

        public void setSplits(SplitManager splits) {
            JSONArray jsonArray = new JSONArray();

            SHOTS = splits.getNumbers() ;

            for(int i = 0; i < SHOTS; i++) {
                jsonArray.put(splits.getSplits().get(i).getTime());
                if (i == SHOTS - 1)
                    SPENDTIME = splits.getSplits().get(i).getTime();
            }

            SPLITS = jsonArray.toString();
        }


        public ContentValues getContentValues() {

            ContentValues v = new ContentValues();

            v.put(ShotRecord.ShotRecords.COLUMN_NAME_DATE, DATE);
            v.put(ShotRecord.ShotRecords.COLUMN_NAME_DESCRIPTION,DESCRIPTION);
            v.put(ShotRecord.ShotRecords.COLUMN_NAME_SHOTS, SHOTS);
            v.put(ShotRecord.ShotRecords.COLUMN_NAME_SPENDTIME, SPENDTIME);
            v.put(ShotRecord.ShotRecords.COLUMN_NAME_SPLITS, SPLITS);
            return v;
        }
    }
}
