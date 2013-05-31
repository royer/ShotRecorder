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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.*;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class RecordActivity extends SherlockFragmentActivity implements View.OnClickListener {
	
	public static final String TAG = "RecorderActivity";

    public static final String EXTRA_STARTFROMNOTIFY = "START_FROM_NOTIFY";

    private static final int BEEP_DURATIONMS = 500;
	
	public static final int	STATE_NORMAL 	= 0 ;
	public static final int STATE_PREPARE	= 1;
	public static final int STATE_RECORDING = 2 ;


	private int			mState ;

    private boolean     mbModified = false ;


    private int         mCurrentSplitIndex = -1;

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
                    break;
                case RecordService.MSG_POSITION:
                    UpdateCurrentPosition(msg) ;
                    break;
                case RecordService.MSG_LAPS:
                    OnMessageLaps(msg) ;
                    break;
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
            Message msggetlaps = Message.obtain(null,RecordService.MSG_LAPS,mSplitManager.getNumbers(),0);
            mService.send(msggetlaps);

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

        textElapsed.setText(strText) ;

    }

    private void OnMessageLaps(Message msg) {
        if(msg.arg1 == RecordService.EVENT_ARRIVED) {
            if (mService != null) {
                try {
                    Message msgreturn = Message.obtain(null,RecordService.MSG_LAPS,mSplitManager.getNumbers(),0);
                    msgreturn.replyTo = mMessenger ;
                    mService.send(msgreturn) ;
                } catch(RemoteException e) {

                }
            }
        } else {
            long[] events = (long[])msg.obj ;
            for(long e : events) {
                mSplitManager.append(e);
                mSplitAdapter.notifyDataSetChanged();
                mbModified = true ;
            }

            mCurrentSplitIndex = mSplitManager.getNumbers() - 1;
            UpdateCurrentSplitView(mCurrentSplitIndex) ;
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.recorder,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Prefs.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
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

        btnmode = (Button)findViewById(R.id.btnPrev);
        btnmode.setOnClickListener(this);

        btnmode = (Button)findViewById(R.id.btnNext);
        btnmode.setOnClickListener(this);
	}

    @Override
    public void onClick(View v) {

        if(R.id.btnStart == v.getId()) {
            onStartButtonClick();
        } else if (R.id.btnPrev == v.getId()) {
            if (mCurrentSplitIndex > 0 ) {
                mCurrentSplitIndex-- ;
                UpdateCurrentSplitView(mCurrentSplitIndex);
            }
        } else if (R.id.btnNext == v.getId()) {
            if (mCurrentSplitIndex < mSplitManager.getNumbers()-1) {
                mCurrentSplitIndex++;
                UpdateCurrentSplitView(mCurrentSplitIndex);
            }
        }
    }

    private void onStartButtonClick() {

        if (mState == STATE_NORMAL) {
            DeleteAll();
            doStartRecord();
        } else if (mState == STATE_RECORDING) {
            doStopRecord();
        }

        updateStatus();
    }



    private void doStartRecord() {


        ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC,100);
        tg.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT,BEEP_DURATIONMS);
        SystemClock.sleep(BEEP_DURATIONMS - 20);
        tg.stopTone();
        tg.release();

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

        textTIME.setText(R.string.READY);
    }

    private void doStopRecord() {


        mState = STATE_NORMAL ;

        doUnbindService();

        stopService(new Intent(this, RecordService.class)) ;

    }

    private void DeleteAll() {
        mSplitManager.clear();
        mCurrentSplitIndex = -1 ;
        mSplitAdapter.notifyDataSetChanged();
        UpdateCurrentSplitView(mCurrentSplitIndex);
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
        textTIME = v ;

		v.setTypeface(tfItalic) ; // style bold | italic = 1| 2 = 3;
		
		v = (TextView)findViewById(R.id.textElapsedTime) ;
		v.setTypeface(tfItalic);
        textElapsed = v ;
		
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
        textNumber = v ;
		
		v = (TextView)findViewById(R.id.textSplitLabel);
		v.setTypeface(tfItalic);
		
		v = (TextView)findViewById(R.id.textSplit);
		v.setTypeface(tfItalic);
        textSplit = v ;
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

    private void UpdateCurrentSplitView(int index) {
        if (index >= 0 && index < mSplitManager.getNumbers()) {
            SplitItem split = mSplitManager.getSplits().get(index) ;

            String  str = String.format("%.2f", split.getTime()/1000.0);
            textTIME.setText(str) ;
            str = String.format("%.2f", split.getSplit()/1000.0);
            textSplit.setText(str) ;

            str = String.format("%d",index + 1) ;
            textNumber.setText(str) ;
        } else if (index == -1) {
            textTIME.setText(R.string.READY);
            textNumber.setText("");
            textSplit.setText("");
        }
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
    private TextView textTIME ;
    private TextView textElapsed ;
    private TextView textNumber ;
    private TextView textSplit ;

}
