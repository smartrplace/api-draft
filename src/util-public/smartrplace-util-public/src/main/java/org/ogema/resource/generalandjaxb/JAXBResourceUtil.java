package org.ogema.resource.generalandjaxb;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.util.DeviceTableRaw.SubResourceInfo;
import org.ogema.serialization.jaxb.Resource;
import org.ogema.serialization.jaxb.ResourceLink;

public class JAXBResourceUtil {
	public static List<SubResourceInfo> getSubResourceInfo(Resource model) {
		List<Object> ress = model.getSubresources();
		List<SubResourceInfo> result = new ArrayList<>();
		for(Object res: ress) {
			if (res instanceof ResourceLink) { 
				ResourceLink r = (ResourceLink) res;
				result.add(new SubResourceInfo(r.getName(), r.getType()));
			} else if(res instanceof Resource) {
				Resource r = (Resource) res;
				result.add(new SubResourceInfo(r.getName(), r.getType()));				
			}
		}
		return result ;
	}
}
