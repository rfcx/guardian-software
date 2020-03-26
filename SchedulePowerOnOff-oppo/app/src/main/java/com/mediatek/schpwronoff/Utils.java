/**
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.mediatek.schpwronoff;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class Utils {
	private static final String TAG = "Utils";

	/**
	 * Add for oppo actionbar made
	 * 
	 * @param activity
	 */
	public static void actionbarMade(final Activity activity) {
		if (activity == null) {
			return;
		}
		final ActionBar mActionBar = activity.getActionBar();
		if (null == mActionBar) {
			return;
		}
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);
		LayoutInflater inflator = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View v = inflator.inflate(R.layout.oppo_custom_actionbar, null);
		final TextView titleView = ((TextView) v.findViewById(R.id.title));
		String title = activity.getTitle().toString();
		if (!TextUtils.isEmpty(title)) {
			titleView.setText(title);
		}
		OnClickListener backClickListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				activity.finish();
			}
		};
		titleView.setOnClickListener(backClickListener);

		mActionBar.setCustomView(v, new ActionBar.LayoutParams(
				ActionBar.LayoutParams.MATCH_PARENT,
				ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL));

	}

	/**
	 * for preferencescreen dialog actionbar made
	 */
	public static void actionbarMade(final PreferenceScreen preferencescreen,
			final Activity activity) {
		if (activity == null || preferencescreen == null) {
			return;
		}
		final Dialog dialog = preferencescreen.getDialog();
		if (dialog == null) {
			return;
		}
		ActionBar actionBar = dialog.getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		LayoutInflater inflator = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View v = inflator.inflate(R.layout.oppo_custom_actionbar, null);
		final TextView titleView = ((TextView) v.findViewById(R.id.title));
		titleView.setText(preferencescreen.getTitle());
		OnClickListener backClickListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		};
		titleView.setOnClickListener(backClickListener);
		actionBar.setCustomView(v, new ActionBar.LayoutParams(
				ActionBar.LayoutParams.MATCH_PARENT,
				ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL));
	}
	
	/**
     * check apk is install or not
     * */
	public static boolean isApkExist(Context context, String packageName){
    	 boolean isApkExist = false;
         try {
             ApplicationInfo info = context.getPackageManager()
                     .getApplicationInfo(packageName ,
                             PackageManager.GET_UNINSTALLED_PACKAGES);
             isApkExist = true;
         } catch (NameNotFoundException e) { 
        	 Log.d(TAG , "apk not found !");
         }
	return isApkExist;
    }

}
