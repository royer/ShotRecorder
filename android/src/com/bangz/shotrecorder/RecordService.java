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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by royer on 19/05/13.
 *
 * RecordService
 *
 * <p><b>Brief:</b>
 * RecordService use {@link AudioRecord } to record sound and detect shot by shotdetector class </p>
 *
 * This Service is a private Service
 */
public class RecordService extends Service {

    private static final String TAG = "RecordService" ;

    private static final String EXTRA_PREFIX = "com.bangz.RecordService." ;

    public static final String EXTRA_MODE = EXTRA_PREFIX + "MODE" ;

    public static final int MODE_COMSTOCK = 0;
    public static final int MODE_VIRGINIA = 1;
    public static final int MODE_PARTIME = 2;

    private int mMode ;

    public static final String EXTRA_MAXSHOT = EXTRA_PREFIX + "MAXSHOT" ;
    private int mMaxShot ;

    public static final String EXTRA_MAXPARTIME = EXTRA_PREFIX + "MAXPARTIME" ;
    private int mParTime ; // millisecond.

    public static final String EXTRA_SAMPLERATE = EXTRA_PREFIX + "SAMPLERATE" ;
    public static final String EXTRA_CHANNLES = EXTRA_PREFIX + "CHANNELS" ;
    public static final String EXTRA_ENCODDING = EXTRA_PREFIX + "ENCODING" ;

    public static final String EXTRA_CAPTURESIZE = EXTRA_PREFIX + "CAPTURE_SIZE" ;
    /** send waveform capture size. if 0 do not send capture data. the value must be a power of 2 SampleRate
     * and cann't large than the return value of {@link android.media.AudioRecord#getMinBufferSize } */
    private int mCaptureSize ;

    public static final String EXTRA_MAXRECORDTIME = EXTRA_PREFIX + "MAX_RECORD_TIME" ;
    public static final int DEFAULT_MAX_RECORD_TIME = 5 * 60 ;
    /** the max record time. second. default is 5 minutes. */
    private int mMaxRecordTime = 5 * 60 ;


    private NotificationManager mNM ;


    private AudioRecord recorder = null ;
    private RecordThread recordThread = null ;
    private boolean isRecord = false ;

    private int mSampleRate = 44100 ;
    private int mChannels = AudioFormat.CHANNEL_IN_MONO ;
    private int mEncoding = AudioFormat.ENCODING_PCM_16BIT ;

    private int mRecordBufferSize = 0;

    ArrayList<Long> mShotEvents = new ArrayList<Long>();


    /** total record samples in bytes */
    private long mSamples = 0;

    /** the bind client Messenger , because we only have one client */
    private Messenger mClient ;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;


    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;


    /** command to the service to stop record.   */
    static final int MSG_STOP_RECORD = 3 ;



    /** command to the service to report current status.
     *  service answer this message to client with arg0 for detail.
     *  the arg0 can be {@link #STATUS_RECORDING} or {@link #STATUS_STOPPED}.
     */
    static final int MSG_RECORD_STATUS = 4;

    /**
     * current status is waitting for start record. the arg0 param of message {@link #MSG_RECORD_STATUS}
     * this status is only happen the service create, but not start AudioRecord. client usually does not get
     * this status when they bind.
     */
    static final int STATUS_PREPARE = 0 ;

    /**
     * current status is recording. the arg0 param of message {@link #MSG_RECORD_STATUS}.
     *
     */
    static final int STATUS_RECORDING = 1 ;

    /**
     *  current status is stoped. the arg0 param of message {@link #MSG_RECORD_STATUS}.
     *  the arg1 is the stopped reason. it must one of these
     *  {@link #STOPPED_STARTFAILED}
     *  {@link #STOPPED_MAXSIZE} {@link #STOPPED_MAXSHOT} {@link #STOPPED_MAXPARTIME}
     *  {@link #STOPPED_NOT_ENOUGH_SPACE} {@link #STOPPED_MAX_RECORDING_TIME}
     */
    static final int STATUS_STOPPED = 2 ;

    /**
     * stopped because init AudioRecord failed.
     * <p>the arg1 with {@link #MSG_RECORD_STATUS}.arg0 is {@link #STATUS_STOPPED} .</p>
     */
    static final int STOPPED_STARTFAILED = 1 ;
    /** stopped because reach the max file size setting value */
    static final int STOPPED_MAXSIZE = 2 ;
    /** stopped because reach the max shot time detected. it's for viginia mode. */
    static final int STOPPED_MAXSHOT = 3 ;
    /** stopped because reach the max par time setting value. it's for par time mode. */
    static final int STOPPED_MAXPARTIME = 4 ;
    /** stopped because not enough storge space to save wave file." */
    static final int STOPPED_NOT_ENOUGH_SPACE = 5;
    /** stopped because the max recording time reached. */
    static final int STOPPED_MAX_RECORDING_TIME = 6;
    /** Hardware error when recording */
    static final int STOPPED_INVALID_OPERATION = -3 ;


    /** command to service to send all laps to client; service return all laps to client
     *  use the <b>obj</b> param. it is a long[], each value is the shot elapsed time
     *  with the start record in millisecond.
     *  <p></p>
     *  arg1:   -1; service set -1 to notify client there have new event arrived. If set >= 0
     *              means this message obj events which the first event number(it is array
     *              offset start 0).
     *
     */
    static final int MSG_LAPS = 5;
    static final int EVENT_ARRIVED = -1;

    /**
     * command service to send client captured waveform data. the arg0 is capture size {@link #mCaptureSize}
     *
     *
     * service answer:
     * <p>
     *       arg1:  the wave samplerate
     *       arg2:  loword is Channels, hiword indicate encoding is PCM8 or PCM16
     *       obj:   ByteBuffer.</p>
     */
    static final int MSG_WAVEFORMDATA = 6 ;

    /** notify client current position.
     * <p>with this message arg0 is current position(millisecond)</p>
     */
    static final int MSG_POSITION = 7;


    private int mStatus = STATUS_PREPARE ;
    private int mStoppedReason = 0;

    /**
     * Handler of incoming messages from clients ;
     */
    class IncomingHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_REGISTER_CLIENT :
                    onClientRegister(msg.replyTo) ;
                    break ;
                case MSG_UNREGISTER_CLIENT :
                    onClientUnRegister(msg.replyTo) ;
                    break;
                case MSG_STOP_RECORD:
                    //TODO stop record
                    break;
                case MSG_LAPS:
                    onHandleMsgLaps(msg);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandle());

    @Override
    public void onCreate() {
        super.onCreate();

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);



    }


    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        if (mStatus != this.STATUS_PREPARE) {
            stopSelf(startId);
            Log.d(TAG, "stopSelf. because mStatus != STATUS_PREPARE. startId = " + startId) ;
            return START_NOT_STICKY ;
        }

        Init(intent) ;

        startRecord();

        Notification notification = new Notification(R.drawable.ic_launcher, getText(R.string.serviceisrunning),
                System.currentTimeMillis());

        Intent notificationIntent = new Intent(this, RecordActivity.class) ;

        notificationIntent.putExtra(RecordActivity.EXTRA_STARTFROMNOTIFY,true) ;
        notificationIntent.putExtra(EXTRA_MODE, mMode) ;
        Log.d(TAG, "put mMode = " + mMode + " in service side.") ;
        notificationIntent.putExtra(EXTRA_MAXSHOT,mMaxShot) ;
        notificationIntent.putExtra(EXTRA_MAXPARTIME, mParTime);
        notificationIntent.putExtra(EXTRA_SAMPLERATE, mSampleRate);
        notificationIntent.putExtra(EXTRA_CHANNLES,mChannels);
        notificationIntent.putExtra(EXTRA_ENCODDING, mEncoding) ;
        notificationIntent.putExtra(EXTRA_CAPTURESIZE, mCaptureSize);
        notificationIntent.putExtra(EXTRA_MAXRECORDTIME, mMaxRecordTime);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(this, getText(R.string.str_notify_title), getText(R.string.serviceisrunning),
                pendingIntent);

        //mNM.notify(R.string.serviceisrunning,notification);
        startForeground(R.string.serviceisrunning, notification);


        return START_STICKY ;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy") ;
        stopRecord();

        stopForeground(true);
        //mNM.cancel(R.string.serviceisrunning);
    }


    private void Init(Intent intent) {

        mMode = intent.getIntExtra(EXTRA_MODE, MODE_COMSTOCK) ;

        mMaxShot = intent.getIntExtra(EXTRA_MAXSHOT, 0) ;
        mParTime = intent.getIntExtra(EXTRA_MAXPARTIME, 0) ;

        mSampleRate = intent.getIntExtra(EXTRA_SAMPLERATE, 44100) ;
        mChannels = intent.getIntExtra(EXTRA_CHANNLES,AudioFormat.CHANNEL_IN_MONO) ;
        mEncoding = intent.getIntExtra(EXTRA_ENCODDING, AudioFormat.ENCODING_PCM_16BIT) ;

        mCaptureSize = intent.getIntExtra(EXTRA_CAPTURESIZE, 0) ;
        mMaxRecordTime = intent.getIntExtra(EXTRA_MAXRECORDTIME, DEFAULT_MAX_RECORD_TIME) ;

    }


    private void onClientRegister(Messenger client) {
        synchronized (this) {
            mClient = client ;
        }
    }

    private void onClientUnRegister(Messenger client) {
        synchronized (this) {
            if ((mClient != client)) {
                throw new AssertionError();
            }
            mClient = null ;
        }
    }

    synchronized private void onHandleMsgLaps(Message msg) {

        if (msg.arg1 >= 0 && msg.arg1 < mShotEvents.size()) {
            long[] events = new long[mShotEvents.size() - msg.arg1 ] ;

            for(int i = 0,j = msg.arg1; i < events.length; i++,j++) {
                events[i] = mShotEvents.get(j) ;
            }
            try {
                if (mClient != null)
                    mClient.send(Message.obtain(null, MSG_LAPS, msg.arg1, 0, events));
            } catch (RemoteException e) {
                e.printStackTrace();
                mClient = null;
            }
        }
    }

    private void startRecord() {

        mRecordBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannels, mEncoding) ;

        if (mRecordBufferSize == AudioRecord.ERROR_BAD_VALUE || mRecordBufferSize == AudioRecord.ERROR) {
            mStatus = STATUS_STOPPED ;
            mStoppedReason = STOPPED_STARTFAILED ;
            return ;
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, mChannels, mEncoding, mRecordBufferSize);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            recorder.release();
            recorder = null ;

            mStatus = STATUS_STOPPED;
            mStoppedReason = STOPPED_STARTFAILED ;
            return ;
        }

        recorder.startRecording();
        isRecord = true ;

        recordThread = new RecordThread();
        recordThread.start();

    }

    synchronized private void stopRecord() {

        isRecord = false ;
        recordThread = null ;

        mStatus = STATUS_STOPPED ;
    }


    synchronized private void updateStatus(int status, int statusparam) {
        mStatus = status ;
        if (mStatus == STATUS_RECORDING) {

        } else if (mStatus == STATUS_STOPPED) {
            mStoppedReason = statusparam ;
            isRecord = false ;
        }

        if (mClient != null) {
            try {

                mClient.send(Message.obtain(null,MSG_RECORD_STATUS,status,statusparam));

            } catch (RemoteException e) {

                //TODO should we notify user when record stopped ?

                e.printStackTrace();
                mClient = null ;
            }
        }
    }

    synchronized private void NotifyCurrentPostion(int position) {

        if (mClient != null) {
            try {

                mClient.send(Message.obtain(null, MSG_POSITION, position,0));

            } catch (RemoteException e) {

                e.printStackTrace();
                mClient = null;
            }
        }

    }

    synchronized private void AddNewEvents(Long[] events) {

        mShotEvents.addAll(Arrays.asList(events));

        if (mClient != null) {
            try {
                // arg0 = EVENT_ARRIVED(-1), is just notify client that has new event arrived.
                mClient.send(Message.obtain(null, MSG_LAPS,EVENT_ARRIVED, 0));

            } catch (RemoteException e) {
                e.printStackTrace();
                mClient = null ;
            }
        }
    }

    class RecordThread extends Thread {
        @Override
        public void run() {

            ByteBuffer  readbuffer = ByteBuffer.allocateDirect(mRecordBufferSize) ;

            long lastreportpostion = 0 ;

            int channels = (mChannels == AudioFormat.CHANNEL_IN_MONO)?1:2 ;

            float samplespermillisecond =
                    (float)mSampleRate * channels * ((mEncoding == AudioFormat.ENCODING_PCM_16BIT)?2:1) / 1000 ;

            int reportpostionsamples = (int)(50 * samplespermillisecond) ;

            //TODO ThresholdDB should read from setting.
            int thresholdDB = Prefs.getThresholdDB(RecordService.this);
            AmplitudeShotDetectAlgorithms asda = new AmplitudeShotDetectAlgorithms(thresholdDB-96);
            Log.d(TAG,"Threshold dB = "+ thresholdDB) ;

            ShotDetector detector = new ShotDetector(mSampleRate,channels,asda,mRecordBufferSize / channels + 1) ;

            while(true) {

                int read = recorder.read(readbuffer, readbuffer.remaining()) ;

                if (read < 0) {
                    updateStatus(STATUS_STOPPED, STOPPED_INVALID_OPERATION) ;
                    break ;
                }

                mSamples += read ;
                readbuffer.position(0) ;
                readbuffer.limit(read) ;

                //TODO: save to temp file.

                Long[] shotevent = detector.doDetect(readbuffer, mSampleRate, channels, mEncoding);
                Long[] acturycopy = null;

                if (shotevent.length > 0) {
                    int needadd = 0;
                    if (mMode == MODE_VIRGINIA) {
                        needadd = Math.min(shotevent.length, mMaxShot - mShotEvents.size());
                    } else {
                        needadd = shotevent.length;
                    }
                    acturycopy = new Long[needadd] ;
                    System.arraycopy(shotevent,0,acturycopy,0,needadd);
                    AddNewEvents(acturycopy) ;
                }

                if ((mSamples - lastreportpostion) > reportpostionsamples) {
                    //report current postion(millisecond)
                    NotifyCurrentPostion((int)(mSamples / samplespermillisecond)) ;
                    lastreportpostion = mSamples ;
                }

                if (mMode == MODE_VIRGINIA && mShotEvents.size() == mMaxShot) {
                    updateStatus(STATUS_STOPPED,STOPPED_MAXSHOT);
                    break;
                }

                if (mMode == MODE_PARTIME && mSamples >= mParTime * samplespermillisecond) {
                    updateStatus(STATUS_STOPPED, STOPPED_MAXPARTIME) ;
                    break;
                }

                if (mSamples >= mMaxRecordTime * 1000 * samplespermillisecond) {
                    updateStatus(STATUS_STOPPED, STOPPED_MAX_RECORDING_TIME) ;
                    break;
                }

                readbuffer.clear() ;

                synchronized (RecordService.this) {
                    if (isRecord == false) {
                        break;
                    }
                }
            }

            recorder.stop();
            recorder.release();
            recorder = null ;
        }
    }
}
