package org.smartrplace.extenservice.resourcecreate;

import java.util.Collections;
import java.util.List;

import org.smartrplace.extensionservice.ExtensionCapabilityPublicData;
import org.smartrplace.extensionservice.ExtensionResourceType;

public interface ProviderPublicDataForCreate extends ExtensionCapabilityPublicData {
	/** Types that can be created by the GUI provider. Note that further classes may be edited. Usually this
	 * should either be an empty list or a list containing a single element. If more than one element is give
	 * the creator may choose one of the options to create based on the entry data of the actual request or
	 * may create all of the types declared with one editing process. This is organized by one of the 
	 * variants of {@link ExtensionPageSystemAccessForCreate#getNewResource(ExtensionResourceType)*/
	default List<Class<? extends ExtensionResourceType>> createTypes() {
		return Collections.emptyList();
	};
}