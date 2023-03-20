package org.smartrplace.tissue.util.resource;

import org.ogema.core.model.Resource;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.model.sync.mqtt.GatewaySyncData;
import org.smartrplace.os.util.OSGiBundleUtil.BundleType;

/** Note: This service shall use the fields in {@link GatewaySyncData#commandsGwToServer()} etc.
 * It shall determine itself whether it is running on server or gateway by properties and in
 * some cases by arguments.<br>
 * The property org.smartplace.app.srcmon.server.issuperior indicates the main superior instance,
 * which is always server.
 * Otherwise if the property org.ogema.devicefinder.util.supportcascadedccu is NOT set true
 * we always assume the system acting as gateway. 
 * If neither condition applies then the destination has to be determined by the position in
 * the cascading tree. The Cascading trees are defined in {@link GatewaySyncData#toplevelResourcesToBeSynchronized()}
 * for each cascading connection.
 */
public interface GatewaySyncResourceService {
	/** Activate resource locally and the respective resource on the other side of cascading if the
	 * resource is inside a cascading tree.
	 * @param res
	 * @param recursive
	 * @return
	 */
	boolean activateResource(Resource res, boolean recursive);
	boolean deactivateResource(Resource res, boolean recursive);
	boolean delete(Resource res);
	/** TODO */
	Resource create();
	
	Resource addToResourceList();
	
	boolean setRoomAsReference(PhysicalElement device, Room Room);
	boolean setDeviceAsReference(PhysicalElement newReference, PhysicalElement deviceToBeReferenced);
	
	boolean restartBundle(BundleType type);
	boolean restartBundle(String bundleSymbolicName);
}
