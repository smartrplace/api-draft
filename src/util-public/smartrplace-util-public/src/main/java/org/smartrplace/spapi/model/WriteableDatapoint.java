package org.smartrplace.spapi.model;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.PhysicalElement;

public interface WriteableDatapoint extends PhysicalElement {
	/** Registered datapoint location. Note that the resource for savings is different
	 * from the resource acually used
	 */
	StringResource datapointLocation();
	
	/** Reference to resource savings the datapoint data
	 */
	SingleValueResource resource();
	BooleanResource disableLogging();
	
	/** Human readable name to be provided as label*/
	@Override
	StringResource name();
	
	/** Room, device or gateway device the writeable datapoint is assigned to*/
	PhysicalElement deviceAssigned();
	//StringResource alarmType();
}
