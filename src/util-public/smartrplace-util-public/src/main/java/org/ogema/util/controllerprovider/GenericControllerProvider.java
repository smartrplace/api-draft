package org.ogema.util.controllerprovider;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author dnestle
 *
 * @param <C> controller class
 */
public class GenericControllerProvider<C> {
	protected final List<String> acceptedClassNames;
	private volatile C controller;
	
	private final Map<String, GenericExtensionProvider<C>> extProvidersOpen =
			Collections.synchronizedMap(new LinkedHashMap<String, GenericExtensionProvider<C> >());
	public Map<String, GenericExtensionProvider<C>> getExtProviders() {
		synchronized (extProvidersOpen) {
			return new LinkedHashMap<>(extProvidersOpen);
		}
	}

	public GenericControllerProvider(String acceptedClassName) {
		this.acceptedClassNames = Arrays.asList(new String[] {acceptedClassName});
	}
	public GenericControllerProvider(String... acceptedClassNames) {
		this.acceptedClassNames = Arrays.asList(acceptedClassNames);
	}
	public GenericControllerProvider(List<String> acceptedClassNames) {
		this.acceptedClassNames = acceptedClassNames;
	}

	/** Call this method as soon as controller as been set up by the providing application*/
	public void setController(C controller) {
		synchronized (extProvidersOpen) {
			this.controller = controller;
			for(GenericExtensionProvider<C> p: extProvidersOpen.values()) {
				p.setController(controller);
				System.out.println("SetController done for "+p.getClass());
			}
			extProvidersOpen.clear();
		}
	}
	
	public void addExtProvider(GenericExtensionProvider<C> provider) {
		if(!acceptedClassNames.contains(provider.getClass().getName()))
    		return;
		synchronized (extProvidersOpen) {
	    	if(controller != null) {
	    		provider.setController(controller);
	    	} else
	        	extProvidersOpen.put(provider.getClass().getName(), provider);
		}
    }
    public void removeExtProvider(GenericExtensionProvider<C> provider) {
    	extProvidersOpen.remove(provider.getClass().getName());
    }

}
