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
import org.ogema.core.administration.UserAccount;
import org.ogema.core.administration.UserConstants;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.app.useradmin.config.UserAdminData;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class GUIUtilHelper {
	public static String getUserLoggedIn(OgemaHttpRequest req) {
        HttpSession session = req.getReq().getSession();
        return getUserLoggedInBase(session);
	}
	public static String getUserLoggedInBase(HttpSession session) {
        SessionAuth sauth = (SessionAuth) session.getAttribute(Constants.AUTH_ATTRIBUTE_NAME);
        if(sauth == null)
        	return null;
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
	
	public static NaturalPerson getUserData(String userName, ResourceAccess ra) {
		ResourceList<NaturalPerson> userData = ra.getResource("userAdminData/userData");
		NaturalPerson ud = ResourceListHelper.getNamedElementFlex(userName, userData);
		return ud;
	}
	
	/** Version using resource data*/
	public static String getRealName(String userName, ResourceAccess ra) {
		NaturalPerson ud = getUserData(userName, ra);
		if(ud == null)
			return null;
		if(ud.firstName().isActive() && ud.lastName().isActive())
			return ud.firstName().getValue()+" "+ud.lastName().getValue();
		if(ud.userName().isActive())
			return ud.userName().getValue();
		if(ud.firstName().isActive())
			return ud.firstName().getValue();
		if(ud.lastName().isActive())
			return ud.lastName().getValue();
		return null;
	}
	
	/** Version using the admin data*/
	public static String getRealName(UserAccount userAccount) {
		String realName =userAccount.getProperties().getOrDefault(UserConstants.FORMATTED_NAME, "--").toString();
		return realName;	
	}
	
	public static NaturalPerson setRealName(String realName, ResourceAccess ra, UserAccount userAccount) {
		userAccount.getProperties().put(UserConstants.FORMATTED_NAME, realName);
		String userName = userAccount.getName();
		ResourceList<NaturalPerson> userData = ra.getResource("userAdminData/userData");
		NaturalPerson ud = getOrCreateUserPropertyResource(userName, userData); //getUserData(userName, ra);
		if(ud == null)
			return null;
		ValueResourceHelper.setCreate(ud.userName(), realName);
		return ud;
	}

    public static NaturalPerson getOrCreateUserPropertyResource(String userId, ResourceList<NaturalPerson> userData) {
        //Resource userDataRes = data.userData().addDecorator(userId, NaturalPerson.class);
        NaturalPerson userDataRes = ResourceListHelper.getOrCreateNamedElementFlex(userId, userData);
        userDataRes.create();
        userData.activate(false);
        userDataRes.activate(false);
        return userDataRes;
    }

    public static NaturalPerson deleteUser(String userName, ApplicationManager appMan) {
		appMan.getAdministrationManager().removeUserAccount(userName);
		UserAccount restAc = getRestUserAccount(userName, appMan);
		if(restAc != null)
			appMan.getAdministrationManager().removeUserAccount(restAc.getName());

    	UserAdminData udd = appMan.getResourceAccess().getResource("userAdminData");
    	NaturalPerson userDataRes = ResourceListHelper.getOrCreateNamedElementFlex(userName, udd.userData());
    	if(userDataRes != null)
    		userDataRes.delete();
    	return userDataRes;
    }
    
    /** Get user account or return null if not existing
     * 
     * @param userName
     * @return
     */
    public static UserAccount getUserAccount(String userName, ApplicationManager appMan) {
   		try {
			return appMan.getAdministrationManager().getUser(userName);
		} catch(RuntimeException e) {
		}
   		return null;
    }
    public static UserAccount getRestUserAccount(String naturalUserName, ApplicationManager appMan) {
    	String restUsername = naturalUserName+"_rest";
    	return getUserAccount(restUsername, appMan);
    }

    /*public static void registerStyleSheet(String cssFileName, String baseURL, WidgetApp wApp,
			ApplicationManager appMan) {
		final String stylesheet_URL = baseURL + "/"+ cssFileName;
		final String stylesheet_LINK = "<link rel=\"stylesheet\" href=\""+ stylesheet_URL + "\">";
		appMan.getWebAccessManager().registerWebResource(stylesheet_URL, cssFileName);
		wApp.addStylesheet(cssFileName, null);
		addStyle(wApp.getPages().values(), stylesheet_LINK);
		
	}
	
	public static void addStyle(Collection<WidgetPage<?>> pages, String stylesheet_LINK) {
		for (WidgetPage<?> p : pages)
			p.append(stylesheet_LINK);
	}*/
}
