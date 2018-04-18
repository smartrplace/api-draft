package org.smartrplace.smarteff.util.button;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public interface ButtonControlProvider {
	default boolean openInNewTab(OgemaHttpRequest req) {return false;}
}
