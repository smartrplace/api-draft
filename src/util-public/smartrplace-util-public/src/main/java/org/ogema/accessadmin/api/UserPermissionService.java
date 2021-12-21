package org.ogema.accessadmin.api;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;

/** This service shall be offered to applications for checking whether relevant permissions are available
 * TODO: Move this to smartrplace-util or ogema-gui-api*/
public interface UserPermissionService {
	/** Rooms marked as priority are the primary rooms displayed to the user. Other rooms may be viewed, but
	 * are not shown by default. Implies USER_ROOM_PERM*/
	public static final String USER_PRIORITY_PERM = "UserRoomPriority";	
	//public static final String SUBCUSTOMER_PRIORITY_PERM = "UserSubcustomerPriority";	
	
	/** General access permission to the room. If this is granted then checking for read or write
	 * shall return true for both. If read/write are set separately then the integer of the most
	 * specific permission set shall be returned. */
	public static final String USER_ROOM_PERM = "UserRoomPermission";
	//public static final String USER_SUBCUSTOMER_PERM = "UserSubcustomerPermission";
	
	/** Permission to read the data point*/
	public static final String USER_READ_PERM = "UserReadPermission";
	
	/** Permisson to write the data point*/
	public static final String USER_WRITE_PERM = "UserWritePermission";
	
	/** Permission to create children of the data point and to delete them*/
	public static final String USER_CREATE_DELETE_PERM = "UserCreateDeletePermission";
	
	/** Permission to view and download the historical log data*/
	public static final String USER_READ_HISTORICALDATA_PERM = "UserReadHistoricalDataPermission";
	
	/** Permission to administer the room, especially change general settings like eco mode temperature*/
	public static final String USER_ADMIN_PERM = "UserAdminPermission";
	
	/** Permission to set back the setting for a {@link BuildingPropertyUnit} to the last
	 * backup marked as safe.
	 */
	public static final String RESTORE_SAFE_PERM = "RestoreSafeStatePermission";

	/** Permission to install an application or an application type in a certain
	 * {@link BuildingPropertyUnit}
	 */
	public static final String UPDATE_INSTALL_PERM = "UpdateInstallPermission";
	
	/** Permission to generate backups for a BuildingPropertyUnit, to mark a backup as stable
	 *  and to restore them*/
	public static final String UPDATE_RATING_PERM = "UpdateRatingPermission";
	
	/** Permission to upload new apps with a certain permission level or updates for a certain app
	 * into the Appstore.  Typically the main restrictions should now be imposed for the installation
	 * of apps - upload for installation on test servers can be done even for critical apps quit easily.
	 * For future public appstores this should be different, of course.
	 */
	public static final String UPDATE_DEFINITION_PERM = "UpdateDefinitionPermission";

	/** Permissions for which the rooms per user shall be chosen via Multiselect. Currently USER_READ_HISTORICALDATA_PERM, USER_ADMIN_PERM
	 * are not processed per room, but just per user. So APPCONFIG_ permission types are used.*/
	public static final String[] ROOMPERMISSONS = {USER_PRIORITY_PERM, USER_ROOM_PERM,
			USER_READ_HISTORICALDATA_PERM, USER_ADMIN_PERM };
	/** The extended version is currently not used*/
	public static final String[] ROOMPERMISSONS_EXTENDED = {USER_PRIORITY_PERM, USER_ROOM_PERM, USER_READ_PERM, USER_WRITE_PERM,
			USER_CREATE_DELETE_PERM, USER_READ_HISTORICALDATA_PERM, USER_ADMIN_PERM };
	
	/** User System Permissions. Some of them may be defined per Building Unit in the future.*/
	public static final String[] PROPUNITPERMISSIONS = {RESTORE_SAFE_PERM, UPDATE_INSTALL_PERM, UPDATE_RATING_PERM,
			UPDATE_DEFINITION_PERM};
	
	/** Permissions for which the subcustomers per user shall be chosen via Multiselect*/
	//public static final String[] SUBCUSTOMER_PERMISSONS = {SUBCUSTOMER_PRIORITY_PERM, USER_SUBCUSTOMER_PERM};

	public static final String MONITORING = "KPIs and Charts";
	public static final String INSTALLATION_SETUP = "Setup&Installation";
	public static final String ALARMING = "Alarming";
	public static final String USER_MANAGEMENT = "User Management";
	public static final String GROUP_AND_PERMISSION_MANAGEMENT = "Groups and Permissions";
	public static final String APPSTORE = "Appstore";
	public static final String BACNET = "BACnet Admin";
	
	public static final String APPCONFIG_LOGOUT = "Logout";
	public static final String APPCONFIG_OTHER_APPS = "Other Apps";
	public static final String APPCONFIG_MEASUREMENTS = "Measuremt.";
	public static final String APPCONFIG_ECOMODE = "Ecomode";
	public static final String APPCONFIG_DETAILPAGE = "Detailpage";
	public static final String APPCONFIG_SETPOINT_CURVE = "Setpoint Curve";
	public static final String APPCONFIG_SETUPPAGE = "Setup Page";
	
	//Permission under development
	public static final String DASHBOARD_GENERAL = "Dashboad General";
	public static final String DASHBOARD_SAVINGS = "Savings Dashboad";
	public static final String DASHBOARD_CUSTOMERDOC = "FAQ Dashboad";
	public static final String ROOMCONTROL_WE = "Roomcontrol WE";
	public static final String ROOMCONTROL_2D = "Roomcontrol 2D";
	public static final String ROOM_STATUS_CONTROL = "Room Status Control";
	public static final String MODBUS_SERVER = "Modbus/Bacnet Server";
	public static final String MONTORING_CHART_EXPERT = "Expert Charts";
	
	public static final String[] APP_ACCESS_PERMISSIONS_ALL = {MONITORING, INSTALLATION_SETUP, ALARMING,
			USER_MANAGEMENT, GROUP_AND_PERMISSION_MANAGEMENT, APPSTORE,
			DASHBOARD_GENERAL, DASHBOARD_SAVINGS, DASHBOARD_CUSTOMERDOC,
			ROOMCONTROL_WE, ROOMCONTROL_2D, ROOM_STATUS_CONTROL, MODBUS_SERVER, MONTORING_CHART_EXPERT};

	public static final String[] APP_ACCESS_PERMISSIONS = {MONITORING, INSTALLATION_SETUP, ALARMING,
			USER_MANAGEMENT, GROUP_AND_PERMISSION_MANAGEMENT};
	public static final String[] APP_ACCESS_PERMISSIONS_WITHAPPSTORE = {MONITORING, INSTALLATION_SETUP, ALARMING,
			USER_MANAGEMENT, GROUP_AND_PERMISSION_MANAGEMENT, APPSTORE};
	public static final String[] APP_ACCESS_PERMISSIONS_FOR_SUPERADMIN = {DASHBOARD_GENERAL, DASHBOARD_SAVINGS, DASHBOARD_CUSTOMERDOC,
			ROOMCONTROL_WE, ROOMCONTROL_2D, ROOM_STATUS_CONTROL, MODBUS_SERVER, MONTORING_CHART_EXPERT};
	
	public static final String[] APP_CONFIG_PERMISSIONS = {APPCONFIG_LOGOUT, APPCONFIG_OTHER_APPS, APPCONFIG_MEASUREMENTS,
			APPCONFIG_ECOMODE, APPCONFIG_SETPOINT_CURVE, APPCONFIG_DETAILPAGE, APPCONFIG_SETUPPAGE};

	/** Permissions for which the building property units rooms per user and per app or app permission type
	 * shall be chosen via Multiselect. So the table will most likely only show one app or app permission type
	 * to be edited at once.
	 * TODO: For now we give these permissions for all apps per unit. To be discussed if a per-app differentiation
	 * makes sense*/
	//public static final String[] PROPUNIT_PER_APP_PERMISSIONS = {APP_INSTALL_PERM, APP_RESTORE_PERM};
	
	/** Permission that are given per app permission type per user. So a Multiselect to choose the apps
	 * instead of choosing the rooms could be used here. It could also be integrated
	 * with PROPUNIT_PER_APP_PERMISSIONS and show as just a BooleanCheckBox there.*/
	public static final String[] USER_APP_PERMISSIONS = {UPDATE_DEFINITION_PERM};
	
	/** True if at least a read permission for the room is granted*/
	boolean hasUserPermissionForRoom(String userName, Room room);
	
	/** Check if the user has a certain permission for a room*/
	boolean hasUserPermissionForRoom(String userName, Room room, String permissionType);
	
	/** Returns the permission value that may be greater than one for a permission granted*/
	int getUserPermissionForRoom(String userName, Room room, String permissionType);

	/**
	 * @param getSuperSetting if true the specific setting for the user and room will be ignored and
	 * 		only the more general setting will be returned
	 */
	public int getUserPermissionForRoom(String userName, String resourceId, String permissionType,
			boolean getSuperSetting);

	/** Returns the permission value that may be greater than one for a permission granted*/
	int getUserSystemPermission(String userName, String permissionType);

	/** Permission types allowed here are from APP_ACCESS_PERMISSIONS_ALL*/
	int getUserStatusAppPermission(UserStatus userStatus, String permissionType, boolean useWorkingCopy);
	
	/** Returns the permission value that may be greater than one for a permission granted*/
	//int getUserPermissionForUnitApps(String userName, String unitName, String appName, String permissionType);

	/** Returns the permission value that may be greater than one for a permission granted
	 * Note: Management of these permissions not implemented yet*/
	int getUserPermissionForApp(String userName, String appName, String permissionType);

	/** True if at least a read permission for the data point is granted*/
	//boolean hasUserPermissionForDatapoint(String userName, DatapointDesc datapoint);

	/** Check if the user has a certain permission for a datapoint*/
	//boolean hasUserPermissionForDatapoint(String userName, DatapointDesc datapoint, String permissionType);
	//int getUserPermissionForDatapoint(String userName, DatapointDesc datapoint, String permissionType);
	
	/** Get all permission values for a datapoint to be added to the Datapoint interface*/
	//int[] getPermissionValuesForDatapoint(String userName, DatapointDesc datapoint);
	
	//TODO: Add methods to check directly for write, read and write
	
	/** Get filter for a user group for which membership is not specified by {@link AccessConfigUser#superGroups()}
	 * 
	 * @param userGroupName name of the group
	 * @return null if the group membership specified via superGroups
	 */
	default GenericFilterFixedGroup<String, AccessConfigUser> getUserGroupFiler(String userGroupName) {
		return null;
	}
	default GenericFilterFixedGroup<Room, BuildingPropertyUnit> getRoomGroupFiler(String roomGroupName) {
		return null;
	}
	
	public static class UserStatusResult {
		public UserStatus status = null;
		public List<String> addPerms = null;
	}
	UserStatusResult getUserStatus(String userName);
	
	public boolean hasExtendedView(HttpSession session);
	public boolean hasExtendedView(String user);
}
