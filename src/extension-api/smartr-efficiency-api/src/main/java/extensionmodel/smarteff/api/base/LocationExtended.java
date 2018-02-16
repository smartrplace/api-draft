package extensionmodel.smarteff.api.base;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.locations.Location;

public interface LocationExtended extends Location{
	/**TODO*/
	IntegerResource country();
	
	StringResource state();
	
	StringResource postalCode();
	
	StringResource city();
	
	StringResource street();
	
	StringResource number();
}
