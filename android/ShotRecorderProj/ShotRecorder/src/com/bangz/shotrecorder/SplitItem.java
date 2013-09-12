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

import android.os.Parcel;
import android.os.Parcelable;

public class SplitItem implements Parcelable {

	/**
	 * from previous shot to this shot elapsed time
	 * if 0 mean is the first shot, it shuld display "---"
	 * 
	 * <p>get method: {@link #getSplit() } </p>
	 * <p>set method: {@link #setSplit(long split)} </p> 
	 */
	private long	mSplit ;
	
	/**
	 * from start to this shot elapsed time
	 * <p>get method: {@link #getTime()}</p>
	 *<p>set method: {@link #setTime(long time)} </p>
	 */
	private long	mTime ;
	
	
	public SplitItem(long split, long time) {
		mSplit = split ;
		mTime = time ;
	}
	
	public SplitItem(Parcel in) {
		readFromParcel(in);
	}
	
	public long getSplit() {
		return mSplit ;
	}
	
	/**
	 * 
	 * @param split
	 * @return the old mSplit value ;
	 */
	public long setSplit(long split) {
		long old = mSplit;
		mSplit = split ;
		return old;
	}
	
	public long getTime() {
		return mTime;
	}
	/**
	 * 
	 * @param time
	 * @return the old mTime value
	 */
	public long setTime(long time) {
		long old = mTime ;
		mTime = time ;
		return old ;
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mSplit);
		dest.writeLong(mTime) ;
	}

	private void readFromParcel(Parcel in) {
		mSplit = in.readLong() ;
		mTime = in.readLong();
	}
	
	public static final Parcelable.Creator<SplitItem> CREATOR = 
			new Parcelable.Creator<SplitItem>() {

				@Override
				public SplitItem createFromParcel(Parcel source) {
					return new SplitItem(source);
				}

				@Override
				public SplitItem[] newArray(int size) {
					return new SplitItem[size];
				}
			};
}
