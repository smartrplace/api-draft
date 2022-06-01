package org.smartrplace.os.access.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;

public interface CCUAccessI {
	/**
	 * Get the addresses of all devices lost on all known CCUs
	 *
	 * @return List of addresses for devices known to RFD daemon.
	 */
	public List<String> getLostDevices();
	
	/**
	 * Get the addresses of all devices known to the RFD process on the given
	 * CCU.
	 *
	 * @param iface
	 * @return List of addresses for devices known to RFD daemon.
	 * @throws IOException
	 */
	public List<String> listRfdDevices(HmLogicInterface iface) throws IOException;

	/** Delete a faulty entry
	 * 
	 * @param iface
	 * @param addr
	 * @return the exit value of the subprocess represented by this Process object. By convention, the value 0 indicates normal termination.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public int deleteRfdDevice(HmLogicInterface iface, String addr) throws IOException, InterruptedException;

	public static class HomematicConnectionData {
		public String connUrl;
		public boolean isConnected;	
	}
	
	public Map<HmLogicInterface, HomematicConnectionData> getConnectionsData();

}
