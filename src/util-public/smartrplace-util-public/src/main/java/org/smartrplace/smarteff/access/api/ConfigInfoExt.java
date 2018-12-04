package org.smartrplace.smarteff.access.api;

import java.util.List;

import org.ogema.core.model.Resource;

public class ConfigInfoExt {
	public ConfigInfoExt(int entryIdx, List<Resource> entryResources) {
		this.entryIdx = entryIdx;
		this.entryResources = entryResources;
	}
	public int entryIdx;
	public List<Resource> entryResources;
	
	public Resource lastPrimaryResource;
	public Object lastContext;
	
	public Object context; 
}
