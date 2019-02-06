/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.util.format;

import java.util.List;

public class StringListFormatUtils {
	public static String getStringFromList(List<String> slist, String... addString) {
		String result = null;
		if(slist != null) for(String s: slist) {
			if(result == null) result = s;
			else result += "-"+s;
		}
		for(String s: addString) {
			if(result == null) result = s;
			else result += "-"+s;			
		}
		if(result == null) return "";
		return result;
	}
}
