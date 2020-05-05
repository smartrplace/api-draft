package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;

public interface PatternListenerExtended<P extends ResourcePattern<R>, R extends Resource> extends PatternListener<P> { 
	public List<P> getAllPatterns();
}
