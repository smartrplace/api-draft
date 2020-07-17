package org.ogema.accessadmin.api;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Basic user types defined in the system*/
public enum UserStatus {
	/** A defect user does not have the app permissions defined in BASE_APPS. This user cannot
	 * perform the standard logout process, so such a user should not exist on the system.
	 */
	DISABLED,
	/** A raw user does not have all permissions in USER_APPS. Such a user cannot acsess the
	 * basic SmartrRoomControl user apps*/
	//RAW,
	/** A display user can just open the Room Control Front-end GUI, the "Other Apps" button is deactivated*/
	DISPLAY,
	/** A standard user can access the basic SmartrRoomControl user apps, but not all admin apps*/ 
	USER_STD,
	/** A secretary user can access at least all apps defined in USER_STD, but usually has additional
	 * app permissions*/
	SECRETARY,
	/** An admin user can access at least all apps defined in ADMIN_APPS*/
	ADMIN,
	/** Super admin has access to framework administration, full message settings etc.*/
	SUPERADMIN;

	/** Only access to standard room control interface*/
	//GUEST,
	/** Like ADMIN, also access to simulation GUI*/
	//TESTER
	
	public static String getLabel(UserStatus obj, OgemaLocale locale) {
		switch(obj) {
		case DISABLED:
			return "Disabled";
		case DISPLAY:
			return "Display User";
		case USER_STD:
			return "User Standard";
		case SECRETARY:
			return "Secretary";
		case ADMIN:
			return "Facility Manager";
		case SUPERADMIN:
			return "Master Administrator";
		default:
			throw new IllegalStateException("Unknown type:"+obj);
		}
	}

}
