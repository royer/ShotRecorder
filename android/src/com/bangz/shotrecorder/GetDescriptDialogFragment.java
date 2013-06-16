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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Created by royer on 14/06/13.
 */
public class GetDescriptDialogFragment extends SherlockDialogFragment {

    public interface DescriptDialogListener {
        public void onGetDescription(GetDescriptDialogFragment dialog, String descript);
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_getdescription, null))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        EditText v = (EditText)GetDescriptDialogFragment.this.getDialog().findViewById(R.id.txtDescription);
                        String str = v.getText().toString() ;

                        mListener.onGetDescription(GetDescriptDialogFragment.this, str);

                        GetDescriptDialogFragment.this.getDialog().dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GetDescriptDialogFragment.this.getDialog().cancel();
                    }
                });


        return builder.create();
    }

    private DescriptDialogListener mListener ;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (DescriptDialogListener)activity ;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implements DescriptDialogListener.");
        }
    }
}
