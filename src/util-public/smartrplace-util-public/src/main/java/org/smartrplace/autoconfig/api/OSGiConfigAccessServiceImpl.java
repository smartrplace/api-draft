package org.smartrplace.autoconfig.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class OSGiConfigAccessServiceImpl implements OSGiConfigAccessService {

	//private final ApplicationManagerPlus appMan;
	private final ConfigurationAdmin configAdmin;

	public OSGiConfigAccessServiceImpl(ConfigurationAdmin configAdmin) {
		//this.appMan = appMan;
		this.configAdmin = configAdmin;
	}

	Map<String, OSGiConfiguration> configObjects = new HashMap<>();
	
	@Override
	public OSGiConfiguration getConfiguration(String pid) {
		try {
			OSGiConfiguration result = configObjects.get(pid);
			if(result != null)
				return result;
			Configuration config = configAdmin.getConfiguration(pid);
			if(config.getProperties() == null)
				return null;
			
			result = new OSGiConfigurationImpl(config);
			
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<String> getFactoryPids(String mainPid) {
		List<String> all = getMainPidsConfigured(false);
		List<String> result = new ArrayList<>();
		for(String c: all) {
			String[] els = c.split("~", 2);
			if(els.length == 2 && els[0].equals(mainPid))
				result.add(c);
		}
		return result;
	}

	@Override
	public boolean deleteConfiguration(OSGiConfiguration config) {
		throw new UnsupportedOperationException("Delete not implemented yet!");
	}

	@Override
	public List<String> getMainPidsConfigured(boolean instantiatedOnly) {
		try {
			Configuration[] all = configAdmin.listConfigurations(null);
			List<String> result = new ArrayList<>();
			for(Configuration c: all) {
				result.add(c.getPid());
			}
			return result;
		} catch (IOException | InvalidSyntaxException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

}
