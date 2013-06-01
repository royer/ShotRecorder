/*
 * Copyright (C) 2011 The Android Open Source Project
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
 */

package com.bangz.shotrecorder;


import android.content.Context;

import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by royer on 30/05/13.
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private boolean mTrackingTouch ;

    private int mProgress ;
    private int mMax;
    private int mMin ;
    private int mProgressMax ;

    TextView  textValue ;


    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);


        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
               R.styleable.SeekBarPreference, defStyle, 0);
        //TODO get attributes
        try {

            mMax = a.getInt(R.styleable.SeekBarPreference_MaxValue, 100);

            mMin = a.getInt(R.styleable.SeekBarPreference_MinValue, 0) ;

            setMax(mMax - mMin) ;

        } finally {
            a.recycle();
        }


        setLayoutResource(R.layout.seekbar_preference);
    }


    public SeekBarPreference(Context context, AttributeSet attrs) {
        //super(context, attrs);
        this(context,attrs, 0) ;
    }

    public SeekBarPreference(Context context) {
        //super(context);
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        SeekBar seekbar = (SeekBar) view.findViewById(R.id.seekBar);
        textValue = (TextView)view.findViewById(android.R.id.text1);

        seekbar.setOnSeekBarChangeListener(this);
        seekbar.setMax(mProgressMax);
        seekbar.setProgress(mProgress - mMin);
        seekbar.setEnabled(isEnabled());

        textValue.setText(Integer.toString(mProgress));

    }

    private void setMax(int max) {
        if (max != mProgressMax) {
            mProgressMax = max ;
            notifyChanged();
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setProgress(restorePersistedValue ? getPersistedInt(mProgress) :
                (Integer)defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {

        return a.getInt(index, 0);
    }



    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < mMin) {
            progress = mMin;
        }

        notifyDependencyChange(progress == mMin);

        if (progress != mProgress) {
            mProgress = progress;

            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return mProgress;
    }

    /**
     * Persist the seekBar's progress value if callChangeListener
     * returns true, otherwise set the seekBar's progress to the stored value
     */
    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress() + mMin;
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
                textValue.setText(Integer.toString(mProgress));
            } else {
                seekBar.setProgress(mProgress - mMin);
                textValue.setText(Integer.toString(mProgress));
            }
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return mProgress == mMin ;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            syncProgress(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }

    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        myState.max = mMax;
        myState.min = mMin;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;
        mMax = myState.max;
        mMin = myState.min ;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        int max;
        int min;

        public SavedState(Parcel source) {
            super(source);

            // Restore the click counter
            progress = source.readInt();
            max = source.readInt();
            min = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            // Save the click counter
            dest.writeInt(progress);
            dest.writeInt(max);
            dest.writeInt(min) ;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
