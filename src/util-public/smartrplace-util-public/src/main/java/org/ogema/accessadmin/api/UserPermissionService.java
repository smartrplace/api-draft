package org.ogema.accessadmin.api;

import org.ogema.devicefinder.api.DatapointDesc;
import org.ogema.model.locations.Room;

public interface UserPermissionService {
	/** General access permission to the room*/
	public static final String USER_ROOM_PERM = "UserRoomPermission";
	
	/** Permission to read the data point*/
	public static final String USER_READ_PERM = "UserReadPermission";
	
	/** Permisson to write the data point*/
	public static final String USER_WRITE_PERM = "UserWritePermission";
	
	/** Permission to create children of the data point and to delete them*/
	public static final String USER_CREATE_DELETE_PERM = "UserCreateDeletePermission";
	
	/** Permission to administer the data point, especially change permissions*/
	public static final String USER_ADMIN_PERM = "UserAdminPermission";
	
	/** True if at least a read permission for the room is granted*/
	boolean hasUserPermissionForRoom(String userName, Room room);
	
	/** Check if the user has a certain permission for a room*/
	boolean hasUserPermissionForRoom(String userName, Room room, String permissionType);
	/** Returns the permission value that may be greater than one for a permission granted*/
	int getUserPermissionForRoom(String userName, Room room, String permissionType);

	/** True if at least a read permission for the data point is granted*/
	boolean hasUserPermissionForDatapoint(String userName, DatapointDesc datapoint);

	/** Check if the user has a certain permission for a datapoint*/
	boolean hasUserPermissionForDatapoint(String userName, DatapointDesc datapoint, String permissionType);
	int getUserPermissionForDatapoint(String userName, DatapointDesc datapoint, String permissionType);
	
	/** Get all permission values for a datapoint to be added to the Datapoint interface*/
	int[] getPermissionValuesForDatapoint(String userName, DatapointDesc datapoint);
	
	//TODO: Add methods to check directly for write, read and write
	
	//TODO: Add mechanism to also support permissions for Datapoint types ({@link DatapointDesc}).
	
	//TODO: Define permissions specific to the appstore e.g defining the permissions accepted for a user
	//to upload or install apps and the instances on which a user may install apps. Typically the main restrictions
	//should now be imposed for the installation of apps - upload for installation on test servers can
	//be done even for critical apps quit easily. For future public appstores this should be different, of course.
}
