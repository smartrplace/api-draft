package org.ogema.accessadmin.api;

import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;

/** This service shall be offered to applications for checking whether relevant permissions are available
 * TODO: Move this to smartrplace-util or ogema-gui-api*/
public interface UserPermissionService {
	/** General access permission to the room. If this is granted then checking for read or write
	 * shall return true for both. If read/write are set separately then the integer of the most
	 * specific permission set shall be returned. */
	public static final String USER_ROOM_PERM = "UserRoomPermission";
	
	/** Permission to read the data point*/
	public static final String USER_READ_PERM = "UserReadPermission";
	
	/** Permisson to write the data point*/
	public static final String USER_WRITE_PERM = "UserWritePermission";
	
	/** Permission to create children of the data point and to delete them*/
	public static final String USER_CREATE_DELETE_PERM = "UserCreateDeletePermission";
	
	/** Permission to view and download the historical log data*/
	public static final String USER_READ_HISTORICALDATA_PERM = "UserReadHistoricalDataPermission";
	
	/** Permission to administer the data point, especially change permissions*/
	public static final String USER_ADMIN_PERM = "UserAdminPermission";
	
	/** Permission to set back the setting for a {@link BuildingPropertyUnit} to the last
	 * backup marked as safe.
	 */
	public static final String APP_RESTORE_SAFE_PERM = "AppRestoreSafePermission";

	/** Permission to install an application or an application type in a certain
	 * {@link BuildingPropertyUnit}
	 */
	public static final String APP_INSTALL_PERM = "AppInstallPermission";
	
	/** Permission to generate backups for a BuildingPropertyUnit, to mark a backup as stable
	 *  and to restore them*/
	public static final String APP_RESTORE_PERM = "AppInstallPermission";
	
	/** Permission to upload new apps with a certain permission level or updates for a certain app
	 * into the Appstore.  Typically the main restrictions should now be imposed for the installation
	 * of apps - upload for installation on test servers can be done even for critical apps quit easily.
	 * For future public appstores this should be different, of course.
	 */
	public static final String APP_UPLOAD_PERM = "AppUploadPermission";

	/** Permissions for which the rooms per user shall be chosen via Multiselect*/
	public static final String[] ROOMPERMISSONS = {USER_ROOM_PERM, USER_READ_PERM, USER_WRITE_PERM,
			USER_CREATE_DELETE_PERM, USER_READ_HISTORICALDATA_PERM, USER_ADMIN_PERM };
	
	/** Permissions for which the building property units rooms per user shall be chosen via Multiselect*/
	public static final String[] PROPUNITPERMISSIONS = {APP_RESTORE_SAFE_PERM, APP_INSTALL_PERM, APP_RESTORE_PERM};
	
	/** Permissions for which the building property units rooms per user and per app or app permission type
	 * shall be chosen via Multiselect. So the table will most likely only show one app or app permission type
	 * to be edited at once.
	 * TODO: For now we give these permissions for all apps per unit. To be discussed if a per-app differentiation
	 * makes sense*/
	//public static final String[] PROPUNIT_PER_APP_PERMISSIONS = {APP_INSTALL_PERM, APP_RESTORE_PERM};
	
	/** Permission that are given per app permission type per user. So a Multiselect to choose the apps
	 * instead of choosing the rooms could be used here. It could also be integrated
	 * with PROPUNIT_PER_APP_PERMISSIONS and show as just a BooleanCheckBox there.*/
	public static final String[] USER_APP_PERMISSIONS = {APP_UPLOAD_PERM};
	
	/** True if at least a read permission for the room is granted*/
	boolean hasUserPermissionForRoom(String userName, Room room);
	
	/** Check if the user has a certain permission for a room*/
	boolean hasUserPermissionForRoom(String userName, Room room, String permissionType);
	
	/** Returns the permission value that may be greater than one for a permission granted*/
	int getUserPermissionForRoom(String userName, Room room, String permissionType);

	/** Returns the permission value that may be greater than one for a permission granted*/
	int getUserPermissionForUnitApps(String userName, String unitName, String appName, String permissionType);

	/** Returns the permission value that may be greater than one for a permission granted*/
	int getUserPermissionForApp(String userName, String appName, String permissionType);

	/** True if at least a read permission for the data point is granted*/
	//boolean hasUserPermissionForDatapoint(String userName, DatapointDesc datapoint);

	/** Check if the user has a certain permission for a datapoint*/
	//boolean hasUserPermissionForDatapoint(String userName, DatapointDesc datapoint, String permissionType);
	//int getUserPermissionForDatapoint(String userName, DatapointDesc datapoint, String permissionType);
	
	/** Get all permission values for a datapoint to be added to the Datapoint interface*/
	//int[] getPermissionValuesForDatapoint(String userName, DatapointDesc datapoint);
	
	//TODO: Add methods to check directly for write, read and write
}
