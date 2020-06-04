package org.ogema.devicefinder.api;

public interface DocumentationLinkProvider {
	
	/** Get link to driver documentation page. Usually this is an absolute URL
	 * 
	 * @param publicVersion if true a page accessible publicly for all users shall be returned. Note that some
	 * 		more granularity in the access level will be introduced soon
	 * @return null if no fitting documentation page is available
	 */
	String getDriverDocumentationPageURL(boolean publicVersion);
	
}
