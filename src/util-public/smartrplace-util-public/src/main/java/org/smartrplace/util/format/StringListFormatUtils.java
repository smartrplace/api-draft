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
