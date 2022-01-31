package org.ogema.accessadmin.api;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Basic user types defined in the system*/
public enum UserStatus {
	/** A defect user does not have the app permissions defined in BASE_APPS. This user cannot
	 * perform the standard logout process, so such a user should not exist on the system.
	 */
	DISABLED,
	/** A public user can be accessed by a public login, this user can e.g. be used to display the page to reset
	 * the password for new users or those who forgot their password
	 */
	PUBLIC,
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
	public static final String DISABLED_NAME = "Disabled";
	public static final String PUBLIC_NAME = "Public Access";
	public static final String DISPLAY_NAME = "Display User";
	public static final String USER_STD_NAME = "User Standard";
	public static final String SECRETARY_NAME = "Secretary";
	public static final String FACILITY_MAN_NAME = "Facility Manager";
	public static final String SUPERADMIN_NAME = "Master Administrator";
	
	public static String getLabel(UserStatus obj, OgemaLocale locale) {
		switch(obj) {
		case DISABLED:
			return DISABLED_NAME;
		case PUBLIC:
			return PUBLIC_NAME;
		case DISPLAY:
			return DISPLAY_NAME;
		case USER_STD:
			return USER_STD_NAME;
		case SECRETARY:
			return SECRETARY_NAME;
		case ADMIN:
			return FACILITY_MAN_NAME;
		case SUPERADMIN:
			return SUPERADMIN_NAME;
		default:
			throw new IllegalStateException("Unknown type:"+obj);
		}
	}

	public static UserStatus getbyLabel(String label) {
		switch(label) {
		case DISABLED_NAME:
			return DISABLED;
		case PUBLIC_NAME:
			return PUBLIC;
		case DISPLAY_NAME:
			return DISPLAY;
		case USER_STD_NAME:
			return USER_STD;
		case SECRETARY_NAME:
			return SECRETARY;
		case FACILITY_MAN_NAME:
			return ADMIN;
		case SUPERADMIN_NAME:
			return SUPERADMIN;
		default:
			return null;		
		}
	}
	
}
