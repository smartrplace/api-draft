package org.smartrplace.smarteff.admin.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.ResourceException;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

public class NavigationPageSystemAccess implements ExtensionPageSystemAccessForCreate {
	private final String userName;
	private final String applicationName;
	private final Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> pageInfo;
	private final ResourceLockAdministration lockAdmin;
	private final ConfigIdAdministration configIdAdmin;
	
	public NavigationPageSystemAccess(String userName, String applicationName,
			Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> pageInfo,
			ResourceLockAdministration lockAdmin, ConfigIdAdministration configIdAdmin) {
		this.userName = userName;
		this.applicationName = applicationName;
		this.pageInfo = pageInfo;
		this.lockAdmin = lockAdmin;
		this.configIdAdmin = configIdAdmin;
	}

	@Override
	public List<NavigationPublicPageData> getPages(Class<? extends ExtensionResourceType> type) {
		List<NavigationPublicPageData> result = pageInfo.get(type);
		if(result == null) return Collections.emptyList();
		return result;
	}
	
	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx,
			List<ExtensionResourceType> entryResources) {
		return configIdAdmin.getConfigId(entryIdx, entryResources);
	}

	@Override
	public String accessCreatePage(NavigationPublicPageData pageData, int entryIdx,
			ExtensionResourceType parent) {
		Class<? extends ExtensionResourceType> type = pageData.getEntryType().get(entryIdx).getType();
		String name = getnewDecoratorName(type.getSimpleName(), parent);
		NewResourceResult<? extends ExtensionResourceType> newResource = getNewResource(parent, name, type);
		if(newResource.result != ResourceAccessResult.OK) {
			System.out.println("Error while trying to create "+parent.getLocation()+"/"+name+": "+newResource.result);
			return null;			
		}
		List<ExtensionResourceType> entryResources = Arrays.asList(new ExtensionResourceType[] {newResource.newResource});
		return accessPage(pageData, entryIdx, entryResources );
	}
	
	@Override
	public LockResult lockResource(ExtensionResourceType resource) {
		if(!checkAllowed(resource)) return null;
		return lockAdmin.lockResource(resource, userName, applicationName);
	}

	@Override
	public void unlockResource(ExtensionResourceType resource, boolean activate) {
		if(!checkAllowed(resource)) return;
		if(activate) {
			resource.activate(true);
		}
		lockAdmin.unlockResource(resource);
	}

	@Override
	public <T extends ExtensionResourceType> NewResourceResult<T> getNewResource(ExtensionResourceType parent,
			String name, Class<T> type) {
		try {
			T res = parent.getSubResource(name, type);
			return getNewResource(res);
		} catch(ResourceException e) {
			NewResourceResult<T> result = new NewResourceResult<>();
			result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS_DIFFENT_TYPE;
			return result;			
		}
	}

	@Override
	public <T extends ExtensionResourceType> NewResourceResult<T> getNewResource(T virtualResource) {
		NewResourceResult<T> result = new NewResourceResult<>();
		if(!checkAllowed(virtualResource)) {
			result.result = ResourceAccessResult.NOT_ALLOWED;
			return result;
		}
		if(virtualResource.exists()) {
			result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS;
			return result;
		}
		virtualResource.create();
		result.result = ResourceAccessResult.OK;
		result.newResource = virtualResource;
		return result;
	}

	@Override
	public void activateResource(ExtensionResourceType resource) {
		unlockResource(resource, true);
	}
	
	private boolean checkAllowed(ExtensionResourceType resource) {
		String[] els = resource.getLocation().split("/", 2);
		if(els.length == 0) throw new IllegalStateException("Resource location should not be empty!");
		if(els[0].equals(userName)) return true;
		return false;
	}
	
	private static String getnewDecoratorName(String baseName, Resource parent) {
		String name = baseName;
		int i=0;
		while(parent.getSubResource(name) != null) {
			i++;
			name = baseName+"_"+i;
		}
		return name;
	}

}
