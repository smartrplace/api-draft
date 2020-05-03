package org.ogema.devicefinder.api;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.schedule.Schedule;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.widgets.template.LabelledItem;

/** Description of a resource that may be located on the local OGEMA instance or somewhere else*. The
 * id usually equals getLocation./
 */
public interface GatewayResource extends LabelledItem {
	/** Get the resource location. In case the resource location is on the local system usually the
	 * method {@link #getResource()} should return the resource on the location. If the location is on another
	 * gateway or the location refers to a path that does not exist anymore then getResource will
	 * return null.
	 * @return
	 */
	String getLocation();
	
	/** If the location is on the local gateway return null or {@link GaRoMultiEvalDataProvider#LOCAL_GATEWAY_ID},
	 * otherwise return the id of the gateway to which the resource location belongs.
	 * @return
	 */
	String getGatewayId();
	
	/** See {@link #getLocation()}
	 * @return usually this should be resource of type {@link ValueResource} or {@link Schedule}.
	 */
	Resource getResource();
}
