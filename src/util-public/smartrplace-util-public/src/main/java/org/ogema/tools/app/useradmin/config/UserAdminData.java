package org.ogema.tools.app.useradmin.config;

import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Data;
import org.ogema.model.user.NaturalPerson;

public interface UserAdminData extends Data {
	ResourceList<NaturalPerson> userData();
	
	ResourceList<RESTUserData> restUserData();
}
