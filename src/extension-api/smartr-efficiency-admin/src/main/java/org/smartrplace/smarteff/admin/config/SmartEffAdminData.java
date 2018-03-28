package org.smartrplace.smarteff.admin.config;

import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Data;

import extensionmodel.smarteff.api.base.SmartEffGeneralData;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

/** Data that is accessible for all users.*/
public interface SmartEffAdminData extends Data {
	/**References to user data*/
	ResourceList<SmartEffUserData> userData();
	
	/** TODO: This is probably not safe against access to other user data as the elements are currently
	 * not references here.
	 */
	ResourceList<SmartEffUserDataNonEdit> userDataNonEdit();
	
	/**General data visible to apps*/
	SmartEffGeneralData generalData();
}
