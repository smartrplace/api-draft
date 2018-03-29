package org.smartrplace.smarteff.admin.protect;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceException;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.ResourceLockAdministration;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.smarteff.defaultservice.CapabilityHelper;
import org.smartrplace.util.format.ValueFormat;

public class NavigationPageSystemAccess implements ExtensionPageSystemAccessForCreate {
	private final String userName;
	private final String applicationName;
	private final Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> pageInfo;
	private final ResourceLockAdministration lockAdmin;
	private final ConfigIdAdministration configIdAdmin;
	private final ApplicationManagerSPExt appExt;
	private final TypeAdministration typeAdmin;
	
	public NavigationPageSystemAccess(String userName, String applicationName,
			Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> pageInfo,
			ResourceLockAdministration lockAdmin, ConfigIdAdministration configIdAdmin,
			TypeAdministration typeAdmin,
			ApplicationManagerSPExt appExt) {
		this.userName = userName;
		this.applicationName = applicationName;
		this.pageInfo = pageInfo;
		this.lockAdmin = lockAdmin;
		this.configIdAdmin = configIdAdmin;
		this.typeAdmin = typeAdmin;
		this.appExt = appExt;
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
		Class<? extends ExtensionResourceType> type = pageData.getEntryTypes().get(entryIdx).getType();
		ExtensionResourceTypeDeclaration<? extends ExtensionResourceType> typeDecl = appExt.getTypeDeclaration(type);
		String name = CapabilityHelper.getnewDecoratorName(ValueFormat.firstLowerCase(type.getSimpleName()), parent);
		NewResourceResult<? extends ExtensionResourceType> newResource = getNewResource(parent, name, typeDecl);
		if(newResource.result != ResourceAccessResult.OK) {
			System.out.println("Error while trying to create "+parent.getLocation()+"/"+name+": "+newResource.result);
			return CapabilityHelper.ERROR_START+newResource.result;			
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
	public boolean isLocked(ExtensionResourceType resource) {
		return lockAdmin.isLocked(resource);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtensionResourceType> NewResourceResult<T> getNewResource(ExtensionResourceType parentIn,
			String name, ExtensionResourceTypeDeclaration<T> type) {
		if(isMulti(type.cardinality())) {
			ResourceList<T> parent = null;
			if(parentIn instanceof ResourceList) {
				parent = (ResourceList<T>) parentIn;
				if(!parent.isActive()) parent.create().activate(false);
			} else {
				name = ValueFormat.firstLowerCase(type.dataType().getSimpleName());
				for(ResourceList<?> rl: parentIn.getSubResources(ResourceList.class, false)) {
					if(rl.getElementType() == null) continue;
					if(rl.getElementType().isAssignableFrom(type.dataType())) {
						parent = (ResourceList<T>) rl;
						if(!parent.getName().equals(name))
							appExt.log().error("Name of ResourceList for "+type.dataType().getName()+" is not "+name);
						break;
					}
				}
				if(parent == null) {
					try {
						parent = parentIn.getSubResource(name, ResourceList.class);
						parent.create();
						parent.setElementType(type.dataType());
						parent.activate(false);
					} catch(ResourceException e) {
						NewResourceResult<T> result = new NewResourceResult<>();
						result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS_DIFFENT_TYPE;
						return result;			
					}
					
				}
			}
			String elName = CapabilityHelper.getnewDecoratorName("E", parent);
			//We add as decorator here as type may be inherited class
			T res = parent.getSubResource(elName, type.dataType());
			return getNewResource(res);
		}
		ExtensionResourceType parent = parentIn;
		List<? extends T> existing = parent.getSubResources(type.dataType(), false);
		if(!existing.isEmpty()) {
			NewResourceResult<T> result = new NewResourceResult<>();
			result.result = ResourceAccessResult.SINGLE_RESOURCETYPE_ALREADY_EXISTS;
			return result;						
		}
		try {
			T res = (T) parentIn.getSubResource(name, type.dataType());
			return getNewResource(res);
		} catch(ResourceException e) {
			NewResourceResult<T> result = new NewResourceResult<>();
			result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS_DIFFENT_TYPE;
			return result;			
		}
	}
	
	private boolean isMulti(Cardinality card) {
		if(card == Cardinality.MULTIPLE_OPTIONAL || card == Cardinality.MULTIPLE_REQUIRED) return true;
		return false;
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
		typeAdmin.registerElement(virtualResource);
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
}
