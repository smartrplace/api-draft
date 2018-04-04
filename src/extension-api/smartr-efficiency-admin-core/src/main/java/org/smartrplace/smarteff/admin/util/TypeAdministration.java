package org.smartrplace.smarteff.admin.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;

public class TypeAdministration {
	public Map<Class<? extends SmartEffExtensionResourceType>, SmartrEffExtResourceTypeData> resourceTypes = new HashMap<>();
	private final SpEffAdminController app;
	
	public TypeAdministration(SpEffAdminController app) {
		this.app = app;
	}

	public void registerService(SmartEffExtensionService service) {
    	for(ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType> rtd: service.resourcesDefined()) {
    		Class<? extends SmartEffExtensionResourceType> rt = rtd.dataType();
    		SmartrEffExtResourceTypeData data = resourceTypes.get(rt);
    		if(data == null) {
    			data = new SmartrEffExtResourceTypeData(rtd, service, app);
    			resourceTypes.put(rt, data );    			
    		} else data.addParent(service);
    	}
		
	}
	
	public void unregisterService(SmartEffExtensionService service) {
     	for(ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType> rtd: service.resourcesDefined()) {
    		Class<? extends SmartEffExtensionResourceType> rt = rtd.dataType();
    		SmartrEffExtResourceTypeData data = resourceTypes.get(rt);
    		if(data == null) {
    			//should not occur
    			app.log.error("Resource type "+rt.getName()+" not found when service "+SmartrEffUtil.buildId(service)+ "unregistered!");
    		} else if(data.removeParent(service)) resourceTypes.remove(rt);
    	}
	}

	/*@SuppressWarnings("unchecked")
	public List<EntryType> getStandardEntryTypeList(Class<? extends ExtensionResourceType>... types) {
		List<EntryType> result = new ArrayList<>();
		for(Class<? extends ExtensionResourceType> t: types) {
			EntryType r = getEntryType(resourceTypes.get(t).typeDeclaration);
			result.add(r);
		}
		return result ;
	}*/
	
	public void registerElement(ExtensionResourceType res) {
		SmartrEffExtResourceTypeData data = resourceTypes.get(res.getResourceType());
		if(data == null) return;
		data.registerElement(res);
	}
	
	public void unregisterElement(SmartEffExtensionResourceType res) {
		SmartrEffExtResourceTypeData data = resourceTypes.get(res.getResourceType());
		if(data == null) return;
		data.unregisterElement(res);
	}
}
