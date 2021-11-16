package org.ogema.accessadmin.api;

import org.smartrplace.external.accessadmin.config.AccessConfigBase;

public class ConfigurablePermission implements PermissionCellData {
	public String resourceId;
	public String permissionId;
	public AccessConfigBase accessConfig;
	public boolean defaultStatus;
	//ResourceList<AccessConfigUser> userPerms;
	//String userName;
	//boolean supportsUnset = true;
	
	@Override
	public Boolean getOwnstatus() {
		return UserPermissionUtil.getPermissionStatus(resourceId, permissionId, accessConfig);
	}
	@Override
	public void setOwnStatus(Boolean newStatus) {
		if(newStatus == null) {
			UserPermissionUtil.removePermissionSetting(resourceId, permissionId, accessConfig);				
		} else {
			UserPermissionUtil.addPermission(resourceId, permissionId, accessConfig,
					newStatus==null?null:(newStatus?1:0));
		}
	}
	@Override
	public boolean getDefaultStatus() {
		return defaultStatus;
	}
	@Override
	public boolean supportsUnset() {
		return true;
	}
	
	public ConfigurablePermission() {
	}
}