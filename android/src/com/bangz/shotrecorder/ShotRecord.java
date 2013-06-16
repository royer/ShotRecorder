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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by royer on 07/06/13.
 *
 * Defines a contract between the Shot Recorder content provider and its client.
 */
public final class ShotRecord {

    public static final String AUTHORITY = "com.bangz.provider.ShotRecord";

    private ShotRecord() {}


    /**
     * Shot Record table contract
     */
    public static final class ShotRecords implements BaseColumns {
        private ShotRecords() {}

        /**
         * The Table Name  offered by this provider
         */
        public static final String TABLE_NAME = "shotrecords";

        // URI definitions

        /**
         * The scheme part
         */
        private static final String SCHEME = "content://" ;

        // path part for the URI
        private static final String PATH_SHOTRECORDS = "/shotrecords" ;

        // path part of Records ID URI
        private static final String PATH_RECORD_ID = "/shotrecrods/" ;

        /**
         * 0-relative position of a record ID segment in the path part of a shotrecord ID URI
         */
        public static final int SHOTRECORD_ID_PATH_POSITION = 1 ;

        /**
         * the content:// style of URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_SHOTRECORDS) ;

        /**
         * The content URI base for a single shot record. Callers must append a numeric record id
         * to this Uri to retrieve a record
         */
        public static final Uri CONTENT_ID_URI_BASE =
                Uri.parse(SCHEME + AUTHORITY + PATH_RECORD_ID);

        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTEN
                = Uri.parse(SCHEME + AUTHORITY + PATH_RECORD_ID + "/#") ;


        /*
         * Columns definitions
         */

        /**
         * column name for the DATE of the shot record
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_DATE = "date" ;


        /**
         * column name for the description
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_DESCRIPTION = "description" ;

        /**
         * column name for the shots
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SHOTS = "shots" ;

        /**
         * column name for the time. long of millisecond
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SPENDTIME = "spendtime";

        /**
         * column name for each shot time. JSON array type
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_SPLITS = "splits" ;

    }
}
