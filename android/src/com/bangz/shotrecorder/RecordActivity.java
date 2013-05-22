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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.os.*;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class RecordActivity extends SherlockFragmentActivity implements View.OnClickListener {
	
	public static final String TAG = "RecorderActivity";

    public static final String EXTRA_STARTFROMNOTIFY = "START_FROM_NOTIFY";
	
	public static final int	STATE_NORMAL 	= 0 ;
	public static final int STATE_PREPARE	= 1;
	public static final int STATE_RECORDING = 2 ;


	private int			mState ;




    public static enum MODE { COMSTOCK, VIRGINIA, PAR_TIME }

	private MODE		mMode ;

    private int mCaptureSize = 128 ;
    private int mMaxRecordTime = 5 * 60 ;


    /** Messenger for communicating with service */
    Messenger mService = null ;
    /** Flag indicating whether we have called bind on the Service. */
    boolean mIsBound ;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
                case RecordService.MSG_RECORD_STATUS:
                    UpdateRecordStatus(msg) ;
                case RecordService.MSG_POSITION:
                    UpdateCurrentPosition(msg) ;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    /**
     * Target we publish for client to send messages to InComingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler()) ;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mService = new Messenger(service) ;

            doRegisterClient();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mService = null;
            //TODO should we do clearn all status when service disconnect ?

        }
    };

    private void doRegisterClient() {

        Message msg = Message.obtain(null, RecordService.MSG_REGISTER_CLIENT);
        msg.replyTo = mMessenger ;

        try {
            mService.send(msg) ;
        } catch (RemoteException e) {
        }
    }


    private void doBindService() {

        bindService(new Intent(RecordActivity.this,RecordService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true ;
    }

    private void doUnbindService() {

        if (mIsBound) {

            if (mService != null) {

                try {
                Message msg = Message.obtain(null,RecordService.MSG_UNREGISTER_CLIENT) ;
                msg.replyTo = mMessenger ;
                mService.send(msg) ;
                } catch(RemoteException e) {

                }
            }

            unbindService(mConnection);

            mService = null ;
            mIsBound = false;
        }
    }



    private void UpdateRecordStatus(Message msg) {
        if(msg.arg1 == RecordService.STATUS_RECORDING) {



        } else if (msg.arg1 == RecordService.STATUS_STOPPED ){

            mState = STATE_NORMAL ;

            if(mIsBound) {

                doStopRecord();
                updateStatus();
            }
        }
    }

    private void UpdateCurrentPosition(Message msg) {

        String strText = String.format("%.02f",(float)msg.arg1 / 1000) ;

        TextView txtElapsedTime = (TextView)findViewById(R.id.textElapsedTime) ;
        txtElapsedTime.setText(strText) ;

    }


    /**
	 * the Max.Shots setting value for virginia mode
	 */
	private int			mMaxShots;
	
	/**
	 * the Max.Time setting value for PAR_TIME mode ;
	 */
	private float mMaxParTime;
	
	private int mSampleRate = 44100 ;
    private int mChannels = AudioFormat.CHANNEL_IN_MONO ;
    private int mEncoding = AudioFormat.ENCODING_PCM_16BIT ;

    private String[]	strModeNames ;
	private String[]	strModeValNames ;
	
	SplitManager		mSplitManager = new SplitManager();
	SplitArrayAdapter	mSplitAdapter ;

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
		
		
		

		FragmentManager fm = getSupportFragmentManager() ;
		SplitListFragment splitfragment = (SplitListFragment)fm.findFragmentById(R.id.splitlist) ;
		
		mSplitAdapter = new SplitArrayAdapter(this, mSplitManager.getSplits()) ;
		
		
		
		splitfragment.setListAdapter(mSplitAdapter) ;

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.getBooleanExtra(EXTRA_STARTFROMNOTIFY, false)) {
                mState = STATE_RECORDING ;

                int mode = intent.getIntExtra(RecordService.EXTRA_MODE, 0) ;
                Log.d(TAG, "get mode in RecordActivity from service mode = " + mode);
                mMode = MODE.values()[mode] ;

                mSampleRate = intent.getIntExtra(RecordService.EXTRA_SAMPLERATE, 44100);
                mChannels = intent.getIntExtra(RecordService.EXTRA_CHANNLES,AudioFormat.CHANNEL_IN_MONO);
                mEncoding = intent.getIntExtra(RecordService.EXTRA_ENCODDING, AudioFormat.ENCODING_PCM_16BIT);

                mMaxShots = intent.getIntExtra(RecordService.EXTRA_MAXSHOT, 0);
                mMaxParTime = intent.getIntExtra(RecordService.EXTRA_MAXPARTIME, 0) / 1000.0f;

                mCaptureSize = intent.getIntExtra(RecordService.EXTRA_CAPTURESIZE, 0) ;
                mMaxRecordTime = intent.getIntExtra(RecordService.EXTRA_MAXRECORDTIME, 5 * 60) ;
            }

        }

        updateMode();
        updateStatus();

	}


    @Override
    protected void onStart() {
        super.onStart();

        if (mIsBound == false && mState == STATE_RECORDING) {
            doBindService();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mIsBound)
            doUnbindService();
    }

    private void initButtonsListener() {
		
		View.OnClickListener modebuttonlsn = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (mState != STATE_NORMAL) 
					return ;
				
				switch(v.getId()) {
				
				case R.id.btnMode:
					
					mMode = MODE.values()[(mMode.ordinal() + 1) % MODE.values().length] ;
					break ;
				case R.id.btnAdd:
					if (mMode == MODE.VIRGINIA ) {
						mMaxShots++ ;
					} else if (mMode == MODE.PAR_TIME) {
						mMaxParTime++ ;
					}
					break;
				case R.id.btnAddTenth:
					if (mMode == MODE.PAR_TIME) {
						mMaxParTime += 0.1 ;
					}
					break;
				case R.id.btnSub :
					if (mMode == MODE.VIRGINIA) {
						if (mMaxShots > 0)
							mMaxShots--;
					} else if (mMode == MODE.PAR_TIME) {
						if (mMaxParTime > 0.0)
							mMaxParTime-- ;
					}
					break ;
				case R.id.btnSubtenth:
					if (mMode == MODE.PAR_TIME) {
						if (mMaxParTime > 0.0) {
							mMaxParTime -= 0.1 ;
						}
					}
					break;
				case R.id.btnReset :
					if (mMode == MODE.VIRGINIA) {
						mMaxShots = 0;
					} else if (mMode == MODE.PAR_TIME) {
						mMaxParTime = 0;
					}
				}
				updateMode();
			}
		};
		
		Button btnmode = (Button)findViewById(R.id.btnMode) ;
		btnmode.setOnClickListener(modebuttonlsn) ;
		
		btnmode = (Button)findViewById(R.id.btnAdd) ;
		btnmode.setOnClickListener(modebuttonlsn);
		
		btnmode = (Button)findViewById(R.id.btnAddTenth);
		btnmode.setOnClickListener(modebuttonlsn);
		
		btnmode = (Button)findViewById(R.id.btnReset);
		btnmode.setOnClickListener(modebuttonlsn);
		
		btnmode = (Button)findViewById(R.id.btnSub);
		btnmode.setOnClickListener(modebuttonlsn);
		
		btnmode = (Button)findViewById(R.id.btnSubtenth) ;
		btnmode.setOnClickListener(modebuttonlsn) ;

        btnStart = (Button)findViewById(R.id.btnStart) ;
        btnStart.setOnClickListener(this);
	}

    @Override
    public void onClick(View v) {

        if(R.id.btnStart == v.getId()) {
            onStartButtonClick();
        }
    }

    private void onStartButtonClick() {

        if (mState == STATE_NORMAL) {
            doStartRecord();
        } else if (mState == STATE_RECORDING) {
            doStopRecord();
        }

        updateStatus();
    }



    private void doStartRecord() {



        mState = STATE_RECORDING ;
        Intent intent = new Intent(this, RecordService.class) ;

        intent.putExtra(RecordService.EXTRA_SAMPLERATE, mSampleRate) ;
        intent.putExtra(RecordService.EXTRA_CHANNLES, mChannels) ;
        intent.putExtra(RecordService.EXTRA_ENCODDING, mEncoding) ;

        intent.putExtra(RecordService.EXTRA_MODE, mMode.ordinal()) ;
        intent.putExtra(RecordService.EXTRA_MAXSHOT, this.mMaxShots) ;
        intent.putExtra(RecordService.EXTRA_MAXPARTIME, (int)(mMaxParTime * 1000)) ;
        intent.putExtra(RecordService.EXTRA_CAPTURESIZE, 128) ;
        intent.putExtra(RecordService.EXTRA_MAXRECORDTIME, 5 * 60) ;
        startService(intent);

        doBindService();
    }

    private void doStopRecord() {


        mState = STATE_NORMAL ;

        doUnbindService();

        stopService(new Intent(this, RecordService.class)) ;

    }


    private void updateStatus() {

        if (this.mState != STATE_NORMAL) {
            btnStart.setText(getString(R.string.strSTOP)) ;
        } else {
            btnStart.setText(getString(R.string.strSTART)) ;
        }
    }



    private void setDigitFont() {
		
		
		Typeface tfItalic = TypefaceCache.getTypeface(TypefaceCache.FONT_ITALIC_PATH, this) ;
		Typeface tfNormal = TypefaceCache.getTypeface(TypefaceCache.FONT_NORMAL_PATH, this) ;
		
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
	}

	private void updateMode() {
		
		TextView vModeName = (TextView)findViewById(R.id.textmode) ;
		TextView vModeValName = (TextView)findViewById(R.id.textModePrefix) ;
		TextView vModeVal = (TextView)findViewById(R.id.textModeValue);

		vModeName.setText(strModeNames[mMode.ordinal()]);
		
		vModeValName.setText(strModeValNames[mMode.ordinal()]);
		
		if (mMode == MODE.VIRGINIA) {
			vModeVal.setText(String.format("%d", mMaxShots));
		} else if ( mMode == MODE.PAR_TIME ) {
			vModeVal.setText(String.format("%.1f", mMaxParTime)) ;
		}
		
		int visable = (mMode != MODE.COMSTOCK)?View.VISIBLE : View.INVISIBLE ;
		vModeValName.setVisibility(visable);
		vModeVal.setVisibility(visable);
	}

    @Override
    public void onBackPressed() {

        if (mState == STATE_NORMAL)
            super.onBackPressed();
        else {
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.please_stop_record)
                    .setPositiveButton(android.R.string.ok,null)
                    .show();
        }
    }

    private Button  btnStart ;

}
