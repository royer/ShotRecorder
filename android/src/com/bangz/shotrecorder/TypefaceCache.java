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

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;

public class TypefaceCache {

	public static final String FONT_ITALIC_PATH = "fonts/digital-7 (italic).ttf" ;
	public static final String FONT_NORMAL_PATH = "fonts/digital-7.ttf" ;
	public static final String FONT_MONO_PATH = "fonts/digital-7 (mono).ttf" ;
	
	private static final HashMap<String, Typeface> map =
			new HashMap<String, Typeface>();
	public static Typeface getTypeface(String file, Context context) {
		Typeface result = map.get(file);
		if (result == null) {
			result = Typeface.createFromAsset(context.getAssets(), file);
			map.put(file, result);
	    }
	    return result;
	}
}
