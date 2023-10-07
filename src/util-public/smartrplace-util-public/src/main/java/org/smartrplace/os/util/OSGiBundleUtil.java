package org.smartrplace.os.util;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class OSGiBundleUtil {
	public enum BundleType {
		HomematicDriver,
		MdnsService,
		WMBusDriver,
		ModbusDriver,
		Gateway,
		MQTTReplicator
	}
	public static Map<BundleType, String> typeSymbolicNames = new HashMap<>();
	static {
		typeSymbolicNames.put(BundleType.HomematicDriver, "org.ogema.drivers.homematic-xmlrpc-hl");
		typeSymbolicNames.put(BundleType.MdnsService, "org.smartrplace.drivers.jmdns-service");
		typeSymbolicNames.put(BundleType.WMBusDriver, "org.smartrplace.drivers.jmbus-connector");
		typeSymbolicNames.put(BundleType.Gateway, "System Bundle");
		typeSymbolicNames.put(BundleType.MQTTReplicator, "org.smartrplace.drivers.mqtt-resource-replicator");
	}
	
	
	public static void stopBundleById(long id, BundleContext bc) {
		for(Bundle bd: bc.getBundles()) {
			if(bd.getBundleId() == id) {
				try {
System.out.println("Stopping "+bd.getSymbolicName()+" ...");
					bd.stop();
System.out.println("Stopping of "+bd.getSymbolicName()+" done.");
				} catch (BundleException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** Get bundle symbolic name on console with lb -s*/
	public static void restartBundle(String bundleSymbolicName, BundleContext bc) {
		for(Bundle bd: bc.getBundles()) {
			if(bd.getSymbolicName().equals(bundleSymbolicName)) {
				try {
System.out.println("Restarting "+bd.getBundleId()+" ...");
					bd.stop();
					bd.start();
System.out.println("Restart of "+bd.getBundleId()+" done.");
				} catch (BundleException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String restartBundle(BundleType type, BundleContext bc) {
		String symbolicName = typeSymbolicNames.get(type);
		if(symbolicName == null)
			return null;
		restartBundle(symbolicName, bc);
		return symbolicName;
	}
}
