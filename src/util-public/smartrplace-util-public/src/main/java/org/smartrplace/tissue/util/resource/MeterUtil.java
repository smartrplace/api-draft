package org.smartrplace.tissue.util.resource;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.metering.ElectricityMeter;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;

import de.iwes.util.resource.ResourceHelper;

public class MeterUtil {
	public static class MainMeterResult {
		ElectricityConnection conn = null;
		ElectricityMeter meter = null;
	}
	
	public static MainMeterResult getMainMeter(ResourceAccess resAcc) {
		MainMeterResult result = new MainMeterResult();
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		if(hwInstall.mainMeter().isReference(false))
			result.conn = hwInstall.mainMeter();
		if(hwInstall.mainMeterAsElMeter().isReference(false))
			result.meter = hwInstall.mainMeterAsElMeter();
		return result ;
	}
	
	public static boolean isMainMeter(ElectricityMeter meter, ResourceAccess resAcc)  {
		MainMeterResult mainAll = getMainMeter(resAcc);
		if(mainAll.meter != null && mainAll.meter.equalsLocation(meter))
			return true;
		return false;
	}
	
	public static boolean isMainMeter(ElectricityConnection conn, ResourceAccess resAcc)  {
		MainMeterResult mainAll = getMainMeter(resAcc);
		if(mainAll.conn != null && mainAll.conn.equalsLocation(conn))
			return true;
		return false;
	}
}
