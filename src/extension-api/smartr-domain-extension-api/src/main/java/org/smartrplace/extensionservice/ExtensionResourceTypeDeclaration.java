package org.smartrplace.extensionservice;

public interface ExtensionResourceTypeDeclaration<T extends ExtensionResourceType> {
	/**Resource type required by app module to store its persistent data*/
	Class<? extends T> resourceType();
	/**Name of the element or the resource list. The name needs to be unique among the sub elements of the
	 * parent type. If null returned the name is chosen automatically (to be tested if a good idea to be
	 * defined as default).
	 */
	String resourceName();
	
	/**Super type to which new resource or resource list shall be applied. The element shall be 
	 * created as decorator if not (yet) defined as regular element. If the extension element is
	 * accepted as standard extension the respective element should be added in the parent tpye
	 * definition.
	 */
	Class<? extends T> parentType();
	
	public enum Cardinality {
	
	/**Maximal one subresource, do not create if not exists*/
	SINGLE_VALUE_OPTIONAL,
	/**Exactly one item required, should be created if not existing yet*/
	SINGLE_VALUE_REQUIRED,
	/** Create resource list*/
	MULTIPLE_OPTIONAL,
	MULTIPLE_REQUIRED
	
	}

	Cardinality cardinality();
}
