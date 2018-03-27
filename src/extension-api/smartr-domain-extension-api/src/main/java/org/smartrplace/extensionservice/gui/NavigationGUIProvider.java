package org.smartrplace.extensionservice.gui;

import java.util.List;

import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionDoneListener;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** A navigation page is an overview table or any other web page representation that allows to navigate
 * between views and that can initiate editing or creating of resources
 */
public interface NavigationGUIProvider extends ExtensionCapability {
	
	/** Initialize navigation page
	 * 
	 * @param page page with URL that was generated by main domain application
	 * @param generalData usually there is some data that does not depend on a parent that shall always be
	 * 		accessible. This information should be handed over here.
	 * @param appManMin
	 */
	void initPage(final ExtensionNavigationPage page, ExtensionResourceType generalData);
	
	/** For each new session the relevant user data is provided with this method
	 * 
	 * @param entryTypeIdx index within {@link #getEntryType()} used to open the page
	 * @param entryResources resources of the entry type specified by entryTypeIdx. If the cardinality of
	 * 		the EntryType does not allow multiple entries the list will only contain a single element. If
	 * 		the cardinality allows zero the list may be empty.
	 * @param userData domain-specific reference to user data. May also be obtainable just as parent of the resource.
	 * @param listener when the user presses a "Save" button or finishes editing otherwise, finishing of editing
	 *		 shall be notified to the main domain app so that it can activate resources etc. 
	 */
	void setUserData(int entryTypeIdx, List<ExtensionResourceType> entryResources, ExtensionResourceType userData,
			ExtensionResourceType userDataNonEdit,
			ExtensionDoneListener<ExtensionResourceType> listener, OgemaHttpRequest req);	
	
	public static interface EntryType {
		Class<? extends ExtensionResourceType> getType();
		/** The standard cardinality is SINGLE_VALUE_REQUIRED. If _OPTIONAL is specified the
		 * navigator must be able to search by itself for suitable data in the userData or generalData.
		 */
		Cardinality getCardinality();
	}
	
	/** Any resource of any entry type shall be sufficient to open the page. Only a single entry type is used to
	 * open a page, but if the cardinality of the type allows it more than one element may be submitted.
	 * If this is null the page is a start page.
	 */
	List<EntryType> getEntryType();
}
