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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SplitArrayAdapter extends ArrayAdapter<SplitItem> {
	
	private LayoutInflater	mInflater ;
	
	private ArrayList<SplitItem>	mValues ;
	
	
	private static class ViewHolder {
		
		public TextView	Number ;
		public TextView Split ;
		public TextView Time ;
		
	}

	public SplitArrayAdapter(Context context, ArrayList<SplitItem> values) {
		
		super(context, R.layout.split_list_entry, values) ;
		
        // Cache the LayoutInflate to avoid asking for a new one each time.
		mInflater = LayoutInflater.from(context);
		
		
		mValues = values ;
		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder viewHolder ;
		
		if (convertView == null) {
			
			convertView = this.mInflater.inflate(R.layout.split_list_entry, null) ;
			
			viewHolder = new ViewHolder() ;
			
			viewHolder.Number = (TextView)convertView.findViewById(R.id.colNumber) ;
			viewHolder.Split = (TextView)convertView.findViewById(R.id.colSplit) ;
			viewHolder.Time = (TextView)convertView.findViewById(R.id.colTime) ;

			//viewHolder.Number.setTypeface(TypefaceCache.getTypeface(TypefaceCache.FONT_MONO_PATH, this.getContext()));
			//viewHolder.Split.setTypeface(TypefaceCache.getTypeface(TypefaceCache.FONT_MONO_PATH, this.getContext()));
			//viewHolder.Time.setTypeface(TypefaceCache.getTypeface(TypefaceCache.FONT_MONO_PATH, this.getContext()));

			convertView.setTag(viewHolder) ;
			
		} else {
			viewHolder = (ViewHolder)convertView.getTag() ;
		}
		
		
		viewHolder.Number.setText(String.format("%d", position + 1)) ;
		
		viewHolder.Split.setText(String.format("%.2f", mValues.get(position).getSplit()/1000.0)) ;
		
		viewHolder.Time.setText(String.format("%.2f", mValues.get(position).getTime()/1000.0));
		
		
		return convertView ;
	}
	
	
}
