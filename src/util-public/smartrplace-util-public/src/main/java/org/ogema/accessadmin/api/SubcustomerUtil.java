package org.ogema.accessadmin.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.locations.Room;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.SubCustomerData;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class SubcustomerUtil {
	
	/** The ids are stored in resources persistently, so on a certain system the meaning of each integer
	 * value shall not be changed, although the exact name texts can be adapted.
	 */
	public static class SubCustomerType extends NamedIntegerType {
		/**Id is written into the {@link Room#type()} field and should this be aligned with the
		 * standard room types if possible
		 */
		public final Map<Integer, NamedIntegerType> roomTypes = new HashMap<>();
		public int[] defaultWorkingDays = new int[] {1,2,3,4,5};
		public float defaultEcoTemperatureHeating = 273.15f+16f;
		public float defaultEcoTemperatureCooling = 273.15f+30f;
		
		public SubCustomerType(int id, Map<OgemaLocale, String> name) {
			super(id, name);
		}

		public SubCustomerType(int id, String englishName) {
			super(id, englishName);
		}
		
		public SubCustomerType(int id, String englishName, String germanName) {
			super(id, englishName, germanName);
		}
		
		public void addRoomType(NamedIntegerType roomType) {
			roomTypes.put(roomType.id, roomType);
		}
	}
	public static Map<Integer, SubCustomerType> subCustomerTypes = new HashMap<>();
	
	/**
	 * 
	 * @param type
	 * @return true if an existing entry with a different name was overwritten
	 */
	public static boolean addCustomerType(SubCustomerType type) {
		SubCustomerType prev = subCustomerTypes.put(type.id, type);
		return (prev != null)&&(!prev.label(null).equals(type.label(null)));
	}

	public static SubCustomerData addSubcustomer(int type, String name, ApplicationManager appMan) {
		SubCustomerType typeData = subCustomerTypes.get(type);
		if(typeData == null)
			throw new IllegalStateException("Type data for "+type+" not registered before trying to use it!");
		
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		SubCustomerData result = ResourceListHelper.getOrCreateNamedElementFlex(
				name, accessAdminConfigRes.subCustomers(), SubCustomerData.class);
		ValueResourceHelper.setCreate(result.subCustomerType(), type);
		ValueResourceHelper.setCreate(result.ecoModeActive(), false);
		ValueResourceHelper.setCreate(result.workingDays(), typeData.defaultWorkingDays);
		ValueResourceHelper.setCreate(result.defaultEcoTemperatureHeating(), typeData.defaultEcoTemperatureHeating);
		ValueResourceHelper.setCreate(result.defaultEcoTemperatureCooling(), typeData.defaultEcoTemperatureCooling);
		
		return result;
	}
	public static SubCustomerData getSubcustomer(String name, ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		return ResourceListHelper.getNamedElementFlex(
				name, accessAdminConfigRes.subCustomers());
	}
	
	public static class RoomTypeGroupData {
		public String id;
		public String name;
		public List<String> rooms = new ArrayList<>();
		public int roomNumHeating;
		public int roomNumCooling;
		public HeatCoolData heatingData;
		public HeatCoolData coolingData;
	}
	public static class HeatCoolData {
		/** get general heating start time in */
		public long startTime;
		public long endTime;
		public float usageTemperature;
		public float nonUsageTemperature;
		public float comfortTemperature;
		public float ecoTemperature;
		public int specialSettingsStartTime;
		public int specialSettingsEndTime;
		public int specialSettingsUsageTemperature;
		public int specialSettingsNonUsageTemperature;
		public int specialSettingsComfortTemperature;
		public int specialSettingsEcoTemperature;
	}
	public static List<RoomTypeGroupData> getRoomTypeData(SubCustomerData subCustomer, ApplicationManager appMan,
			boolean includeEmptyStandardTypes) {
		RoomTypeGroupData result1 = new RoomTypeGroupData();
		List<RoomTypeGroupData> result = new ArrayList<>();
		result.add(result1);
		return result ;
	}
	
	/** Set room heating/cooling data of a room type group
	 * 
	 * @param rooms rooms of the subType
	 * @param newSettings newSettings to be applied
	 * @param isCooling
	 * @param overwriteNonStandard if true then all settings in the rooms given in the list are overwritten even
	 * 		if they are special settings
	 * @param currentSettings Only relevant if overwriteNonStandard=false. Then
	 * 		current settings in each room a compared to the settings provided here.
	 * 		Only if a setting is equal then it is overwritten, otherwise it is considered
	 * 		special to preserve.
	 */
	public static void setRoomTypeData(List<String> rooms, HeatCoolData newSettings, boolean isCooling,
			boolean overwriteNonStandard, HeatCoolData currentSettings) {
		//TODO
	}
}
