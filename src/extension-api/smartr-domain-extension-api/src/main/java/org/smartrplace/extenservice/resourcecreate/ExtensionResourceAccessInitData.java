package org.smartrplace.extenservice.resourcecreate;

import java.util.List;

import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

public interface ExtensionResourceAccessInitData {
	/** index within {@link #getEntryType()} used to open the page*/
	int entryTypeIdx();
	
	/** resources of the entry type specified by entryTypeIdx. If the cardinality of
	 * 		the EntryType does not allow multiple entries the list will only contain a single element. If
	 * 		the cardinality allows zero the list may be empty.
	 */
	List<ExtensionResourceType> entryResources();

	/**Domain-specific reference to user data.
	 */
	ExtensionResourceType userData();
	
	/** User data than cannot be edited by the user*/
	ExtensionUserDataNonEdit userDataNonEdit();
	
	/** Access for module for resource creation process*/
	ExtensionPageSystemAccessForCreate systemAccess();
}
