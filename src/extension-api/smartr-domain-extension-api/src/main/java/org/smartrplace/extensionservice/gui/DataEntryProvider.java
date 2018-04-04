package org.smartrplace.extensionservice.gui;

import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceType;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** The data entry provider does not create new resources, this is done by the main domain
 * app. It does create sub resources and it does write to them. Activation is done in the main
 * domain app
 * (to be discussed)
 * The main domain app must also provide a WidgetPage reference defining the URL. To open the
 * edit page for 
 */
@Deprecated
public interface DataEntryProvider<T extends ExtensionResourceType> extends ExtensionCapability {
	
	/** Initialize page for display and editing of a single instane of the resource type to be edited
	 * 
	 * @param page page with URL that was generated by main domain application
	 * @param generalData usually there is some data that does not depend on a parent that shall always be
	 * 		accessible. This information should be handed over here.
	 * @param appManMin
	 */
	void initPage(final ExtensionResourceEditPage<T> page, ExtensionResourceType generalData);
	/** Note that a different page instance has to be provided from the instance used with
	 * initPage
	 */
	//TODO: A suitable table page with user-based init has to be defined 
	//void initOverviewTable(final WidgetPage<?> page, ExtensionResourceType generalData, ResourceList<BuildingData> buildings);
	
	/**If true the method initPage shall be called and the respective URL
	 * should be called with a parameter configId indicating the location of the resource
	 * to edit
	 */
	boolean providesSingleEditPage();
	/**If true the method initOverviewTable shall be called to instantiate the page
	 */
	boolean providesOverviewTable();
	
	/** For each new session the relevant user data is provided with this method
	 * 
	 * @param userData domain-specific reference to user data. May also be obtainable just as parent of the resource.
	 * 		If the data for each user is stored in a separate top-level resource the provider cannot access the
	 * 		data of other users during the editing process. So a code review would just have to check that the
	 * 		implementing class has no class member variables and the bundle has no static non-final elements.
	 * 	 TODO: To be discussed: Should we instantiate a new class provided by the service each time we perform a
	 * 		new edit action of for each user? Then we would just have to forbid static non-final declarations in
	 * 		the extension bundle (which can be done quite easily during a code review, even an automated code review).
	 * @param listener when the user presses a "Save" button or finishes editing otherwise, finishing of editing
	 *		 shall be notified to the main domain app so that it can activate resources etc. 
	 */
	void setUserData(ExtensionResourceType userData,
			ExtensionPageSystemAccessForCreate listener, OgemaHttpRequest req);	
	
	/** When a resource is newly created or after restart of extension bundle (may be emulated by re-init every some hours)
	 * this method is called on any destination resource before it is edited
	 * @param resourceToUpdateInit
	 */
	void initResource(T resourceToUpdateInit);
	
	/** Resource Type to be edited
	 */
	Class<T> getResourceTypeToEdit();
}