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

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.mobeta.android.dslv.DragSortListView;

public class SplitListFragment extends SherlockListFragment implements
    DragSortListView.RemoveListener{

	public static final String TAG = "SplitListFragment" ;


    public interface OnSplitItemSelectedListerner {
        public void onSplitItemSelected(int position) ;
        public void onSplitItemRemoved(int position) ;
    }

    OnSplitItemSelectedListerner    mListener ;
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        DragSortListView lv = (DragSortListView)getListView();
        lv.setRemoveListener(this);
	}

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG,"onAttach");
		super.onAttach(activity);

        try {
            mListener = (OnSplitItemSelectedListerner)activity;
        }catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSplitItemSelectedListener");
        }
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.split_list, null) ;

		TextView theader = (TextView)v.findViewById(R.id.colNumber);
		theader.setTypeface(null, Typeface.BOLD);
		
		theader = (TextView)v.findViewById(R.id.colSplit);
		theader.setTypeface(null,Typeface.BOLD) ;
		
		theader = (TextView)v.findViewById(R.id.colTime);
		theader.setTypeface(null,Typeface.BOLD);
		
		return v ;
		//return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		
		super.onViewCreated(view, savedInstanceState);
		
		// add Header
		//this.getListView().addHeaderView(mHeader);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Log.d(TAG,"position = " + position + "id = " + id);

        if(mListener != null)
            mListener.onSplitItemSelected(position);
	}


    @Override
    public void remove(int which) {

        if (mListener != null) {
            mListener.onSplitItemRemoved(which);
        }
    }

}
