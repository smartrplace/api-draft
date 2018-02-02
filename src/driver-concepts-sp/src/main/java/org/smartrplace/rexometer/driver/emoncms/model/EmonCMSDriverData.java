package org.smartrplace.rexometer.driver.emoncms.model;

import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Configuration;

public interface EmonCMSDriverData extends Configuration {

	 ResourceList<EmonCMSConnection> connections();

}

