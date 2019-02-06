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
package org.smartrplace.widget.extensions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.ogema.accesscontrol.Constants;
import org.ogema.accesscontrol.SessionAuth;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class GUIUtilHelper {
	public static String getUserLoggedIn(OgemaHttpRequest req) {
        HttpSession session = req.getReq().getSession();
        SessionAuth sauth = (SessionAuth) session.getAttribute(Constants.AUTH_ATTRIBUTE_NAME);
        return sauth.getName();
	}
	
	public static <T extends Resource> Map<T, String> getValuesToSetForReferencingDropdown(Class<T> type,
			ApplicationManager appMan) {
		List<T> options =
				appMan.getResourceAccess().getResources(type);
		Map<T, String> valuesToSet = new HashMap<>();
		for(T opt: options) {
			valuesToSet.put(opt, ResourceUtils.getHumanReadableName(opt));					
		}
		return valuesToSet;
	}
	
	/**
	 * 
	 * @param values
	 * @param valuesToSet if null a new map is generated
	 */
	public static <T extends Resource, R extends T> Map<T, String> getValuesToSetForReferencingDropdown(List<R> values,
			Map<T, String> valuesToSet) {
		if(valuesToSet == null) valuesToSet = new HashMap<>();
		for(R opt: values) {
			valuesToSet.put(opt, ResourceUtils.getHumanReadableName(opt));					
		}
		return valuesToSet;
	}
}
