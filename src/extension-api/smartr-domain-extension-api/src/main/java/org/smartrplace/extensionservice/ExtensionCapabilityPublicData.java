package org.smartrplace.extensionservice;

import java.util.List;

import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;

import de.iwes.widgets.template.LabelledItem;

public interface ExtensionCapabilityPublicData extends LabelledItem {
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
	List<EntryType> getEntryTypes();

}
