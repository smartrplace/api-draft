package org.ogema.util.controllerprovider;

/**
 * 
 * @author dnestle
 *
 * @param <C> controller class
 */
public interface GenericExtensionProvider<C> {
	void setController(C controller);
}
