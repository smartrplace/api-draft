package org.smartrplace.tissue.util.resource;

import org.ogema.core.model.Resource;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
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
 * for each cascading connection. Whether a GatewaySyncData entry is relevant for you as client or as
 * server can be determined based on the own gatewayId. This can be determined by
 * {@link GatewayUtil#getGatewayId(org.ogema.core.resourcemanager.ResourceAccess)},
 * to make sure got get only 5-digit-id process the result by {@link ViaHeartbeatUtil#getBaseGwId(String)}.
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
	
	/** Room must be identified by name*/
	boolean setRoomAsReference(PhysicalElement device, Room Room);
	boolean setDeviceAsReference(PhysicalElement newReference, PhysicalElement deviceToBeReferenced);
	
	boolean restartBundle(BundleType type, String gatewayId);
	boolean restartBundle(String bundleSymbolicName, String gatewayId);
}
