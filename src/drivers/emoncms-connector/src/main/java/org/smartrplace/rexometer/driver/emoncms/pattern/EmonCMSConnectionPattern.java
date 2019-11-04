package org.smartrplace.rexometer.driver.emoncms.pattern;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.rexometer.driver.emoncms.model.EmonCMSConnection;
import org.smartrplace.rexometer.driver.emoncms.model.EmonCMSReadConfiguration;
import org.smartrplace.rexometer.driver.emoncms.model.EmonCMSWriteConfiguration;

public class EmonCMSConnectionPattern extends ResourcePattern<EmonCMSConnection> {

	@Existence(required=CreateMode.OPTIONAL)
	public final ResourceList<EmonCMSReadConfiguration> readConfiguraions = model.readConfigurations();

	@Existence(required=CreateMode.OPTIONAL)
	public final ResourceList<EmonCMSWriteConfiguration> inputConfiguraions = model.inputConfigurations();

	public final StringResource url = model.url();

	@Existence(required=CreateMode.OPTIONAL)
	public final StringResource apiKeyRead = model.apiKeyRead();

	@Existence(required=CreateMode.OPTIONAL)
	public final StringResource apiKeyWrite = model.apiKeyWrite();
	

	public EmonCMSConnectionPattern(Resource match) {
		super(match);
	}

}
