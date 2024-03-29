package org.ogema.accessadmin.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;

import de.iwes.util.resource.ValueResourceHelper;

public class UserPermissionUtil {
	public static final String SYSTEM_RESOURCE_ID = "system";
	
	public static interface PermissionForLevelProvider {
		Integer getUserPermissionForLevel(AccessConfigUser userAcc, String resourceId, String permissionType);
	}

	/*public static class PermissionInUtil {
		public String type;
		public int value;
		public PermissionInUtil(String type, int value) {
			this.type = type;
			this.value = value;
		}
	}*/
	public static class RoomPermissionData {
		public String resourceId;
		public Map<String, Integer> permissions = new HashMap<>();
	}
	public static RoomPermissionData getRoomPermissionData(String roomLocation, AccessConfigUser userAcc) {
		return getResourcePermissionData(roomLocation, userAcc.roompermissionData());
	}
	public static RoomPermissionData getResourcePermissionData(String resourceId, AccessConfigBase configRes) {
		int idx = 0;
		RoomPermissionData result = new RoomPermissionData();
		result.resourceId = resourceId;
		String[] types = configRes.permissionTypes().getValues();
		int[] values = configRes.permissionValues().getValues();
		for(String id: configRes.resourceIds().getValues()) {
			if(id.contentEquals(resourceId)) {
				result.permissions.put(types[idx], values[idx]);
			}
			idx++;
		}
		return result ;
	};
	public static Integer getRoomAccessPermission(String roomLocation, String permissionType, AccessConfigUser userAcc) {
		RoomPermissionData roomPerms = getRoomPermissionData(roomLocation, userAcc);
		Integer result = roomPerms.permissions.get(permissionType);
		if(result != null)
			return result;
		return null;
	};
	public static Integer getAppAccessPermission(String appName, String permissionType, AccessConfigUser userAcc) {
		RoomPermissionData roomPerms = getResourcePermissionData(appName, userAcc.appstorePermissionData());
		Integer result = roomPerms.permissions.get(permissionType);
		if(result != null)
			return result;
		return null;
	};
	public static Integer getOtherSystemAccessPermission(String permissionType, AccessConfigUser userAcc) {
		RoomPermissionData roomPerms = getResourcePermissionData(SYSTEM_RESOURCE_ID, userAcc.otherResourcepermissionData());
		Integer result = roomPerms.permissions.get(permissionType);
		if(result != null)
			return result;
		return null;
	};
	/*public static Integer getOtherAccessPermission(String unitName, String appName, String permissionType, AccessConfigUser userAcc) {
		RoomPermissionData roomPerms = getResourcePermissionData(unitName+"$$"+appName, userAcc.otherResourcepermissionData());
		Integer result = roomPerms.permissions.get(permissionType);
		if(result != null)
			return result;
		return null;
	};*/
	
	public static AccessConfigUser getUserPermissions(ResourceList<AccessConfigUser> userPerms, String userName) {
		for(AccessConfigUser userData: userPerms.getAllElements()) {
			if(userData.name().getValue().equals(userName))
				return userData;
		}
		return null;
	}
	public static AccessConfigUser getOrCreateUserPermissions(ResourceList<AccessConfigUser> userPerms, String userName) {
		AccessConfigUser result = getUserPermissions(userPerms, userName);
		if(result != null)
			return result;
		result = userPerms.add();
		ValueResourceHelper.setCreate(result.name(), userName);
		result.activate(true);
		return result;
	}
	
	/** Process level of user groups
	 * 
	 * @param levelPlus all user groups in the level. This shall NOT be a list containing natural users as for these
	 * 		the type 2 user groups would not be processed
	 * @param resourceId
	 * @param permissionType
	 * @param levelProv
	 * @return
	 */
	public static Integer getUserPermissionForUserGroupLevel(List<AccessConfigUser> levelPlus, String resourceId, String permissionType,
			PermissionForLevelProvider levelProv) {
		List<AccessConfigUser> levelPlusNew = new ArrayList<>();
		for(AccessConfigUser grp: levelPlus) {
			Integer resultLoc = levelProv.getUserPermissionForLevel(grp, resourceId, permissionType);
			//Integer resultLoc = getUserPermissionForRoomLevel(grp, resourceId, permissionType, roomGroups);
			if(resultLoc != null)
				return resultLoc;
			List<AccessConfigUser> plusLevelGrps = grp.superGroups().getAllElements();
			if(plusLevelGrps != null)
				levelPlusNew.addAll(plusLevelGrps);			
		}
		if(levelPlusNew.isEmpty())
			return null;
		return getUserPermissionForUserGroupLevel(levelPlusNew, resourceId, permissionType, levelProv);
	}

	public static boolean addPermission(String resourceId, String permissionType, AccessConfigBase configRes) {
		return addPermission(resourceId, permissionType, configRes, 1);
	}
	public static boolean denyPermission(String resourceId, String permissionType, AccessConfigBase configRes) {
		return addPermission(resourceId, permissionType, configRes, 0);
	}
	public static boolean removePermissionSetting(String resourceId, String permissionType, AccessConfigBase configRes) {
		Integer idx = getIndexOfExisting(resourceId, permissionType, configRes);
		if(idx == null)
			return false;
		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ValueResourceUtils.removeElement(configRes.resourceIds(), idx);
		ValueResourceUtils.removeElement(configRes.permissionTypes(), idx);
		ValueResourceUtils.removeElement(configRes.permissionValues(), idx);
		return true;
	}

	/** We have to check whether an existing value has to be overwritten or not
	 * @return true if a new entry was added*/
	public static boolean addPermission(String resourceId, String permissionType, AccessConfigBase configRes, int value) {
		Integer idx = getIndexOfExisting(resourceId, permissionType, configRes);
		if(idx != null) {
			configRes.permissionValues().setElementValue(value, idx);
			return false;
		}
		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		boolean isNew = false;
		if(!configRes.isActive()) {
			configRes.create();
			configRes.resourceIds().create();
			configRes.permissionTypes().create();
			configRes.permissionValues().create();
			isNew = true;
		}
		ValueResourceUtils.appendValue(configRes.resourceIds(), resourceId);
		ValueResourceUtils.appendValue(configRes.permissionTypes(), permissionType);
		ValueResourceUtils.appendValue(configRes.permissionValues(), value);
		if(isNew)
			configRes.activate(true);
		return true;
	}
	
	public static Integer getIndexOfExisting(String resourceId, String permissionType, AccessConfigBase configRes) {
		int idx = 0;
		String[] types = configRes.permissionTypes().getValues();
		for(String id: configRes.resourceIds().getValues()) {
			if(id.contentEquals(resourceId) && types[idx].equals(permissionType)) {
				return idx;
			}
			idx++;
		}
		return null;
	}
	
	public static Integer getPermissionValue(String resourceId, String permissionType, AccessConfigBase configRes) {
		Integer idx = getIndexOfExisting(resourceId, permissionType, configRes);
		if(idx == null) {
			return null;
		}
		return configRes.permissionValues().getElementValue(idx);
	}
	public static Boolean getPermissionStatus(String resourceId, String permissionType, AccessConfigBase configRes) {
		Integer value = getPermissionValue(resourceId, permissionType, configRes);
		if(value == null)
			return null;
		return value > 0;
	}
	
	public static ConfigurablePermission getAccessConfig(AccessConfigUser userAcc, String permissionID) {
		//String userName = userAcc.name().getValue();
		ConfigurablePermission result = new ConfigurablePermission();

		result.accessConfig = userAcc.otherResourcepermissionData(); //.roompermissionData();
		result.resourceId = UserPermissionUtil.SYSTEM_RESOURCE_ID;
		result.permissionId = permissionID;
		//result.defaultStatus = controller.userPermService.getUserSystemPermission(userName,permissionID, true) > 0;
		return result;
	}
	
	public static void setConfigAccess(String userTypeName, String userTypePermissionLabel, boolean value,
			AccessAdminConfig appConfigData) {
		List<AccessConfigUser> all = SubcustomerUtil.getUserGroups(false, true, appConfigData);
		for(AccessConfigUser object: all) {
			if(!object.name().getValue().equals(userTypeName))
				continue;
			PermissionCellData acc = UserPermissionUtil.getAccessConfig(object, userTypePermissionLabel);
			acc.setOwnStatus(value);
		}
	}
	public static float getConfigAccess(String userTypeName, String userTypePermissionLabel,
			AccessAdminConfig appConfigData) {
		List<AccessConfigUser> all = SubcustomerUtil.getUserGroups(false, true, appConfigData);
		int countActive = 0;
		int countBase = 0;
		for(AccessConfigUser object: all) {
			if(!object.name().getValue().equals(userTypeName))
				continue;
			PermissionCellData acc = UserPermissionUtil.getAccessConfig(object, userTypePermissionLabel);
			Boolean val = acc.getOwnstatus();
			if(val == null)
				continue;
			countBase++;
			if(val)
				countActive++;
		}
		if(countBase == 0)
			return 0f;
		return ((float)countActive)/countBase;
	}
	
	public static ConfigurablePermission getUserAppAccessConfig(AccessAdminConfig appConfigData, String permissionID, UserStatus object,
			boolean useWorkingCopy) {
		ConfigurablePermission result = new ConfigurablePermission() {
			@Override
			public void setOwnStatus(Boolean newStatus) {
				super.setOwnStatus(newStatus);
				appConfigData.userStatusPermissionChanged().<BooleanResource>create().setValue(true);
			}
			@Override
			public boolean supportsUnset() {
				return false;
			}
		};
		result.accessConfig = useWorkingCopy?appConfigData.userStatusPermissionWorkingCopy():appConfigData.userStatusPermission(); //userAcc.roompermissionData();
		result.resourceId = object.name();
		result.permissionId = permissionID;
		result.defaultStatus = false; //controller.userPermService.getUserStatusAppPermission(object, permissionID, true) > 0;
		return result;
	}

}
