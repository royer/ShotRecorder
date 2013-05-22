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

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public class SplitManager implements Parcelable {

	
	private ArrayList<SplitItem> Splits = new ArrayList<SplitItem>();
	
	public SplitManager() {
	}
	
	public SplitManager(Parcel in) {
		readFromParcel(in);
	}
	
	public ArrayList<SplitItem> getSplits() {
		return Splits ;
	}
	
	public void clear() {
		
		
		Splits.clear() ;
	}
	
	/**
	 * 
	 * @param elpasedtime : the elapsed time from beginning
	 */
	public void append(long elpasedtime) {
		
		long splittime = 0;
		
		if (!Splits.isEmpty()) {
			SplitItem lastone = Splits.get(Splits.size() - 1) ;
			splittime = elpasedtime - lastone.getTime() ;
		}
		
		SplitItem split = new SplitItem(splittime,elpasedtime) ;
		Splits.add(split);
		
	}
	
	public void remove(int position) {
		
		SplitItem sitem = Splits.get(position);
		
		if (position < (Splits.size() - 1)) {
			
			SplitItem next = Splits.get(position + 1) ;
			long splittime = position == 0 ? 0 : next.getTime() - sitem.getTime() ;
			
			next.setSplit(splittime) ;
		}
		
		Splits.remove(position) ;
	}
	
	
	public long getTotalElapsedTime() {
		long t = 0;
		if (Splits.isEmpty() == false) {
			t = Splits.get(Splits.size() - 1).getTime() ; 
		}
		return t;
	}
	
	
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		SplitItem[] ss = Splits.toArray(new SplitItem[0]) ;
		
		dest.writeParcelableArray(ss, flags);
	}
	
	private void readFromParcel(Parcel in) {
		
		SplitItem [] ss = (SplitItem[])in.readParcelableArray(SplitItem.class.getClassLoader());
		
		Splits.addAll(Arrays.asList(ss)) ;
	}
	
	public static final Parcelable.Creator<SplitManager> CREATOR = 
			new Parcelable.Creator<SplitManager>() {

				@Override
				public SplitManager createFromParcel(Parcel source) {
					return new SplitManager(source);
				}

				@Override
				public SplitManager[] newArray(int size) {
					return new SplitManager[size];
				}
		
	};

}
